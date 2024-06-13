package com.hn2.cms.repository;

import com.hn2.cms.model.AcaBrdEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcaBrdRepository extends JpaRepository<AcaBrdEntity, String> {
    Optional<AcaBrdEntity> findByCreatedByBranchIdAndAcaIdNo(String createdByBranchId, String acaIdNo);
}
