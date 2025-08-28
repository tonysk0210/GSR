package com.hn2.cms.repository.aca2003;

import com.hn2.cms.model.aca2003.AcaDrugUseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface Aca2003Repository extends JpaRepository<AcaDrugUseEntity, Integer> {

    // 依 ACABrd 取分會（新增時用）
    @Query(value = "SELECT TOP 1 CreatedByBranchID FROM dbo.ACABrd WHERE ACACardNo = :cardNo", nativeQuery = true)
    String findCreatedByBranchIdByAcaCardNo(@Param("cardNo") String acaCardNo);

    // 回傳有效筆數（isDeleted = 0 or null）
    @Query("select count(a) from AcaDrugUseEntity a " +
            "where a.acaCardNo = :cardNo and a.proRecId = :proRecId " +
            "and (a.isDeleted = false or a.isDeleted is null)")
    long countActive(@Param("cardNo") String cardNo, @Param("proRecId") String proRecId);

}

