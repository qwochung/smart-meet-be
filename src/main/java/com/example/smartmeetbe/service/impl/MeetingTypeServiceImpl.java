package com.example.smartmeetbe.service.impl;

import com.example.smartmeetbe.constant.MeetingType;
import com.example.smartmeetbe.constant.MinutesFormat;
import com.example.smartmeetbe.constant.SummaryField;
import com.example.smartmeetbe.dto.request.CustomMeetingTypeRequest;
import com.example.smartmeetbe.dto.request.MeetingTypePreferenceRequest;
import com.example.smartmeetbe.dto.response.MeetingTypeResponse;
import com.example.smartmeetbe.entity.CustomMeetingType;
import com.example.smartmeetbe.entity.User;
import com.example.smartmeetbe.entity.UserMeetingTypePreference;
import com.example.smartmeetbe.repository.CustomMeetingTypeRepository;
import com.example.smartmeetbe.repository.UserMeetingTypePreferenceRepository;
import com.example.smartmeetbe.service.MeetingTypePromptFactory;
import com.example.smartmeetbe.service.MeetingTypeService;
import com.example.smartmeetbe.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingTypeServiceImpl implements MeetingTypeService {

    private static final String CUSTOM_CODE_PREFIX = "CUSTOM_";
    private static final int MAX_CUSTOM_TYPES_PER_USER = 20;

    private final CustomMeetingTypeRepository customTypeRepository;
    private final UserMeetingTypePreferenceRepository preferenceRepository;
    private final MeetingTypePromptFactory promptFactory;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public List<MeetingTypeResponse> getMeetingTypes(String email) {
        User user = userService.findByEmail(email);
        Map<String, Boolean> enabledByCode = loadPreferences(user.getId());

        List<MeetingTypeResponse> result = new ArrayList<>();
        for (MeetingType type : MeetingType.values()) {
            result.add(MeetingTypeResponse.builder()
                    .code(type.name())
                    .label(type.getLabel())
                    .description(type.getDescription())
                    .builtIn(true)
                    .enabled(enabledByCode.getOrDefault(type.name(), true))
                    .extractFields(List.of())
                    .defaultMinutesFormat(MinutesFormat.defaultFor(type.name()))
                    .build());
        }

        customTypeRepository.findByOwnerUserIdOrderByCreatedAtAsc(user.getId()).stream()
                .map(custom -> toResponse(custom, enabledByCode.getOrDefault(custom.getCode(), true)))
                .forEach(result::add);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingTypeResponse> getEnabledMeetingTypes(String email) {
        return getMeetingTypes(email).stream().filter(MeetingTypeResponse::isEnabled).toList();
    }

    @Override
    @Transactional
    public MeetingTypeResponse createCustomType(String email, CustomMeetingTypeRequest request) {
        User user = userService.findByEmail(email);
        String label = requireLabel(request);

        List<CustomMeetingType> existing = customTypeRepository.findByOwnerUserIdOrderByCreatedAtAsc(user.getId());
        if (existing.size() >= MAX_CUSTOM_TYPES_PER_USER) {
            throw new IllegalArgumentException("Bạn chỉ có thể tạo tối đa "
                    + MAX_CUSTOM_TYPES_PER_USER + " loại cuộc họp tùy chỉnh.");
        }
        if (customTypeRepository.existsByOwnerUserIdAndLabelIgnoreCase(user.getId(), label)) {
            throw new IllegalArgumentException("Bạn đã có một loại cuộc họp tên '" + label + "'.");
        }

        Set<SummaryField> fields = requireFields(request);
        String customInstruction = trimOrNull(request.getCustomInstruction());
        CustomMeetingType entity = CustomMeetingType.builder()
                .code(generateUniqueCode(user.getId(), label))
                .ownerUserId(user.getId())
                .label(label)
                .description(trimOrNull(request.getDescription()))
                .systemPrompt(promptFactory.build(label, request.getDescription(), fields, customInstruction))
                .extractFields(promptFactory.formatFields(fields))
                .customInstruction(customInstruction)
                .defaultMinutesFormat(request.getDefaultMinutesFormat() != null
                        ? request.getDefaultMinutesFormat()
                        : MinutesFormat.ACTION)
                .build();

        CustomMeetingType saved = customTypeRepository.save(entity);
        log.info("User {} created custom meeting type {} ({})", user.getId(), saved.getCode(), label);
        return toResponse(saved, true);
    }

    @Override
    @Transactional
    public MeetingTypeResponse updateCustomType(String email, Long id, CustomMeetingTypeRequest request) {
        User user = userService.findByEmail(email);
        CustomMeetingType entity = loadOwnedType(user.getId(), id);
        String label = requireLabel(request);

        if (!entity.getLabel().equalsIgnoreCase(label)
                && customTypeRepository.existsByOwnerUserIdAndLabelIgnoreCase(user.getId(), label)) {
            throw new IllegalArgumentException("Bạn đã có một loại cuộc họp tên '" + label + "'.");
        }

        Set<SummaryField> fields = requireFields(request);
        String customInstruction = trimOrNull(request.getCustomInstruction());
        entity.setLabel(label);
        entity.setDescription(trimOrNull(request.getDescription()));
        entity.setExtractFields(promptFactory.formatFields(fields));
        entity.setCustomInstruction(customInstruction);
        entity.setSystemPrompt(promptFactory.build(label, request.getDescription(), fields, customInstruction));
        if (request.getDefaultMinutesFormat() != null) {
            entity.setDefaultMinutesFormat(request.getDefaultMinutesFormat());
        }

        CustomMeetingType saved = customTypeRepository.save(entity);
        boolean enabled = preferenceRepository.findByUserIdAndTypeCode(user.getId(), saved.getCode())
                .map(UserMeetingTypePreference::getEnabled)
                .orElse(true);
        return toResponse(saved, enabled);
    }

    @Override
    @Transactional
    public void deleteCustomType(String email, Long id) {
        User user = userService.findByEmail(email);
        CustomMeetingType entity = loadOwnedType(user.getId(), id);

        // Các phòng cũ vẫn giữ mã này trong rooms.type_code; khi sinh biên bản chúng sẽ
        // không tìm thấy loại và rơi về GENERAL, nên không cần dọn dữ liệu phòng.
        preferenceRepository.deleteByTypeCode(entity.getCode());
        customTypeRepository.delete(entity);
        log.info("User {} deleted custom meeting type {}", user.getId(), entity.getCode());
    }

    @Override
    @Transactional
    public List<MeetingTypeResponse> updatePreferences(String email, List<MeetingTypePreferenceRequest> preferences) {
        User user = userService.findByEmail(email);
        if (preferences == null || preferences.isEmpty()) {
            return getMeetingTypes(email);
        }

        Set<String> knownCodes = getMeetingTypes(email).stream()
                .map(MeetingTypeResponse::getCode)
                .collect(Collectors.toSet());

        for (MeetingTypePreferenceRequest preference : preferences) {
            String code = preference.getTypeCode();
            if (!knownCodes.contains(code)) {
                log.debug("Ignoring preference for unknown meeting type {} of user {}", code, user.getId());
                continue;
            }
            UserMeetingTypePreference row = preferenceRepository.findByUserIdAndTypeCode(user.getId(), code)
                    .orElseGet(() -> UserMeetingTypePreference.builder()
                            .userId(user.getId())
                            .typeCode(code)
                            .build());
            row.setEnabled(Boolean.TRUE.equals(preference.getEnabled()));
            preferenceRepository.save(row);
        }

        return getMeetingTypes(email);
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveTypeCodeForUser(String email, String requestedCode) {
        if (requestedCode == null || requestedCode.isBlank()) {
            return MeetingType.GENERAL.name();
        }
        if (MeetingType.isBuiltIn(requestedCode)) {
            return requestedCode;
        }

        User user = userService.findByEmail(email);
        return customTypeRepository.findByCode(requestedCode)
                .filter(custom -> custom.getOwnerUserId().equals(user.getId()))
                .map(CustomMeetingType::getCode)
                .orElseGet(() -> {
                    log.warn("User {} requested unavailable meeting type {}, falling back to GENERAL",
                            user.getId(), requestedCode);
                    return MeetingType.GENERAL.name();
                });
    }

    @Override
    @Transactional(readOnly = true)
    public MinutesFormat resolveDefaultMinutesFormat(String typeCode) {
        if (typeCode != null && !MeetingType.isBuiltIn(typeCode)) {
            Optional<MinutesFormat> custom = customTypeRepository.findByCode(typeCode)
                    .map(CustomMeetingType::getDefaultMinutesFormat);
            if (custom.isPresent() && custom.get() != null) {
                return custom.get();
            }
        }
        return MinutesFormat.defaultFor(typeCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomMeetingType> findCustomTypeByCode(String code) {
        return (code == null || MeetingType.isBuiltIn(code))
                ? Optional.empty()
                : customTypeRepository.findByCode(code);
    }

    private Map<String, Boolean> loadPreferences(Long userId) {
        return preferenceRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(
                        UserMeetingTypePreference::getTypeCode,
                        UserMeetingTypePreference::getEnabled,
                        (first, second) -> second));
    }

    private CustomMeetingType loadOwnedType(Long userId, Long id) {
        CustomMeetingType entity = customTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại cuộc họp với id: " + id));
        if (!entity.getOwnerUserId().equals(userId)) {
            throw new IllegalArgumentException("Bạn không có quyền thao tác với loại cuộc họp này.");
        }
        return entity;
    }

    private String requireLabel(CustomMeetingTypeRequest request) {
        String label = trimOrNull(request.getLabel());
        if (label == null) {
            throw new IllegalArgumentException("Tên loại cuộc họp không được để trống.");
        }
        return label;
    }

    /**
     * Bắt buộc có ít nhất một mục trích xuất, nếu không biên bản sinh ra sẽ chỉ còn
     * phần tóm tắt tổng quan — gần như rỗng và thường là do người dùng tích nhầm.
     */
    private Set<SummaryField> requireFields(CustomMeetingTypeRequest request) {
        Set<SummaryField> fields = parseRequestedFields(request.getExtractFields());
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một nội dung AI cần trích xuất.");
        }
        return fields;
    }

    private Set<SummaryField> parseRequestedFields(List<String> requested) {
        if (requested == null || requested.isEmpty()) {
            return Set.of();
        }
        Set<String> allowed = Arrays.stream(SummaryField.values()).map(Enum::name).collect(Collectors.toSet());
        return requested.stream()
                .filter(java.util.Objects::nonNull)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .filter(allowed::contains)
                .map(SummaryField::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Mã phải duy nhất toàn hệ thống và vừa cột {@code rooms.type_code} (50 ký tự),
     * nên ghép tiền tố + id người dùng + slug tên + hậu tố ngẫu nhiên.
     */
    private String generateUniqueCode(Long userId, String label) {
        String slug = slugify(label);
        for (int attempt = 0; attempt < 5; attempt++) {
            String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
            String code = CUSTOM_CODE_PREFIX + userId + "_" + slug + "_" + suffix;
            if (code.length() > 50) {
                code = code.substring(0, 50);
            }
            if (!customTypeRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Không sinh được mã loại cuộc họp duy nhất, vui lòng thử lại.");
    }

    /** Bỏ dấu tiếng Việt và ký tự đặc biệt để mã chỉ còn [A-Z0-9_]. */
    private String slugify(String label) {
        String normalized = Normalizer.normalize(label, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.isEmpty()) {
            normalized = "TYPE";
        }
        return normalized.length() > 20 ? normalized.substring(0, 20) : normalized;
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private MeetingTypeResponse toResponse(CustomMeetingType entity, boolean enabled) {
        return MeetingTypeResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .label(entity.getLabel())
                .description(entity.getDescription())
                .builtIn(false)
                .enabled(enabled)
                .extractFields(promptFactory.parseFields(entity.getExtractFields()).stream()
                        .map(Enum::name)
                        .collect(Collectors.toList()))
                .customInstruction(entity.getCustomInstruction())
                .defaultMinutesFormat(entity.getDefaultMinutesFormat() != null
                        ? entity.getDefaultMinutesFormat()
                        : MinutesFormat.ACTION)
                .build();
    }
}
