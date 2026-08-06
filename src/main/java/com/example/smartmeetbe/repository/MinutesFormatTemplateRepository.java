package com.example.smartmeetbe.repository;

import com.example.smartmeetbe.constant.MinutesFormat;
import com.example.smartmeetbe.entity.MinutesFormatTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MinutesFormatTemplateRepository extends JpaRepository<MinutesFormatTemplate, Long> {
    Optional<MinutesFormatTemplate> findByFormatCodeAndIsActiveTrue(MinutesFormat formatCode);
    Optional<MinutesFormatTemplate> findByFormatCode(MinutesFormat formatCode);
}
