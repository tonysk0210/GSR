package com.hn2.cms.repository.aca3001;

import com.hn2.cms.dto.aca3001.Aca3001QueryDto;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class Aca3001RepositoryImpl implements Aca3001Repository {

    // jdbcTemplate 幫你處理了連線、SQL 執行、ResultSet 轉換等細節，你只要專注寫 SQL + RowMapper。
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public Aca3001RepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---------- 介面實作 ----------
    @Override
    public Integer findProAdoptIdByProRecId(String proRecId) {
        final String SQL_FIND_PROADOPT_ID = "SELECT ID FROM ProAdopt WHERE ProRecID = ?";
        List<Integer> ids = jdbcTemplate.query(SQL_FIND_PROADOPT_ID, (rs, i) -> rs.getInt("ID"), proRecId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    public Aca3001QueryDto.Profile findProfileByProRecId(String proRecId) {

        // 這是 SQL 查詢語句，從 ProRec 和 ACABrd 表中查找資料
        // 把 ProRec 表的案件，跟 ACABrd 表的受保護人基本資料，用 ACACardNo（卡號）對應起來
        final String SQL_FIND_PROFILE = "SELECT b.ACAName, b.ACAIDNo, b.ACACardNo " +
                "FROM ProRec r " +
                "JOIN ACABrd b ON b.ACACardNo = r.ACACardNo " +
                "WHERE r.ID = ?";

        // 這是 RowMapper，把每一筆 ResultSet 轉換成 Aca3001QueryDto.Profile 物件
        // 注意：這裡的 rs 是 ResultSet，i 是行號（從 0 開始）
        // rs.getString("ACAName") 會從 ResultSet 中取出名為 "ACAName" 的欄位值
        // 這裡使用了 lambda 表達式來簡化 RowMapper 的實現
        // 這樣可以讓程式碼更簡潔易讀
        // 最後，jdbcTemplate.query()
        // 會執行 SQL 查詢，並把結果轉換成 List<Aca3001QueryDto.Profile>。
        List<Aca3001QueryDto.Profile> list = jdbcTemplate.query(SQL_FIND_PROFILE, (rs, i) -> {
            Aca3001QueryDto.Profile profile = new Aca3001QueryDto.Profile();
            profile.setAcaName(rs.getString("ACAName"));
            profile.setAcaIdNo(rs.getString("ACAIDNo"));
            profile.setAcaCardNo(rs.getString("ACACardNo"));
            return profile;
        }, proRecId);

        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public Aca3001QueryDto.Header findHeaderByProRecId(String proRecId) {
        final String SQL_FIND_HEADER =
                "SELECT l.[Text] AS BranchName, " +                 // 最終顯示名稱
                        "       r.ProNoticeDate AS ProNoticeDate, " +
                        "       r.ProDate       AS ProDate " +
                        "FROM dbo.ProRec  r " +
                        "JOIN dbo.ACABrd  ab ON ab.ID = r.ID " +            // 關聯：ProRec.ID = ACABrd.ID
                        "LEFT JOIN dbo.Lists l " +
                        "  ON l.ParentID = 26 " +                           // 只取分會那群清單
                        " AND l.Value = ab.CreatedByBranchID " +
                        "WHERE r.ID = ?";

        RowMapper<Aca3001QueryDto.Header> HEADER_MAPPER = (rs, i) -> {
            Aca3001QueryDto.Header h = new Aca3001QueryDto.Header();
            h.setBranchName(rs.getString("BranchName"));
            h.setProNoticeDate(getLocalDate(rs, "ProNoticeDate"));
            h.setProDate(getLocalDate(rs, "ProDate"));
            return h;
        };
        List<Aca3001QueryDto.Header> list = jdbcTemplate.query(SQL_FIND_HEADER, HEADER_MAPPER, proRecId);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public Aca3001QueryDto.DirectAdoptCriteria loadDirectCriteria(Integer proAdoptId) {
        Aca3001QueryDto.DirectAdoptCriteria dto = new Aca3001QueryDto.DirectAdoptCriteria();

        final String SQL_DIRECT_OPTIONS =
                "SELECT EntryID, Value, [Text], SortOrder " +
                        "FROM dbo.Lists " +
                        "WHERE ListName = 'PROADOPT_DAC' " +
                        "  AND IsDisabled = 0 " +         // bit 欄位：用 0/1，不要用 '0'/'1'
                        "ORDER BY SortOrder ASC, EntryID ASC";
        // options
        var options = jdbcTemplate.query(SQL_DIRECT_OPTIONS, (rs, i) -> {
            Aca3001QueryDto.DirectAdoptCriteria.Option o = new Aca3001QueryDto.DirectAdoptCriteria.Option();
            o.setEntryId(rs.getInt("EntryID"));
            o.setValue(rs.getString("Value"));
            o.setText(rs.getString("Text"));
            o.setSortOrder(rs.getInt("SortOrder"));
            return o;
        });
        dto.setOptions(options);

        final String SQL_DIRECT_SELECTED =
                "SELECT l.EntryID, l.Value, l.[Text], l.IsDisabled " +
                        "FROM dbo.Lists AS l " +
                        "JOIN dbo.DirectAdoptCriteria AS c " +
                        "  ON c.ListsEntryID = l.EntryID " +
                        "WHERE c.ProAdoptID = ? " +
                        "  AND l.ListName = 'PROADOPT_DAC' " +
                        "ORDER BY l.SortOrder ASC, l.EntryID ASC";
        // selected
        var selected = jdbcTemplate.query(SQL_DIRECT_SELECTED, (rs, i) -> {
            Aca3001QueryDto.DirectAdoptCriteria.Selected s = new Aca3001QueryDto.DirectAdoptCriteria.Selected();
            s.setEntryId(rs.getInt("EntryID"));
            s.setValue(rs.getString("Value"));
            s.setText(rs.getString("Text"));
            s.setDisabled(rs.getBoolean("IsDisabled")); // bit -> boolean
            return s;
        }, proAdoptId);
        dto.setSelected(selected);

        return dto;
    }

    @Override
    public Aca3001QueryDto.EvalAdoptCriteria loadEvalCriteria(Integer proAdoptId) {

        Aca3001QueryDto.EvalAdoptCriteria dto = new Aca3001QueryDto.EvalAdoptCriteria();

        // options
        final String SQL_EVAL_OPTIONS =
                "SELECT EntryID, Value, [Text], SortOrder " +
                        "FROM dbo.Lists " +
                        "WHERE ListName = 'PROADOPT_EAC' " +
                        "  AND IsDisabled = 0 " +         // bit 欄位：用 0/1，不要用 '0'/'1'
                        "ORDER BY SortOrder ASC, EntryID ASC";

        var options = jdbcTemplate.query(SQL_EVAL_OPTIONS, (rs, i) -> {
            Aca3001QueryDto.EvalAdoptCriteria.Option o = new Aca3001QueryDto.EvalAdoptCriteria.Option();
            o.setEntryId(rs.getInt("EntryID"));
            o.setValue(rs.getString("Value"));
            o.setText(rs.getString("Text"));
            o.setSortOrder(rs.getInt("SortOrder"));
            return o;
        });
        dto.setOptions(options);

        // selected
        final String SQL_EVAL_SELECTED =
                "SELECT l.EntryID, l.Value, l.[Text], l.IsDisabled " +
                        "FROM dbo.Lists AS l " +
                        "JOIN dbo.EvalAdoptCriteria AS c " +
                        "  ON c.ListsEntryID = l.EntryID " +
                        "WHERE c.ProAdoptID = ? " +
                        "  AND l.ListName = 'PROADOPT_EAC' " +
                        "ORDER BY l.SortOrder ASC, l.EntryID ASC";

        var selected = jdbcTemplate.query(SQL_EVAL_SELECTED, (rs, i) -> {
            Aca3001QueryDto.EvalAdoptCriteria.Selected s = new Aca3001QueryDto.EvalAdoptCriteria.Selected();
            s.setEntryId(rs.getInt("EntryID"));
            s.setValue(rs.getString("Value"));
            s.setText(rs.getString("Text"));
            s.setDisabled(rs.getBoolean("IsDisabled")); // bit -> boolean
            return s;
        }, proAdoptId);
        dto.setSelected(selected);

        // scores
        final String SQL_EVAL_SCORES =
                "SELECT ScoreEconomy, ScoreEmployment, ScoreFamily, " +
                        "       ScoreSocial, ScorePhysical, ScorePsych, " +
                        "       ScoreParenting, ScoreLegal, ScoreResidence, " +
                        "       ScoreTotal, Comment " +
                        "FROM dbo.ProAdopt WHERE ID = ?";
        dto.setEvalscores(jdbcTemplate.query(SQL_EVAL_SCORES, rs -> {
            Aca3001QueryDto.EvalAdoptCriteria.EvalScore sc = new Aca3001QueryDto.EvalAdoptCriteria.EvalScore();
            if (rs.next()) {
                sc.setScoreEconomy(rs.getInt("ScoreEconomy"));
                sc.setScoreEmployment(rs.getInt("ScoreEmployment"));
                sc.setScoreFamily(rs.getInt("ScoreFamily"));
                sc.setScoreSocial(rs.getInt("ScoreSocial"));
                sc.setScorePhysical(rs.getInt("ScorePhysical"));
                sc.setScorePsych(rs.getInt("ScorePsych"));
                sc.setScoreParenting(rs.getInt("ScoreParenting"));
                sc.setScoreLegal(rs.getInt("ScoreLegal"));
                sc.setScoreResidence(rs.getInt("ScoreResidence"));
                sc.setTotalScore(rs.getInt("ScoreTotal"));
                sc.setComment(rs.getString("Comment"));
            }
            return sc;
        }, proAdoptId));

        return dto;
    }

    @Override
    public Aca3001QueryDto.Summary loadSummary(String proRecId, Integer proAdoptId) {

        // 先拿既存 basics
        Aca3001QueryDto.Summary s = loadSummaryBasics(proRecId);

        // 再把 ProAdopt 的 case 狀態補上（示意：若你把狀態放 ProAdopt 表）
        final String SQL_CASE_STATUS =
                "SELECT " +
                        "    CaseReject, " +
                        "    ReasonReject, " +
                        "    CaseAccept, " +
                        "    ReasonAccept, " +
                        "    CaseEnd, " +
                        "    ReasonEnd " +
                        "FROM dbo.ProAdopt " +
                        "WHERE ID = ?";

        jdbcTemplate.query(SQL_CASE_STATUS, rs -> {
            if (rs.next()) {
                var cs = new Aca3001QueryDto.Summary.CaseStatus();

                var r = new Aca3001QueryDto.Summary.StatusFlag();
                r.setFlag(rs.getBoolean("CaseReject"));
                r.setReason(rs.getString("ReasonReject"));

                var a = new Aca3001QueryDto.Summary.StatusFlag();
                a.setFlag(rs.getBoolean("CaseAccept"));
                a.setReason(rs.getString("ReasonAccept"));

                var e = new Aca3001QueryDto.Summary.StatusFlag();
                e.setFlag(rs.getBoolean("CaseEnd"));
                e.setReason(rs.getString("ReasonEnd"));

                cs.setReject(r);
                cs.setAccept(a);
                cs.setEnd(e);
                s.setCaseStatus(cs);
            }
        }, proAdoptId);

        // serviceTypeSelected 若有獨立關聯表，這裡補查並填 s.setServiceTypeSelected(...)
        final String SQL_SVC_SELECTED =
                "WITH Leaf AS ( " +
                        "  SELECT DISTINCT l.EntryID AS LeafEntryID " +
                        "  FROM dbo.ProRec  r " +
                        "  JOIN dbo.ProDtl  d ON d.ProRecID = r.ID " +
                        "  CROSS APPLY STRING_SPLIT(d.ProItem, ',') s " +
                        "  JOIN dbo.Lists   l " +
                        "    ON l.ListName = 'ACA_PROTECT' " +
                        "   AND ( l.Value = LTRIM(RTRIM(s.value)) " +
                        "      OR l.EntryID = TRY_CONVERT(int, LTRIM(RTRIM(s.value))) ) " +
                        "  WHERE r.ID = ? " +
                        "), cte AS ( " +
                        "  SELECT l.EntryID, l.ParentID, l.[Text], l.[Level], " +
                        "         l.IsDisabled, ISNULL(l.IsDeleted,0) AS IsDeleted, " +
                        "         0 AS depth, lf.LeafEntryID " +
                        "  FROM dbo.Lists l " +
                        "  JOIN Leaf lf ON lf.LeafEntryID = l.EntryID " +
                        "  WHERE l.ListName = 'ACA_PROTECT' " +
                        "  UNION ALL " +
                        "  SELECT p.EntryID, p.ParentID, p.[Text], p.[Level], " +
                        "         p.IsDisabled, ISNULL(p.IsDeleted,0) AS IsDeleted, " +
                        "         c.depth + 1, c.LeafEntryID " +
                        "  FROM dbo.Lists p " +
                        "  JOIN cte c ON p.EntryID = c.ParentID " +
                        "  WHERE p.ListName = 'ACA_PROTECT' " +
                        "    AND c.ParentID <> 37 " +        // ← 遇到其父=37就停止，不再往上把 37 撈進來
                        ") " +
                        "SELECT LeafEntryID, EntryID, [Text], [Level], IsDisabled, IsDeleted " +
                        "FROM cte " +
                        "ORDER BY LeafEntryID, [Level] ASC " +  // 子於 37 的層先出，然後葉
                        "OPTION (MAXRECURSION 32);";

        List<Aca3001QueryDto.Summary.ServiceTypeSelected> svcSelected =
                jdbcTemplate.query(SQL_SVC_SELECTED, ps -> ps.setString(1, proRecId), rs -> {
                    //用 LeafEntryID 當 key，確保同一個葉節點的路徑節點會累積在一起
                    //LinkedHashMap 保持插入順序
                    Map<Integer, Aca3001QueryDto.Summary.ServiceTypeSelected> byLeaf = new LinkedHashMap<>();

                    while (rs.next()) {
                        int leaf = rs.getInt("LeafEntryID");
                        int entryId = rs.getInt("EntryID");
                        String text = rs.getString("Text");
                        boolean dis = rs.getBoolean("IsDisabled");
                        boolean del = rs.getBoolean("IsDeleted");

                        // 跳過根節點 37（雙重保險）
                        if (entryId == 37) continue;


                        // 取得或建立該葉節點的 ServiceTypeSelected
                        // computeIfAbsent：如果 byLeaf 中沒有這個 leaf，則建立一個
                        // 新的 ServiceTypeSelected，並放入 byLeaf 中
                        // 這樣可以確保每個葉節點只會有一個 ServiceTypeSelected 實例
                        // leaf 當作 key，ServiceTypeSelected 當作 value
                        // 注意：這裡的 leaf 是 LeafEntryID，代表這個 ServiceTypeSelected
                        //       是針對哪個葉節點的服務類型選擇
                        //       這樣可以確保每個葉節點的服務類型選擇都會被正確地累積
                        Aca3001QueryDto.Summary.ServiceTypeSelected sel = byLeaf.computeIfAbsent(leaf, k -> {
                            var x = new Aca3001QueryDto.Summary.ServiceTypeSelected();
                            x.setLeafEntryId(k);
                            x.setPathEntryIds(new ArrayList<>());   // 一定要可變
                            x.setPathText(new ArrayList<>());
                            x.setHistoricalEntryIds(new ArrayList<>());
                            x.setHasDisabled(false);
                            return x;
                        });

                        sel.getPathEntryIds().add(entryId);  //[101, 202, 123]       // 已按 Level 升冪：37之下的節點 → 葉
                        sel.getPathText().add(text); //["直接保護", "追蹤輔導訪視", "一般追蹤"]
                        if (dis) sel.setHasDisabled(true); // 是否有禁用的節點
                        if (del) sel.setHasDeleted(true); // 是否有刪除的節點
                        if (dis) sel.getHistoricalEntryIds().add(entryId); // 預設isDisable為歷史節點(or use isDeleted)
                    }
                    //byLeaf.values() 會回傳 Map 裡所有的 value (ServiceTypeSelected 物件集合)
                    //new ArrayList<>(...) 把它轉成一個新的 ArrayList<ServiceTypeSelected>
                    return new ArrayList<>(byLeaf.values());
                });
        /*[
          {
            "leafEntryId": 123,
            "pathEntryIds": [101, 202, 123],
            "pathText": ["直接保護", "追蹤輔導訪視", "一般追蹤"],
            "historicalEntryIds": [],
            "hasDisabled": false
          },
          {
            "leafEntryId": 124,
            "pathEntryIds": [101, 203, 124],
            "pathText": ["直接保護", "追蹤輔導訪視", "含三節慰問"],
            "historicalEntryIds": [124],
            "hasDisabled": true
          }
        ]
*/
        s.setServiceTypeSelected(svcSelected);
        return s;
    }

    @Override
    public Aca3001QueryDto.Summary loadSummaryBasics(String proRecId) {
        return new Aca3001QueryDto.Summary();
    }


    @Override
    public Aca3001QueryDto.DirectAdoptCriteria initDirectCriteriaOptions() {
        return null;
    }

    @Override
    public Aca3001QueryDto.EvalAdoptCriteria initEvalCriteriaOptions() {
        return null;
    }

    // convenience method to convert ResultSet timestamp to LocalDate
    private LocalDate getLocalDate(ResultSet rs, String columnLabel) throws SQLException {
        Timestamp ts = rs.getTimestamp(columnLabel); // datetime 對應 java.sql.Timestamp
        return (ts != null) ? ts.toLocalDateTime().toLocalDate() : null;
    }

}


