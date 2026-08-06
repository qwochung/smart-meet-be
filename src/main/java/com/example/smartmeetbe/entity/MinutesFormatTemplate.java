package com.example.smartmeetbe.entity;

import com.example.smartmeetbe.constant.MinutesFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lớp prompt thứ hai: quyết định MỨC ĐỘ CHI TIẾT của biên bản.
 * Tách riêng khỏi {@link PromptTemplate} (quyết định chủ đề cần trích xuất) để không phải
 * seed/bảo trì tổ hợp 6 loại cuộc họp x 3 mẫu biên bản.
 */
@Entity
@Table(name = "minutes_format_template")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinutesFormatTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "format_code", unique = true, nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private MinutesFormat formatCode;

    /** Đoạn chỉ dẫn được nối vào sau system prompt của loại cuộc họp. */
    @Column(name = "detail_prompt", columnDefinition = "TEXT", nullable = false)
    private String detailPrompt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
