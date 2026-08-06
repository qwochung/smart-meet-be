package com.example.smartmeetbe.repository;

import com.example.smartmeetbe.entity.UserMeetingTypePreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserMeetingTypePreferenceRepository extends JpaRepository<UserMeetingTypePreference, Long> {
    List<UserMeetingTypePreference> findByUserId(Long userId);
    Optional<UserMeetingTypePreference> findByUserIdAndTypeCode(Long userId, String typeCode);
    void deleteByTypeCode(String typeCode);
}
