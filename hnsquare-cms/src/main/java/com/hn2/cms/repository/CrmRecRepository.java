package com.hn2.cms.repository;

import com.hn2.cms.model.CrmRecEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CrmRecRepository extends JpaRepository<CrmRecEntity, String> {
    Optional<CrmRecEntity> findByAcaCardNo(String acaIdNo);

    @Query(value = "select RIGHT(REPLICATE('0', 5) + CAST( isnull(SUBSTRING(max(id),8,5),0)        + 1 AS VARCHAR(5)), 5) as id     from CrmRec where id like :findKey", nativeQuery = true)
    String genNewId(String findKey);

}
