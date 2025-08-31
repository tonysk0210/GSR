package com.hn2.cms.repository.aca2003;

import com.hn2.cms.dto.aca2003.Aca2003DetailView;
import com.hn2.cms.dto.aca2003.Aca2003QueryDto;
import com.hn2.cms.model.aca2003.AcaDrugUseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface Aca2003Repository extends JpaRepository<AcaDrugUseEntity, Integer> {

    // save API
    // 依 ACABrd 取分會（新增時用）
    @Query(value = "SELECT TOP 1 CreatedByBranchID FROM dbo.ACABrd WHERE ACACardNo = :cardNo", nativeQuery = true)
    String findCreatedByBranchIdByAcaCardNo(@Param("cardNo") String acaCardNo);

    // 回傳有效筆數（isDeleted = 0 or null）
    @Query("select count(a) from AcaDrugUseEntity a " +
            "where a.acaCardNo = :cardNo and a.proRecId = :proRecId " +
            "and (a.isDeleted = false or a.isDeleted is null)")
    long countActive(@Param("cardNo") String cardNo, @Param("proRecId") String proRecId);

    //query API
    // === 新增：依 ID 查詢詳情（Native SQL + Interface Projection） ===
    // d.CreatedOnDate → 資料表 AcaDrugUse 的實際欄位。
    // AS createdOnDate → SQL 結果集欄位別名，用來對應 projection。
    @Query(value =
            "SELECT d.ID                 AS id, " +
                    "       d.CreatedOnDate      AS createdOnDate, " +
                    "       d.CreatedByBranchID  AS createdByBranchId, " +
                    "       d.DrgUserText        AS drgUserText, " +
                    "       d.OprFamilyText      AS oprFamilyText, " +
                    "       d.OprFamilyCareText  AS oprFamilyCareText, " +
                    "       d.OprSupportText     AS oprSupportText, " +
                    "       d.OprContactText     AS oprContactText, " +
                    "       d.OprReferText       AS oprReferText, " +
                    "       d.Addr               AS addr, " +
                    "       d.OprAddr            AS oprAddr, " +
                    "       d.ACACardNo          AS acaCardNo, " +
                    "       b.ACAName            AS acaName, " +
                    "       b.ACAIDNo            AS acaIdNo " +
                    "FROM dbo.AcaDrugUse d " +
                    "LEFT JOIN dbo.ACABrd b ON b.ACACardNo = d.ACACardNo " + "  AND b.IsDeleted = 0" + //LEFT JOIN（容錯顯示），要保留「右表對不到也要顯示左表列」 → 用 LEFT JOIN，且右表(b.IsDeleted = 0)條件放在 ON
                    "WHERE d.ID = :id " +
                    "  AND (d.IsDeleted = 0 OR d.IsDeleted IS NULL) " // AcaDrugUse：bit/nullable
            ,                              // ACABrd：int
            nativeQuery = true)
    Optional<Aca2003DetailView> findDetailById(@Param("id") Integer id);

}

