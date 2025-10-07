package com.hn2.cms.repository.aca4001;

import com.hn2.cms.dto.aca4001.Aca4001AuditQueryDto;
import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto;
import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto.CrmRec;
import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto.PersonBirth;
import com.hn2.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hn2.util.DateUtil.DateFormat.yyyMMdd_slash;

@Repository
@RequiredArgsConstructor
public class Aca4001RepositoryImpl implements Aca4001Repository {

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate npJdbc;
    private final org.sql2o.Sql2o sql2o;

    @Override
    public PersonBirth findPersonBirth(String acaCardNo) {
        String sql = "SELECT TOP 1 " +
                "  CAST(ACABirth AS date) AS BirthDate, " + // -- 生日(只留日期)
                "  CASE WHEN ACABirth IS NOT NULL " +
                "       THEN DATEADD(YEAR, 18, CAST(ACABirth AS date)) " + // -- 18歲當天 00:00
                "       ELSE NULL END AS EighteenthStart " +
                "FROM dbo.ACABrd " +
                "WHERE ACACardNo = ? AND IsDeleted = 0";
        // 用 query(...) + extractor：無資料時回傳 null
        return jdbc.query(sql, ps -> ps.setString(1, acaCardNo), (ResultSet rs) -> {
            if (!rs.next()) return null;
            var birthSql = rs.getDate("BirthDate");
            var e18Sql = rs.getDate("EighteenthStart");

            PersonBirth pb = new PersonBirth();
            pb.setBirthDate(birthSql == null ? null : birthSql.toLocalDate());
            pb.setEighteenthStart(e18Sql == null ? null : e18Sql.toLocalDate());
            return pb;
        });
    }

    @Override
    public List<String> findProRecIdsBefore18(String acaCardNo, LocalDateTime eighteenthStart, LocalDateTime startTs, LocalDateTime endExclusive) {
        String sql = "SELECT PR.ID " +
                "FROM dbo.ProRec PR " +
                "WHERE PR.IsDeleted = 0 " +
                "AND PR.ACACardNo = ? " +
                "AND PR.ProNoticeDate  < ? " +
                "AND (? IS NULL OR PR.ProNoticeDate  >= ?) " +
                "AND (? IS NULL OR PR.ProNoticeDate  <= ?) " +
                "ORDER BY PR.ProNoticeDate ";
        return jdbc.query(sql, ps -> {
            ps.setString(1, acaCardNo);
            ps.setObject(2, eighteenthStart);
            ps.setObject(3, startTs);
            ps.setObject(4, startTs);
            ps.setObject(5, endExclusive);
            ps.setObject(6, endExclusive);
        }, (rs, i) -> rs.getString("ID"));
    }

    @Override
    public List<String> findCrmRecIdsBefore18(String acaCardNo, LocalDateTime eighteenthStart, LocalDateTime startTs, LocalDateTime endExclusive) {
        String sql = "SELECT CR.ID " +
                "FROM dbo.CrmRec CR " +
                "WHERE CR.IsDeleted = 0 " +
                "AND CR.ACACardNo = ? " +
                "AND CR.CreatedOnDate < ? " +
                "AND (? IS NULL OR CR.CreatedOnDate  >= ?) " +
                "AND (? IS NULL OR CR.CreatedOnDate  <= ?) " +
                "ORDER BY CR.CreatedOnDate ";
        return jdbc.query(sql, ps -> {
            ps.setString(1, acaCardNo);
            ps.setObject(2, eighteenthStart);
            ps.setObject(3, startTs);
            ps.setObject(4, startTs);
            ps.setObject(5, endExclusive);
            ps.setObject(6, endExclusive);
        }, (rs, i) -> rs.getString("ID"));
    }

    @Override
    public List<CrmRec> findCrmRecsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        // 動態組 IN (?, ?, ...)
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        String sql =
                "SELECT " +
                        "  CR.ID, " +
                        "  CAST(CR.CreatedOnDate AS date)   AS RecordDate, " +     // 紀錄日期
                        "  L_BR.Text                        AS BranchName, " +      // 分會別（ParentID=26）
                        "  L_J.Text                         AS JailAgency, " +      // 執行機關
                        "  L_C1.Text                        AS CrimeName1, " +      // 罪名1
                        "  L_C2.Text                        AS CrimeName2, " +      // 罪名2
                        "  L_C3.Text                        AS CrimeName3, " +      // 罪名3
                        "  L_NJ.Text                        AS NoJailReason, " +    // 未入獄原因
                        "  CAST(CR.Crm_VerdictDate AS date) AS VerdictDate, " +     // 執行日期
                        "  CR.Crm_Sentence                  AS SentenceType, " +    // 刑期種類
                        "  CR.CrmTerm                       AS TermText, " +        // 刑期(文字)
                        "  CAST(CR.CrmChaDate AS date)      AS PrisonInDate, " +    // 入獄時間
                        "  CAST(CR.Crm_ReleaseDate AS date) AS ReleasePlanDate, " + // 預定獲釋日
                        "  CAST(CR.CrmDisDate AS date)      AS PrisonOutDate, " +   // 出獄日期
                        "  L_DIS.Text                       AS PrisonOutReason, " + // 出獄原因
                        "  L_REM.Text                       AS Remission, " +       // 減刑案
                        "  CR.CrmTrain                      AS TrainType, " +       // 受訓種類
                        "  CR.CrmMemo                       AS Memo " +             // 備註
                        "FROM dbo.CrmRec CR " +
                        "LEFT JOIN dbo.Lists L_BR  " +
                        "  ON L_BR.ParentID = 26 " +
                        " AND L_BR.Value = CAST(CR.CreatedByBranchID AS NVARCHAR(50)) " +
                        "LEFT JOIN dbo.Lists L_J   " +
                        "  ON L_J.ListName = 'ACA_JAIL_TYPE' AND L_J.Value = CR.ProNoticeDep " +
                        "LEFT JOIN dbo.Lists L_C1  " +
                        "  ON L_C1.ListName = 'ACA_CRIME' AND L_C1.Value = CR.CrmCrime1 " +
                        "LEFT JOIN dbo.Lists L_C2  " +
                        "  ON L_C2.ListName = 'ACA_CRIME' AND L_C2.Value = CR.CrmCrime2 " +
                        "LEFT JOIN dbo.Lists L_C3  " +
                        "  ON L_C3.ListName = 'ACA_CRIME' AND L_C3.Value = CR.CrmCrime3 " +
                        "LEFT JOIN dbo.Lists L_NJ  " +
                        "  ON L_NJ.ListName = 'ACA_NOJAIL' AND L_NJ.Value = CR.Crm_NoJail " +
                        "LEFT JOIN dbo.Lists L_DIS " +
                        "  ON L_DIS.ListName = 'ACA_DISCHARGE' AND L_DIS.Value = CR.CrmDischarge " +
                        "LEFT JOIN dbo.Lists L_REM " +
                        "  ON L_REM.ListName = 'ACA_REMISSION' AND L_REM.Value = CR.CrmRemission " +
                        "WHERE CR.IsDeleted = 0 " +
                        "  AND CR.ID IN (" + placeholders + ")";
        // 參數
        Object[] params = ids.toArray();

        List<CrmRec> rows = jdbc.query(sql, params, (rs, i) -> {
            var c = new CrmRec();
            c.setId(rs.getString("ID"));

            var d1 = rs.getDate("RecordDate");
            c.setRecordDate(d1 == null ? null :
                    DateUtil.date2Roc(DateUtil.date2LocalDate(d1), yyyMMdd_slash));


            c.setBranchName(rs.getString("BranchName"));
            c.setJailAgency(rs.getString("JailAgency"));
            c.setCrimeName1(rs.getString("CrimeName1"));
            c.setCrimeName2(rs.getString("CrimeName2"));
            c.setCrimeName3(rs.getString("CrimeName3"));
            c.setNoJailReason(rs.getString("NoJailReason"));

            var d2 = rs.getDate("VerdictDate");
            c.setVerdictDate(d2 == null ? null :
                    DateUtil.date2Roc(DateUtil.date2LocalDate(d2), yyyMMdd_slash));

            c.setSentenceType(rs.getString("SentenceType"));
            c.setTermText(rs.getString("TermText"));

            var d3 = rs.getDate("PrisonInDate");
            c.setPrisonInDate(d3 == null ? null :
                    DateUtil.date2Roc(DateUtil.date2LocalDate(d3), yyyMMdd_slash));

            var d4 = rs.getDate("ReleasePlanDate");
            c.setReleasePlanDate(d4 == null ? null :
                    DateUtil.date2Roc(DateUtil.date2LocalDate(d4), yyyMMdd_slash));

            var d5 = rs.getDate("PrisonOutDate");
            c.setPrisonOutDate(d5 == null ? null :
                    DateUtil.date2Roc(DateUtil.date2LocalDate(d5), yyyMMdd_slash));

            c.setPrisonOutReason(rs.getString("PrisonOutReason"));
            c.setRemission(rs.getString("Remission"));
            c.setTrainType(rs.getString("TrainType"));
            c.setMemo(rs.getString("Memo"));
            return c;
        });

        // 依照原本 ID 清單排序（避免 IN(...) 打亂順序）
        Map<String, Integer> order = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) order.put(ids.get(i), i);
        rows.sort(Comparator.comparingInt(r -> order.getOrDefault(r.getId(), Integer.MAX_VALUE)));

        return rows;
    }

    @Override
    public List<Aca4001EraseQueryDto.ProRec> findProRecsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        // 用命名參數 IN (:ids)；SQL 第一字元加分號，避免 driver/代理在上一個語句沒分號時影響這一個
        String sql =
                ";SELECT " +
                        "    PR.ID, " +
                        "    L_BR.[Text]                           AS BranchName, " +
                        "    L_SRC.[Text]                          AS SourceText, " +
                        "    CASE PR.ProHealth " +
                        "         WHEN 'A001' THEN N'良好' " +
                        "         WHEN 'A002' THEN N'普通' " +
                        "         WHEN 'A003' THEN N'舊制身心障礙(16類)' " +
                        "         WHEN 'A004' THEN N'欠佳' " +
                        "         WHEN 'A005' THEN N'具精神異常傾向' " +
                        "         WHEN 'A006' THEN N'新制身心障礙(8類)' " +
                        "         ELSE NULL END                    AS ProHealthText, " +

                        // 各 level 取該 ProRec 最新一筆
                        "    OA1.L1Text                            AS ProtectLevel1, " +
                        "    OA2.L2Text                            AS ProtectLevel2, " +
                        "    OA3.L3Text                            AS ProtectLevel3, " +

                        "    CAST(PR.ProNoticeDate AS date)        AS ProNoticeDate, " +
                        "    CAST(PR.ProDate       AS date)        AS ProDate, " +
                        "    PR.IsAdopt                              AS Adopt, " +
                        "    CASE WHEN EXISTS ( " +
                        "        SELECT 1 FROM dbo.ProjectRec P " +
                        "        WHERE P.LinkTableID = PR.ID AND P.LinkTableType = 'P' " +
                        "          AND P.ProjectID = 'A20130400094' AND P.IsDeleted = 0 " +
                        "    ) THEN N'家支' ELSE N'' END           AS HomeSupportTag, " +
                        "    L_DRUG.[Text]                         AS DrugProjectText, " +
                        "    CASE WHEN PR.ProCloseDate IS NULL THEN 0 ELSE 1 END AS Closed, " +
                        "    U.DisplayName                         AS StaffDisplayName, " +  //-- 建檔者顯示名稱（DNN Users）
                        // -- CounselorInstDisplay：區域 +（必要時空白）+ 機構名稱 +（實習/正式尾註）
                        "    COALESCE( " +
                        "      NULLIF( " +
                        "        CONCAT( " +
                        "          ISNULL(LA.[Text], N''), " + //-- 區域文字（Lists: ACA_INSTAREA）
                        "          CASE WHEN NULLIF(LA.[Text], N'') IS NOT NULL AND NULLIF(IB.InstName, N'') IS NOT NULL THEN N' ' ELSE N'' END, " + //-- 區域與機構名皆非空時才加空白
                        "          ISNULL(IB.InstName, N''), " + //-- 機構名稱（InstBrd.InstName）
                        "          CASE " +
                        "            WHEN OM.WorkerID IS NULL " +
                        "                 OR (NULLIF(LA.[Text], N'') IS NULL AND NULLIF(IB.InstName, N'') IS NULL) THEN N'' " + //-- 沒有輔導員或兩者皆空：不加尾註（避免只顯示「(正式)」）
                        "            WHEN COALESCE(IB.IsUnofficial, 0) = 1 THEN N'(實習)' " +
                        "            ELSE N'(正式)' " +
                        "          END " +
                        "        ), N'' " + // -- CONCAT 結果是空字串時 => 轉為 NULL
                        "      ), N'' " + // -- 最終把 NULL 轉回空字串
                        "    ) AS CounselorInstDisplay, " +
                        "    OM.WorkerID AS CounselorWorkerId, " + //-- 由 OUTER APPLY 取得的輔導員卡號
                        "    PR.ProFile AS ArchiveName " +                          // ← ★ 新增：歸檔名稱
                        "FROM dbo.ProRec PR " +
                        "LEFT JOIN dbo.Lists L_BR  " +
                        "       ON L_BR.ParentID = 26 " +
                        "      AND L_BR.Value = CAST(PR.CreatedByBranchID AS NVARCHAR(50)) " +
                        "LEFT JOIN dbo.Lists L_SRC " +
                        "       ON L_SRC.ListName = 'ACA_SOURCE' " +
                        "      AND L_SRC.Value    = PR.ProSource " +
                        "LEFT JOIN dbo.Lists L_DRUG " +
                        "       ON L_DRUG.ListName = 'PROJ_DRUG' " +
                        "      AND L_DRUG.Value    = PR.DrugForm " +
                        "LEFT JOIN [CaseManagementDnnDB].dbo.Users U " +
                        "       ON U.UserID = PR.CreatedByUserID " +

                        // L1：ProItem 最新一筆
                        "OUTER APPLY ( " +
                        "    SELECT TOP (1) L1.[Text] AS L1Text " +
                        "    FROM dbo.ProDtl PD " +
                        "    LEFT JOIN dbo.Lists L1 ON L1.ListName = 'ACA_PROTECT' AND L1.Value = PD.ProItem " +
                        "    WHERE PD.IsDeleted = 0 AND PD.ProRecID = PR.ID AND PD.ProItem IS NOT NULL " +
                        "    ORDER BY PD.ID DESC " +       //最新一筆
                        ") OA1 " +

                        // L2：Interview 最新一筆
                        "OUTER APPLY ( " +
                        "    SELECT TOP (1) L2.[Text] AS L2Text " +
                        "    FROM dbo.ProDtl PD " +
                        "    LEFT JOIN dbo.Lists L2 ON L2.ListName = 'ACA_PROTECT' AND L2.Value = PD.Interview " +
                        "    WHERE PD.IsDeleted = 0 AND PD.ProRecID = PR.ID AND PD.Interview IS NOT NULL " +
                        "    ORDER BY PD.ID DESC " +       //最新一筆
                        ") OA2 " +

                        // L3：ProPlace 最新一筆
                        "OUTER APPLY ( " +
                        "    SELECT TOP (1) L3.[Text] AS L3Text " +
                        "    FROM dbo.ProDtl PD " +
                        "    LEFT JOIN dbo.Lists L3 ON L3.ListName = 'ACA_PROTECT' AND L3.Value = PD.ProPlace " +
                        "    WHERE PD.IsDeleted = 0 AND PD.ProRecID = PR.ID AND PD.ProPlace IS NOT NULL " +
                        "    ORDER BY PD.ID DESC " +       //最新一筆
                        ") OA3 " +

                        // 輔導員 OneMember：對每筆 PR 逐列找「第一位」非 EP 的成員
                        // -- OUTER APPLY ≈ LEFT JOIN LATERAL：就算找不到也保留左表列（欄位為 NULL）
                        "OUTER APPLY ( " +
                        "    SELECT TOP (1) PRM.WorkerID " +
                        "    FROM dbo.ProRecMember PRM " +
                        "    WHERE PRM.ProRecID = PR.ID " +
                        "      AND PRM.MemberType <> 'EP' " +
                        "      AND PRM.IsDeleted = 0 " +
                        "    ORDER BY PRM.ID " +
                        ") OM " +
                        "LEFT JOIN dbo.InstBrd IB " +
                        "       ON IB.InstCardNo = OM.WorkerID " +
                        "      AND IB.IsDeleted = 0 " +
                        "LEFT JOIN dbo.Lists LA " +
                        "       ON LA.ListName = 'ACA_INSTAREA' " +
                        "      AND LA.Value    = IB.InstArea " +
                        "WHERE PR.IsDeleted = 0 " +
                        "  AND PR.ID IN (:ids)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ids", ids); // 直接丟 List<String> / List<Integer> 都可

        List<Aca4001EraseQueryDto.ProRec> rows = npJdbc.query(sql, params, (rs, i) -> {
            var p = new Aca4001EraseQueryDto.ProRec();
            p.setId(rs.getString("ID"));
            p.setBranchName(rs.getString("BranchName"));
            p.setSourceText(rs.getString("SourceText"));
            p.setProHealthText(rs.getString("ProHealthText"));

            p.setProtectLevel1(rs.getString("ProtectLevel1"));
            p.setProtectLevel2(rs.getString("ProtectLevel2"));
            p.setProtectLevel3(rs.getString("ProtectLevel3"));

            var d1 = rs.getDate("ProNoticeDate");
            p.setProNoticeDate(d1 == null ? null
                    : DateUtil.date2Roc(DateUtil.date2LocalDate(d1), yyyMMdd_slash));

            var d2 = rs.getDate("ProDate");
            p.setProDate(d2 == null ? null
                    : DateUtil.date2Roc(DateUtil.date2LocalDate(d2), yyyMMdd_slash));

            // 包 Boolean 允許 null
            Object adoptObj = rs.getObject("Adopt");
            p.setAdopt(adoptObj == null ? null : (Boolean) adoptObj);

            p.setHomeSupportTag(rs.getString("HomeSupportTag"));
            p.setDrugProjectText(rs.getString("DrugProjectText"));

            Object closedObj = rs.getObject("Closed");
            p.setClosed(closedObj == null ? null : ((Integer) closedObj) == 1);

            p.setStaffDisplayName(rs.getString("StaffDisplayName"));
            p.setCounselorInstDisplay(rs.getString("CounselorInstDisplay"));
            p.setCounselorWorkerId(rs.getString("CounselorWorkerId"));
            p.setArchiveName(rs.getString("ArchiveName"));
            return p;
        });

        // 照呼叫方給的 ID 還原順序（避免 IN 造成亂序）
        Map<String, Integer> order = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) order.put(ids.get(i), i);
        rows.sort(Comparator.comparingInt(r -> order.getOrDefault(r.getId(), Integer.MAX_VALUE)));

        return rows;
    }


    @Override
    public Boolean findLatestProRecClosed(String acaCardNo) {
        String sql = "SELECT TOP 1 " +
                "CASE WHEN ProCloseDate IS NULL THEN 0 ELSE 1 END AS Closed " +
                "FROM dbo.ProRec " +
                "WHERE IsDeleted = 0 AND ACACardNo = ? " +
                "ORDER BY ProDate DESC"; // 依照 ProDate 取最新的

        return jdbc.query(sql, ps -> ps.setString(1, acaCardNo), rs -> {
            if (!rs.next()) return null; // 查無紀錄
            return rs.getInt("Closed") == 1;
        });
    }

    @Override
    public Boolean findPersonErased(String acaCardNo) {
        String sql = "SELECT TOP 1 " +
                "CASE WHEN IsErase = 1 THEN 1 ELSE 0 END AS Erased " +
                "FROM dbo.ACABrd " +
                "WHERE ACACardNo = ? AND IsDeleted = 0";

        return jdbc.query(sql, ps -> ps.setString(1, acaCardNo), rs -> {
            if (!rs.next()) return null; // 查無資料
            return rs.getInt("Erased") == 1;
        });
    }

    //for less than 18
    @Override
    public List<String> findAllCrmRecIdsByAcaCardNo(String acaCardNo) {
        String sql = "SELECT ID FROM dbo.CrmRec WHERE ACACardNo = :aca";
        try (var con = sql2o.open()) {
            return con.createQuery(sql).addParameter("aca", acaCardNo).executeAndFetch(String.class);
        }
    }

    @Override
    public List<String> findAllProRecIdsByAcaCardNo(String acaCardNo) {
        String sql = "SELECT ID FROM dbo.ProRec WHERE ACACardNo = :aca";
        try (var con = sql2o.open()) {
            return con.createQuery(sql).addParameter("aca", acaCardNo).executeAndFetch(String.class);
        }
    }

    //audit
    @Override
    public List<Aca4001AuditQueryDto.Group> findAuditGroups(String acaCardNo,
                                                            LocalDateTime start,
                                                            LocalDateTime end) {
        StringBuilder sb = new StringBuilder();
        sb.append("WITH Base AS (\n")
                .append("  SELECT ACACardNo, ActionType, CreatedByUserID, UserIP,\n")
                .append("         CAST(CreatedOnDate AS DATETIME2(0)) AS CreatedOnSec,\n")
                .append("         DocNum, EraseReason, RestoreReason\n")
                .append("  FROM dbo.ACA_EraseAudit\n")
                .append("  WHERE 1=1\n");
        if (acaCardNo != null && !acaCardNo.isBlank()) sb.append("    AND ACACardNo = :acaCardNo\n");
        if (start != null) sb.append("    AND CreatedOnDate >= :start\n");
        if (end != null) sb.append("    AND CreatedOnDate <= :end\n");
        sb.append("), G AS (\n")
                .append("  SELECT ACACardNo, ActionType, CreatedByUserID, UserIP, CreatedOnSec,\n")
                .append("         CAST(COUNT(*) AS int) AS RecordCount,\n") // 明確 int
                .append("         MAX(DocNum) AS DocNum,\n")
                .append("         MAX(EraseReason)   AS EraseReason,\n")
                .append("         MAX(RestoreReason) AS RestoreReason\n")
                .append("  FROM Base\n")
                .append("  GROUP BY ACACardNo, ActionType, CreatedByUserID, UserIP, CreatedOnSec\n")
                .append(")\n")
                .append("SELECT\n")
                .append("  g.ACACardNo  AS acaCardNo,\n")
                .append("  g.ActionType AS action,\n")
                .append("  CASE WHEN UPPER(g.ActionType)='ERASE' THEN CAST(1 AS bit) ELSE CAST(0 AS bit) END AS isErased,\n")
                .append("  CAST(g.DocNum AS int) AS docNum,\n") // 明確 int
                .append("  COALESCE(NULLIF(g.EraseReason,''), NULLIF(g.RestoreReason,'')) AS reason,\n")
                .append("  g.RecordCount AS recordCount,\n")
                .append("  CAST(g.CreatedOnSec AS DATETIME2(0)) AS createdOn,\n") // 可對映到 java.sql.Timestamp
                .append("  CAST(g.CreatedByUserID AS nvarchar(50)) AS userId,\n") // 用字串接
                .append("  u.DisplayName AS userName,\n")
                .append("  CAST(g.UserIP AS nvarchar(64)) AS userIp\n")
                .append("FROM G g\n")
                .append("LEFT JOIN CaseManagementDnnDB.dbo.Users u\n")
                .append("  ON u.UserID = TRY_CONVERT(int, g.CreatedByUserID)\n")
                .append("ORDER BY g.CreatedOnSec DESC\n");

        try (var con = sql2o.open()) {
            var q = con.createQuery(sb.toString());
            if (acaCardNo != null && !acaCardNo.isBlank()) q.addParameter("acaCardNo", acaCardNo);
            if (start != null) q.addParameter("start", start);
            if (end != null) q.addParameter("end", end);
            return q.executeAndFetch(Aca4001AuditQueryDto.Group.class);
        }
    }

}
