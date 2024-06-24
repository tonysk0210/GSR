package com.hn2.cms.repository;

import com.hn2.cms.model.CrmRecEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrmRecRepository extends JpaRepository<CrmRecEntity, String> {
    Optional<CrmRecEntity> findByAcaCardNo(String acaIdNo);
}
