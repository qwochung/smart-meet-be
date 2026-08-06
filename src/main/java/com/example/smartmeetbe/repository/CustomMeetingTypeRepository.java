package com.example.smartmeetbe.repository;

import com.example.smartmeetbe.entity.CustomMeetingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomMeetingTypeRepository extends JpaRepository<CustomMeetingType, Long> {
    List<CustomMeetingType> findByOwnerUserIdOrderByCreatedAtAsc(Long ownerUserId);
    Optional<CustomMeetingType> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByOwnerUserIdAndLabelIgnoreCase(Long ownerUserId, String label);
}
