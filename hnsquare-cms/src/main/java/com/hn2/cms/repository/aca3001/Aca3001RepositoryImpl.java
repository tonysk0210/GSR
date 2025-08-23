package com.hn2.cms.repository.aca3001;

import com.hn2.cms.dto.aca3001.Aca3001QueryDto;
import com.hn2.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

@Repository
public class Aca3001RepositoryImpl implements Aca3001Repository {

    private final JdbcTemplate jdbcTemplate; // use JdbcTemplate for querying database

    @Autowired
    public Aca3001RepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ========================= 介面實作 "/query" =========================
    @Override
    public Integer findProAdoptIdByProRecId(String proRecId) {
        final String SQL_FIND_PROADOPT_ID = "SELECT ID FROM dbo.ProAdopt WHERE ProRecID = ?";

        List<Integer> proAdoptIds = jdbcTemplate.query(SQL_FIND_PROADOPT_ID, (rs, i) -> rs.getInt("ID"), proRecId);
        return proAdoptIds.isEmpty() ? null : proAdoptIds.get(0); // 空→null，多筆→取第一筆
    }

    @Override
    public Aca3001QueryDto.Profile findProfileByProRecId(String proRecId) {
        final String SQL_FIND_PROFILE =
                "SELECT b.ACAName, b.ACAIDNo, b.ACACardNo " +
                        "FROM dbo.ProRec r " +
                        "JOIN dbo.ACABrd b ON b.ACACardNo = r.ACACardNo " +
                        "WHERE r.ID = ?";

        List<Aca3001QueryDto.Profile> list = jdbcTemplate.query(SQL_FIND_PROFILE, (rs, i) -> {
            var profile = new Aca3001QueryDto.Profile();
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
                "SELECT l.[Text] AS BranchName, r.ProNoticeDate, r.ProDate " +
                        "FROM dbo.ProRec r " +
                        "JOIN dbo.ACABrd ab ON ab.ID = r.ID " +
                        "LEFT JOIN dbo.Lists l ON l.ParentID = 26 AND l.Value = ab.CreatedByBranchID " +
                        "WHERE r.ID = ?";

        List<Aca3001QueryDto.Header> list = jdbcTemplate.query(SQL_FIND_HEADER, (rs, i) -> {
            var header = new Aca3001QueryDto.Header();
            header.setBranchName(rs.getString("BranchName"));
            header.setProNoticeDate(getLocalDateToROC(rs, "ProNoticeDate"));
            header.setProDate(getLocalDateToROC(rs, "ProDate"));
            return header;
        }, proRecId);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 初始化「直接認輔條件（DirectAdoptCriteria）」的選單與預設值。
     * 用途：建立新案件（尚未有 ProAdoptId）或作為既有案件的預設顯示。
     *
     * @return 已載入可用選項且 selected 為空的 {@link Aca3001QueryDto.DirectAdoptCriteria} 物件
     */
    @Override
    public Aca3001QueryDto.DirectAdoptCriteria initDirectCriteriaOptions() {
        var dacDto = new Aca3001QueryDto.DirectAdoptCriteria();
        // dacDto載入「直接認輔條件清單」where IsDisabled = 0
        dacDto.setOptions(loadDirectOptions());
        // dacDto載入所屬「直接認輔條件清單」
        dacDto.setSelected(List.of());
        return dacDto;
    }

    /**
     * 初始化「評估認輔條件（EvalAdoptCriteria）」的選單與預設評分。
     * 用途：建立新案件（尚未有 ProAdoptId）或既有案件的預設載入畫面。
     *
     * @return 已載入可用選項、selected為空、且評分為預設值 0  {@link Aca3001QueryDto.EvalAdoptCriteria}
     */
    @Override
    public Aca3001QueryDto.EvalAdoptCriteria initEvalCriteriaOptions() {
        var eacDto = new Aca3001QueryDto.EvalAdoptCriteria();
        // eacDto載入「評估認輔條件清單」where IsDisabled = 0
        eacDto.setOptions(loadEvalOptions());
        // eacDto載入所屬「評估認輔條件清單」
        eacDto.setSelected(List.of());

        var scDto = new Aca3001QueryDto.EvalAdoptCriteria.EvalScore();
        scDto.setScoreEconomy(0);
        scDto.setScoreEmployment(0);
        scDto.setScoreFamily(0);
        scDto.setScoreSocial(0);
        scDto.setScorePhysical(0);
        scDto.setScorePsych(0);
        scDto.setScoreParenting(0);
        scDto.setScoreLegal(0);
        scDto.setScoreResidence(0);
        scDto.setTotalScore(0);
        scDto.setComment(null);
        // edcDto載入所屬「評估認輔條件分數」預設值
        eacDto.setEvalScores(scDto);

        return eacDto;
    }

    /**
     * 載入指定 ProAdopt 的「直接認輔條件（DirectAdoptCriteria）」：包含可選清單（options）與 已勾選清單（selected）。
     *
     * @param proAdoptId
     * @return 已包含 options 與 selected 的 {@link Aca3001QueryDto.DirectAdoptCriteria}
     */
    @Override
    public Aca3001QueryDto.DirectAdoptCriteria loadDirectCriteria(Integer proAdoptId) {
        var dacDto = new Aca3001QueryDto.DirectAdoptCriteria();
        // dacDto載入「直接認輔條件清單」where IsDisabled = 0
        dacDto.setOptions(loadDirectOptions());

        // dacDto載入所屬「直接認輔條件清單」
        final String SQL_DIRECT_SELECTED =
                "SELECT l.EntryID, l.Value, l.[Text], l.IsDisabled " +
                        "FROM dbo.Lists AS l " +
                        "JOIN dbo.DirectAdoptCriteria AS c " +
                        "  ON c.ListsEntryID = l.EntryID " +
                        "WHERE c.ProAdoptID = ? " +
                        "  AND l.ListName = 'PROADOPT_DAC' " +
                        "ORDER BY l.SortOrder ASC, l.EntryID ASC";
        List<Aca3001QueryDto.DirectAdoptCriteria.Selected> list = jdbcTemplate.query(SQL_DIRECT_SELECTED, (rs, i) -> {
            var selected = new Aca3001QueryDto.DirectAdoptCriteria.Selected();
            selected.setEntryId(rs.getInt("EntryID"));
            selected.setValue(rs.getString("Value"));
            selected.setText(rs.getString("Text"));
            selected.setDisabled(rs.getBoolean("IsDisabled")); // bit -> boolean
            return selected;
        }, proAdoptId);
        dacDto.setSelected(list);

        return dacDto;
    }

    /**
     * 載入指定 ProAdopt 的「評估認輔條件（EvalAdoptCriteria）」：包含可選清單（options）、已勾選清單（selected）與評分（evalScores）。
     *
     * @param proAdoptId
     * @return 已包含 options、selected 與 evalScores 的 {@link Aca3001QueryDto.EvalAdoptCriteria}
     */
    @Override
    public Aca3001QueryDto.EvalAdoptCriteria loadEvalCriteria(Integer proAdoptId) {
        var eacDto = new Aca3001QueryDto.EvalAdoptCriteria();
        // eacDto載入「評估認輔條件清單」where IsDisabled = 0
        eacDto.setOptions(loadEvalOptions());

        // eacDto載入所屬「評估認輔條件清單」
        final String SQL_EVAL_SELECTED =
                "SELECT l.EntryID, l.Value, l.[Text], l.IsDisabled " +
                        "FROM dbo.Lists AS l " +
                        "JOIN dbo.EvalAdoptCriteria AS c ON c.ListsEntryID = l.EntryID " +
                        "WHERE c.ProAdoptID = ? AND l.ListName = 'PROADOPT_EAC' " +
                        "ORDER BY l.SortOrder ASC, l.EntryID ASC";
        List<Aca3001QueryDto.EvalAdoptCriteria.Selected> list = jdbcTemplate.query(SQL_EVAL_SELECTED, (rs, i) -> {
            Aca3001QueryDto.EvalAdoptCriteria.Selected selected = new Aca3001QueryDto.EvalAdoptCriteria.Selected();
            selected.setEntryId(rs.getInt("EntryID"));
            selected.setValue(rs.getString("Value"));
            selected.setText(rs.getString("Text"));
            selected.setDisabled(rs.getBoolean("IsDisabled")); // bit -> boolean
            return selected;
        }, proAdoptId);
        eacDto.setSelected(list);

        // eacDto載入所屬「評估認輔條件分數」
        final String SQL_EVAL_SCORES =
                "SELECT ScoreEconomy, ScoreEmployment, ScoreFamily, " +
                        "   ScoreSocial, ScorePhysical, ScorePsych, " +
                        "   ScoreParenting, ScoreLegal, ScoreResidence, " +
                        "   ScoreTotal, Comment " +
                        "FROM dbo.ProAdopt WHERE ID = ?";
        eacDto.setEvalScores(jdbcTemplate.query(SQL_EVAL_SCORES, rs -> { //ResultSetExtractor<T>
            var evalScore = new Aca3001QueryDto.EvalAdoptCriteria.EvalScore();
            if (rs.next()) {
                evalScore.setScoreEconomy(rs.getInt("ScoreEconomy"));
                evalScore.setScoreEmployment(rs.getInt("ScoreEmployment"));
                evalScore.setScoreFamily(rs.getInt("ScoreFamily"));
                evalScore.setScoreSocial(rs.getInt("ScoreSocial"));
                evalScore.setScorePhysical(rs.getInt("ScorePhysical"));
                evalScore.setScorePsych(rs.getInt("ScorePsych"));
                evalScore.setScoreParenting(rs.getInt("ScoreParenting"));
                evalScore.setScoreLegal(rs.getInt("ScoreLegal"));
                evalScore.setScoreResidence(rs.getInt("ScoreResidence"));
                evalScore.setTotalScore(rs.getInt("ScoreTotal"));
                evalScore.setComment(rs.getString("Comment"));
            }
            return evalScore;
        }, proAdoptId));

        return eacDto;
    }

    /**
     * 載入「案件摘要（Summary）」：
     * 1) 服務類型選擇（serviceTypeSelected）：由個案在 ACA_PROTECT 勾選到的每個「葉節點」(LeafEntryID) 向上回溯父節點，
     *    組成由「父 → … → 葉」的完整路徑（不含根節點 EntryID=37），並彙總禁用/刪除狀態。
     * 2) 基本欄位：同時查詢並設定 Pro_EmploymentStaus（對應屬性：proEmploymentStatus）與 ProStatus（對應屬性：proStatus）。
     *
     * @param proRecId
     * @return 已組裝完成的 {@link Aca3001QueryDto.Summary}（永不為 {@code null}）
     */
    @Override
    public Aca3001QueryDto.Summary loadSummaryBasics(String proRecId) {
        final String SQL_LOAD_SERVICE_SELECTED =
                "WITH Leaf AS ( " + //該個案在 ACA_PROTECT 裡面勾到哪些葉子代碼
                        "  SELECT DISTINCT l.EntryID AS LeafEntryID " +
                        "  FROM dbo.ProRec  r " +
                        "  JOIN dbo.ProDtl  d ON d.ProRecID = r.ID " +
                        "  CROSS APPLY STRING_SPLIT(d.ProItem, ',') s " +
                        "  JOIN dbo.Lists   l " +
                        "    ON l.ListName = 'ACA_PROTECT' " +
                        "   AND ( l.Value = LTRIM(RTRIM(s.value)) " +
                        "      OR l.EntryID = TRY_CONVERT(int, LTRIM(RTRIM(s.value))) ) " +
                        "  WHERE r.ID = ? " +
                        "), cte AS ( " + //這是遞迴的起點 (anchor row)
                        "  SELECT l.EntryID, l.ParentID, l.[Text], l.[Level], " +
                        "         l.IsDisabled, ISNULL(l.IsDeleted,0) AS IsDeleted, " +
                        "         0 AS depth, lf.LeafEntryID " +
                        "  FROM dbo.Lists l " +
                        "  JOIN Leaf lf ON lf.LeafEntryID = l.EntryID " +
                        "  WHERE l.ListName = 'ACA_PROTECT' " +
                        "  UNION ALL " + //這是遞迴部分，會一直撈父節點，直到遇到 37 為止
                        "  SELECT p.EntryID, p.ParentID, p.[Text], p.[Level], " +
                        "         p.IsDisabled, ISNULL(p.IsDeleted,0) AS IsDeleted, " +
                        "         c.depth + 1, c.LeafEntryID " +
                        "  FROM dbo.Lists p " +
                        "  JOIN cte c ON p.EntryID = c.ParentID " +
                        "  WHERE p.ListName = 'ACA_PROTECT' " +
                        "    AND c.ParentID <> 37 " + //每個 葉子 (LeafEntryID) 對應的一條向上路徑（直到遇到 Parent=37）
                        ") " +
                        "SELECT LeafEntryID, EntryID, [Text], [Level], IsDisabled, IsDeleted " +
                        "FROM cte " +
                        "ORDER BY LeafEntryID, [Level] ASC " +  //先依葉子 ID，再依層級排序
                        "OPTION (MAXRECURSION 32);"; //限制遞迴最多 32 層，避免無限迴圈

        final String SQL_LOAD_EMPLOYMENTSTATUS_AND_PROSTATUS =
                "SELECT r.Pro_EmploymentStaus AS ProEmploymentStatus, r.ProStatus AS ProStatus " +
                        "FROM dbo.ProRec r WHERE r.ID = ?";

        var summary = new Aca3001QueryDto.Summary();

        List<Aca3001QueryDto.Summary.ServiceTypeSelected> list = jdbcTemplate.query(SQL_LOAD_SERVICE_SELECTED, rs -> {

            // 以 LeafEntryID 分組：同一個「葉節點」只會有一個容器（value），用來累積該葉的整條路徑
            Map<Integer, Aca3001QueryDto.Summary.ServiceTypeSelected> byLeaf = new LinkedHashMap<>();

            // 每一列 = 路徑上的一個節點（父/祖先...直到葉），SQL 已依 LeafEntryID, Level 升冪排序
            while (rs.next()) {
                int leaf = rs.getInt("LeafEntryID"); // 此列屬於哪個葉節點
                int entryId = rs.getInt("EntryID"); // 路徑中的某個節點 ID（不一定是葉節點）
                if (entryId == 37) continue; // 略過根節點（保護類別），不納入路徑

                String text = rs.getString("Text");
                boolean dis = rs.getBoolean("IsDisabled");
                boolean del = rs.getBoolean("IsDeleted");

                // 取得/建立「此葉節點」的容器：第一次遇到 leaf 會初始化一個空容器，其後重複使用
                var serviceTypeSelectedDto = byLeaf.computeIfAbsent(leaf, k -> {
                    var x = new Aca3001QueryDto.Summary.ServiceTypeSelected();
                    x.setLeafEntryId(k);
                    x.setPathEntryIds(new ArrayList<>());
                    x.setPathText(new ArrayList<>());
                    x.setHistoricalEntryIds(new ArrayList<>());
                    x.setHasDisabled(false);
                    return x;
                });

                // 把目前節點接到路徑尾端（Level ASC 已保證父在前、葉在最後）
                serviceTypeSelectedDto.getPathEntryIds().add(entryId);
                serviceTypeSelectedDto.getPathText().add(text);

                // 任何一個節點被禁用/刪除，都把整條路徑的彙總旗標設為 true
                if (dis) serviceTypeSelectedDto.setHasDisabled(true);
                if (del) serviceTypeSelectedDto.setHasDeleted(true);

                // 收集需標示的節點：此處以「禁用」為歷史依據；若要改成以「已刪除」為準，改這行即可
                if (dis) serviceTypeSelectedDto.getHistoricalEntryIds().add(entryId);
            }

            // 將分組結果由 Map 轉為 List；若無資料則回傳空清單（避免回傳 null）
            return byLeaf.isEmpty()
                    ? Collections.emptyList()
                    : new ArrayList<>(byLeaf.values());
        }, proRecId);
        //1. summary 載入「服務類型選擇」ServiceTypeSelected
        summary.setServiceTypeSelected(list);


        jdbcTemplate.query(SQL_LOAD_EMPLOYMENTSTATUS_AND_PROSTATUS, rs -> {
            if (rs.next()) {
                //2. summary載入 ProRec 的 Pro_EmploymentStaus 與 ProStatus
                summary.setProEmploymentStatus(rs.getString("ProEmploymentStatus"));
                summary.setProStatus(rs.getString("ProStatus"));
            }
            return null; // ResultSetExtractor 需要回傳值，但我們直接修改外部物件
        }, proRecId);

        return summary;
    }

    /**
     *
     * @param proAdoptId
     * @return
     */
    @Override
    public Aca3001QueryDto.Summary.CaseStatus loadCaseStatus(Integer proAdoptId) {

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

        return jdbcTemplate.query(SQL_CASE_STATUS, rs -> {

            if (!rs.next()) return null; // 若 ProAdopt 不存在，則回傳 null
            // 若 ProAdopt 存在，則建立 CaseStatus 物件
            // 注意：這裡的 rs 是 ResultSet，i 是行號（從 0 開始）
            // rs.getBoolean("CaseReject") 會從 ResultSet 中取出名為 "CaseReject" 的欄位值
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
            return cs;
        }, proAdoptId);

    }

    //


    // ---------- 私有方法區 ----------

    /**
     * 從當前 {@link ResultSet} 的指定欄位讀取 DATETIME/DATETIME2 值，並轉換為民國日期字串（格式：yyy年M月d日）。
     *
     * @param rs
     * @param columnLabel
     * @return 民國日期字串（yyy年M月d日），若欄位為 NULL 則回傳 {@code null}
     * @throws SQLException
     */
    private String getLocalDateToROC(ResultSet rs, String columnLabel) throws SQLException {
        Timestamp ts = rs.getTimestamp(columnLabel); // 讀出該欄位的 Timestamp
        String roc = (ts == null) ? null : DateUtil.date2Roc(new java.util.Date(ts.getTime()), DateUtil.DateFormat.yyy年M月d日);
        return roc;
    }


    /**
     * 載入「直接認輔條件」IsDisabled = 0（僅載入目前有效的條件）
     * 建立新 ProAdopt（proAdoptId = null）與查詢既有 ProAdopt（proAdoptId != null）皆會使用。
     *
     * @return 已排序的直接認輔條件選項清單（永不為 {@code null}）
     */
    private List<Aca3001QueryDto.DirectAdoptCriteria.Option> loadDirectOptions() {
        final String sql =
                "SELECT EntryID, Value, [Text], SortOrder FROM dbo.Lists " +
                        "WHERE ListName = 'PROADOPT_DAC' AND IsDisabled = 0 " +
                        "ORDER BY SortOrder ASC, EntryID ASC";
        return jdbcTemplate.query(sql, (rs, i) -> {
            var option = new Aca3001QueryDto.DirectAdoptCriteria.Option();
            option.setEntryId(rs.getInt("EntryID"));
            option.setValue(rs.getString("Value"));
            option.setText(rs.getString("Text"));
            option.setSortOrder(rs.getInt("SortOrder"));
            return option;
        });
    }

    /**
     * 載入「評估認輔條件」IsDisabled = 0（僅載入目前有效的條件）
     * 建立新 ProAdopt（proAdoptId = null）與查詢既有 ProAdopt（proAdoptId != null）皆會使用。
     *
     * @return 已排序的評估認輔條件選項清單（永不為 {@code null}）
     */
    private List<Aca3001QueryDto.EvalAdoptCriteria.Option> loadEvalOptions() {
        final String sql =
                "SELECT EntryID, Value, [Text], SortOrder FROM dbo.Lists " +
                        "WHERE ListName = 'PROADOPT_EAC' AND IsDisabled = 0 " +
                        "ORDER BY SortOrder ASC, EntryID ASC";
        return jdbcTemplate.query(sql, (rs, i) -> {
            var option = new Aca3001QueryDto.EvalAdoptCriteria.Option();
            option.setEntryId(rs.getInt("EntryID"));
            option.setValue(rs.getString("Value"));
            option.setText(rs.getString("Text"));
            option.setSortOrder(rs.getInt("SortOrder"));
            return option;
        });
    }

}


