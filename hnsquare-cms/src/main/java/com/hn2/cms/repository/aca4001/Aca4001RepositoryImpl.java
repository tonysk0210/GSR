package com.hn2.cms.repository.aca4001;

import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto;
import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto.CrmRec;
import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto.PersonBirth;
import com.hn2.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
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
}
