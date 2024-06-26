package com.hn2.cms.repository;

import com.hn2.cms.model.AcaBrdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AcaBrdRepository extends JpaRepository<AcaBrdEntity, String> {
    Optional<AcaBrdEntity> findByAcaIdNo(String acaIdNo);

    @Query(value = "select RIGHT(REPLICATE('0', 5) + CAST( isnull(SUBSTRING(max(id),8,5),0)        + 1 AS VARCHAR(5)), 5) as id     from ACABrd where id like :findKey", nativeQuery = true)
    String genNewId(String findKey);
    @Query(value = "select RIGHT(REPLICATE('0', 4 + CAST( isnull(SUBSTRING(max(ACACardNo),4,4),0) + 1 AS VARCHAR(4)), 4)  as aca_id from ACABrd where ACACardNo like :findKey", nativeQuery = true)
    String genNewAcaCardNo(String findKey);

}
