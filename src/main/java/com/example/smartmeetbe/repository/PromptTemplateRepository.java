package com.example.smartmeetbe.repository;

import com.example.smartmeetbe.constant.MeetingType;
import com.example.smartmeetbe.entity.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {
    Optional<PromptTemplate> findByTypeCodeAndIsActiveTrue(MeetingType typeCode);
    Optional<PromptTemplate> findByTypeCode(MeetingType typeCode);
}
