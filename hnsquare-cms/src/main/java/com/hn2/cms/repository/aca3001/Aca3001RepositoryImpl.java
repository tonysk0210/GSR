package com.hn2.cms.repository.aca3001;

import com.hn2.cms.dto.aca3001.Aca3001QueryDto;

import com.hn2.cms.payload.aca3001.Aca3001SavePayload;
import com.hn2.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Aca3001 認輔評估查詢 Repository（JdbcTemplate 版本）
 * 命名一致：find* 專注單值/存在性；compute/load 用於組裝 DTO。
 * <p>
 * 功能：
 * - 對應 /query 需求，依據 proRecId / proAdoptId 查詢並組裝 DTO。
 * - Meta：時間鎖與可編輯性計算。
 * - Header / Profile：載入案件與個案資訊。
 * - DirectAdoptCriteria / EvalAdoptCriteria：載入直接與評估認輔條件（選項、已選、分數）。
 * - Summary：整合服務類型路徑、就業/案件狀態、案件結論。
 */
@Repository
public class Aca3001RepositoryImpl implements Aca3001Repository {

    private final JdbcTemplate jdbcTemplate;

    // Query API
    @Autowired
    public Aca3001RepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 依 proRecId 取得對應的認輔評估表 ID (ProAdopt.ID)。
     * 若尚未建立則回傳 null；若有多筆僅取第一筆。
     */
    @Override
    public Integer findProAdoptIdByProRecId(String proRecId) {
        final String sql = "SELECT ID FROM dbo.ProAdopt WHERE ProRecID = ?";
        return jdbcTemplate.query(sql, (rs, i) -> rs.getInt("ID"), proRecId)
                .stream()
                .findFirst()
                .orElse(null); // 空→null，多筆→取第一筆
    }

    /**
     * 計算 Meta 區塊：
     * - proRecId、proAdoptId 基本資訊
     * - lockDate：Lists.TIMELOCK_ACABRD 對應的日期（可能為 null）
     * - editable 規則：
     * 1. 無 timeLock → 可編輯
     * 2. proDate 為 null → 可編輯
     * 3. 其餘：proDate > lockDate 才可編輯
     */
    @Override
    public Aca3001QueryDto.Meta computeMeta(String proRecId, Integer proAdoptId) {

        // 4) 建立 MetaDto
        var meta = new Aca3001QueryDto.Meta();
        meta.setProRecId(proRecId);
        meta.setProAdoptId(proAdoptId);
        meta.setLockDate(loadTimeLockDate()); // 可能為 null
        meta.setEditable(isEditable(proRecId, loadTimeLockDate()));
        return meta;
    }

    /**
     * 查詢表頭資訊 (Header)：
     * - BranchName：由 ACABrd.CreatedByBranchID 對應 Lists.Text
     * - ProNoticeDate / ProDate：轉換為民國日期字串
     * 查無資料時回傳 null。
     */
    @Override
    public Aca3001QueryDto.Header computeHeader(String proRecId) {
        final String SQL_HEADER =
                "SELECT l.[Text] AS BranchName, r.ProNoticeDate, r.ProDate " +
                        "FROM dbo.ProRec r " +
                        "JOIN dbo.ACABrd ab ON ab.ID = r.ID " +
                        "LEFT JOIN dbo.Lists l ON l.ParentID = 26 AND l.Value = ab.CreatedByBranchID " +
                        "WHERE r.ID = ?";

        return jdbcTemplate.query(SQL_HEADER, ps -> ps.setString(1, proRecId), rs -> {
            if (!rs.next()) return null;
            var header = new Aca3001QueryDto.Header();
            header.setBranchName(rs.getString("BranchName"));
            header.setProNoticeDate(getLocalDateToROC(rs, "ProNoticeDate"));
            header.setProDate(getLocalDateToROC(rs, "ProDate"));
            return header;
        });
    }

    /**
     * 查詢個案基本資料 (Profile)：
     * - 姓名、身分證號、案號
     * 查無資料時回傳 null。
     */
    @Override
    public Aca3001QueryDto.Profile computeProfile(String proRecId) {
        final String SQL_PROFILE =
                "SELECT b.ACAName, b.ACAIDNo, b.ACACardNo " +
                        "FROM dbo.ProRec r " +
                        "JOIN dbo.ACABrd b ON b.ACACardNo = r.ACACardNo " +
                        "WHERE r.ID = ?";

        return jdbcTemplate.query(SQL_PROFILE, ps -> ps.setString(1, proRecId), rs -> {
            if (!rs.next()) return null;
            var p = new Aca3001QueryDto.Profile();
            p.setAcaName(rs.getString("ACAName"));
            p.setAcaIdNo(rs.getString("ACAIDNo"));
            p.setAcaCardNo(rs.getString("ACACardNo"));
            return p;
        });
    }

    /**
     * 載入直接認輔條件 (DirectAdoptCriteria)：
     * - options：所有有效 Lists (PROADOPT_DAC, IsDisabled=0)
     * - selected：若 proAdoptId != null，則載入該案件已勾選條件（含已失效標示）
     * 新建情境 selected 為空集合。
     */
    @Override
    public Aca3001QueryDto.DirectAdoptCriteria computeDirectAdoptCriteria(Integer proAdoptId) {
        final String SQL_OPTIONS =
                "SELECT EntryID, Value, [Text], SortOrder FROM dbo.Lists " +
                        "WHERE ListName = 'PROADOPT_DAC' AND IsDisabled = 0 " +
                        "ORDER BY SortOrder ASC, EntryID ASC";
        final String SQL_SELECTED =
                "SELECT l.EntryID, l.Value, l.[Text], l.IsDisabled " +
                        "FROM dbo.Lists AS l " +
                        "JOIN dbo.DirectAdoptCriteria AS c " +
                        "  ON c.ListsEntryID = l.EntryID " +
                        "WHERE c.ProAdoptID = ? " +
                        "  AND l.ListName = 'PROADOPT_DAC' " +
                        "ORDER BY l.SortOrder ASC, l.EntryID ASC";

        // 1) 組 DTO
        var dto = new Aca3001QueryDto.DirectAdoptCriteria();

        // 2) options：一律來自 Lists（僅取 IsDisabled=0）
        List<Aca3001QueryDto.DirectAdoptCriteria.Option> listOptions =
                jdbcTemplate.query(SQL_OPTIONS, (rs, i) -> {
                    var o = new Aca3001QueryDto.DirectAdoptCriteria.Option();
                    o.setEntryId(rs.getInt("EntryID"));
                    o.setValue(rs.getString("Value"));
                    o.setText(rs.getString("Text"));
                    o.setSortOrder(rs.getInt("SortOrder"));
                    return o;
                });
        dto.setOptions(listOptions);

        // 3) selected：若 proAdoptId 為 null → 空清單；否則撈實際勾選
        List<Aca3001QueryDto.DirectAdoptCriteria.Selected> listSelected;
        if (proAdoptId == null) {
            listSelected = List.of();
        } else {
            listSelected = jdbcTemplate.query(SQL_SELECTED, (rs, i) -> {
                var scores = new Aca3001QueryDto.DirectAdoptCriteria.Selected();
                scores.setEntryId(rs.getInt("EntryID"));
                scores.setValue(rs.getString("Value"));
                scores.setText(rs.getString("Text"));
                scores.setDisabled(rs.getBoolean("IsDisabled")); // bit -> boolean
                return scores;
            }, proAdoptId);
        }
        dto.setSelected(listSelected);

        return dto;
    }

    /**
     * 載入評估認輔條件 (EvalAdoptCriteria)：
     * - options：所有有效 Lists (PROADOPT_EAC)
     * - selected：案件已勾選條件（含已失效標示）
     * - evalScores：各面向分數 + 總分
     * 若 proAdoptId = null，則 selected 為空、evalScores 為預設值。
     */
    @Override
    public Aca3001QueryDto.EvalAdoptCriteria computeEvalAdoptCriteria(Integer proAdoptId) {

        final String SQL_OPTIONS =
                "SELECT EntryID, Value, [Text], SortOrder FROM dbo.Lists " +
                        "WHERE ListName = 'PROADOPT_EAC' AND IsDisabled = 0 " +
                        "ORDER BY SortOrder ASC, EntryID ASC";
        final String SQL_SELECTED =
                "SELECT l.EntryID, l.Value, l.[Text], l.IsDisabled " +
                        "FROM dbo.Lists AS l " +
                        "JOIN dbo.EvalAdoptCriteria AS c ON c.ListsEntryID = l.EntryID " +
                        "WHERE c.ProAdoptID = ? AND l.ListName = 'PROADOPT_EAC' " +
                        "ORDER BY l.SortOrder ASC, l.EntryID ASC";
        final String SQL_SCORES =
                "SELECT ScoreEconomy, ScoreEmployment, ScoreFamily, " +
                        "   ScoreSocial, ScorePhysical, ScorePsych, " +
                        "   ScoreParenting, ScoreLegal, ScoreResidence, " +
                        "   ScoreTotal, Comment " +
                        "FROM dbo.ProAdopt WHERE ID = ?";

        var dto = new Aca3001QueryDto.EvalAdoptCriteria();

        // 1) options：一律載入有效選項
        List<Aca3001QueryDto.EvalAdoptCriteria.Option> listOptions =
                jdbcTemplate.query(SQL_OPTIONS, (rs, i) -> {
                    var o = new Aca3001QueryDto.EvalAdoptCriteria.Option();
                    o.setEntryId(rs.getInt("EntryID"));
                    o.setValue(rs.getString("Value"));
                    o.setText(rs.getString("Text"));
                    o.setSortOrder(rs.getInt("SortOrder"));
                    return o;
                });
        dto.setOptions(listOptions);

        // 2) selected + scores：依 proAdoptId 是否為 null 決定
        if (proAdoptId == null) {
            // 新建情境：selected 空、scores 預設 0
            dto.setSelected(List.of());
            dto.setEvalScores(new Aca3001QueryDto.EvalAdoptCriteria.EvalScore());
        } else {
            // 2-1 selected：載入此個案勾選過的評估條件（保留 IsDisabled 以便前端標示「已失效」）
            List<Aca3001QueryDto.EvalAdoptCriteria.Selected> selected =
                    jdbcTemplate.query(SQL_SELECTED, (rs, i) -> {
                        var scores = new Aca3001QueryDto.EvalAdoptCriteria.Selected();
                        scores.setEntryId(rs.getInt("EntryID"));
                        scores.setValue(rs.getString("Value"));
                        scores.setText(rs.getString("Text"));
                        scores.setDisabled(rs.getBoolean("IsDisabled"));
                        return scores;
                    }, proAdoptId);
            dto.setSelected(selected);

            // 2-2 scores：從 ProAdopt 載入；若查無資料則給預設值（保險）
            Aca3001QueryDto.EvalAdoptCriteria.EvalScore scores =
                    jdbcTemplate.query(SQL_SCORES, rs -> {
                        if (rs.next()) {
                            var sc = new Aca3001QueryDto.EvalAdoptCriteria.EvalScore();
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
                            return sc;
                        }
                        return new Aca3001QueryDto.EvalAdoptCriteria.EvalScore();
                    }, proAdoptId);
            dto.setEvalScores(scores);
        }

        return dto;
    }

    /**
     * 整合 Summary 區塊：
     * - 服務類型選擇：遞迴展開 ACA_PROTECT 路徑，並判斷是否含失效/刪除節點
     * - ProEmploymentStatus / ProStatus：來源 ProRec
     * - CaseStatus：來源 ProAdopt (Reject / Accept / End 任一，否則 NONE)
     * 若 proAdoptId = null，CaseStatus 回傳預設 NONE。
     */
    @Override
    public Aca3001QueryDto.Summary computeSummary(String proRecId, Integer proAdoptId) {
        final String SQL_SERVICE =
                "WITH Leaf AS ( " + //該個案在 ACA_PROTECT 裡面勾到哪些葉子代碼
                        "  SELECT DISTINCT l.EntryID AS LeafEntryID " +
                        "  FROM dbo.ProRec  r " +
                        "  JOIN dbo.ProDtl  d ON d.ProRecID = r.ID " +
                        "  CROSS APPLY STRING_SPLIT(d.ProItem, ',') scores " +
                        "  JOIN dbo.Lists   l " +
                        "    ON l.ListName = 'ACA_PROTECT' " +
                        "   AND ( l.Value = LTRIM(RTRIM(scores.value)) " +
                        "      OR l.EntryID = TRY_CONVERT(int, LTRIM(RTRIM(scores.value))) ) " +
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
        final String SQL_EMPLOYMENTSTATUS_AND_PROSTATUS =
                "SELECT r.Pro_EmploymentStaus AS ProEmploymentStatus, r.ProStatus AS ProStatus " +
                        "FROM dbo.ProRec r WHERE r.ID = ?";
        final String SQL_CASESTATUS =
                "SELECT CaseReject, " +
                        "ReasonReject, " +
                        "CaseAccept, " +
                        "ReasonAccept, " +
                        "CaseEnd, " +
                        "ReasonEnd " +
                        "FROM dbo.ProAdopt " +
                        "WHERE ID = ?";
        var summary = new Aca3001QueryDto.Summary();
        List<Aca3001QueryDto.Summary.ServiceTypeSelected> listService = jdbcTemplate.query(SQL_SERVICE, rs -> {

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
                    ? List.of()
                    : new ArrayList<>(byLeaf.values());
        }, proRecId);

        //1. summary 載入「服務類型選擇」ServiceTypeSelected
        summary.setServiceTypeSelected(listService);

        jdbcTemplate.query(SQL_EMPLOYMENTSTATUS_AND_PROSTATUS, rs -> {
            if (rs.next()) {
                //2. summary載入 ProRec 的 Pro_EmploymentStaus 與 ProStatus
                summary.setProEmploymentStatus(rs.getString("ProEmploymentStatus"));
                summary.setProStatus(rs.getString("ProStatus"));
            }
            return null; // ResultSetExtractor 需要回傳值，但我們直接修改外部物件
        }, proRecId);

        // proAdoptId 為 null → 尚未建立 ProAdopt，直接回傳預設值
        if (proAdoptId == null) {
            summary.setCaseStatus(new Aca3001QueryDto.Summary.CaseStatus());
        } else {
            Aca3001QueryDto.Summary.CaseStatus caseStatus = jdbcTemplate.query(SQL_CASESTATUS, rs -> {

                // 找不到資料 → 回傳預設值
                if (!rs.next()) {
                    return new Aca3001QueryDto.Summary.CaseStatus();
                }

                var cs = new Aca3001QueryDto.Summary.CaseStatus();

                if (rs.getBoolean("CaseReject")) {
                    cs.setCaseState(Aca3001QueryDto.Summary.CaseStatus.CaseState.REJECT);
                    cs.setReason(rs.getString("ReasonReject").trim());
                } else if (rs.getBoolean("CaseAccept")) {
                    cs.setCaseState(Aca3001QueryDto.Summary.CaseStatus.CaseState.ACCEPT);
                    cs.setReason(rs.getString("ReasonAccept").trim());
                } else if (rs.getBoolean("CaseEnd")) {
                    cs.setCaseState(Aca3001QueryDto.Summary.CaseStatus.CaseState.END);
                    cs.setReason(rs.getString("ReasonEnd").trim());
                } else {
                    cs.setCaseState(Aca3001QueryDto.Summary.CaseStatus.CaseState.NONE);
                    cs.setReason(null);
                }
                return cs;
            }, proAdoptId);
            //3. summary載入 ProAdopt 的 CaseStatus
            summary.setCaseStatus(caseStatus);
        }
        return summary;
    }


    //Save API
    @Override
    public Integer insertProAdopt(String proRecId, Aca3001SavePayload.@NotNull @Valid Scores scores, boolean caseReject, String reasonReject, boolean caseAccept, String reasonAccept, boolean caseEnd, String reasonEnd, Integer createdByUserId) {
        final String sql = "INSERT INTO dbo.ProAdopt " +
                "(ProRecID, " +
                " ScoreEconomy, ScoreEmployment, ScoreFamily, ScoreSocial, " +
                " ScorePhysical, ScorePsych, ScoreParenting, ScoreLegal, ScoreResidence, " +
                " [Comment], " +
                " CaseReject, ReasonReject, CaseAccept, ReasonAccept, CaseEnd, ReasonEnd, " +
                " CreatedByUserID, CreatedOnDate) " +
                "OUTPUT INSERTED.ID " +
                "VALUES " +
                "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                " ?, ?, ?, ?, ?, ?, ?, " +
                " ?, GETDATE())";
        //回傳新紀錄的 ID
        return jdbcTemplate.queryForObject(sql, Integer.class, // 希望回傳的型別
                proRecId,
                scores.getEconomy(), scores.getEmployment(), scores.getFamily(), scores.getSocial(),
                scores.getPhysical(), scores.getPsych(), scores.getParenting(), scores.getLegal(), scores.getResidence(),
                nullOrTrim(scores.getComment()),
                caseReject, reasonReject,
                caseAccept, reasonAccept,
                caseEnd, reasonEnd,
                createdByUserId
        );
    }

    @Override
    public void updateProAdopt(Integer proAdoptId, Aca3001SavePayload.@NotNull @Valid Scores scores, boolean caseReject, String reasonReject, boolean caseAccept, String reasonAccept, boolean caseEnd, String reasonEnd, Integer modifiedByUserId) {
        String sql =
                "UPDATE dbo.ProAdopt " +
                        "   SET ScoreEconomy    = ?, " +
                        "       ScoreEmployment = ?, " +
                        "       ScoreFamily     = ?, " +
                        "       ScoreSocial     = ?, " +
                        "       ScorePhysical   = ?, " +
                        "       ScorePsych      = ?, " +
                        "       ScoreParenting  = ?, " +
                        "       ScoreLegal      = ?, " +
                        "       ScoreResidence  = ?, " +
                        "       [Comment]       = ?, " +
                        "       CaseReject      = ?, " +
                        "       ReasonReject    = ?, " +
                        "       CaseAccept      = ?, " +
                        "       ReasonAccept    = ?, " +
                        "       CaseEnd         = ?, " +
                        "       ReasonEnd       = ?, " +
                        "       ModifiedByUserID= ?, " +
                        "       ModifiedOnDate  = GETDATE() " +
                        " WHERE ID = ?";
        jdbcTemplate.update(sql,
                scores.getEconomy(), scores.getEmployment(), scores.getFamily(), scores.getSocial(),
                scores.getPhysical(), scores.getPsych(), scores.getParenting(), scores.getLegal(), scores.getResidence(),
                nullOrTrim(scores.getComment()),
                caseReject, reasonReject,
                caseAccept, reasonAccept,
                caseEnd, reasonEnd,
                modifiedByUserId,
                proAdoptId
        );
    }

    @Override
    public void replaceDirectAdoptCriteria(Integer proAdoptId, List<Integer> directSelectedEntryIds) {
        jdbcTemplate.update("DELETE FROM dbo.DirectAdoptCriteria WHERE ProAdoptID = ?", proAdoptId);
        if (directSelectedEntryIds == null || directSelectedEntryIds.isEmpty()) return;
        jdbcTemplate.batchUpdate(
                "INSERT INTO dbo.DirectAdoptCriteria (ProAdoptID, ListsEntryID) VALUES (?, ?)",
                directSelectedEntryIds,
                directSelectedEntryIds.size(),
                (ps, entryId) -> {
                    ps.setInt(1, proAdoptId);
                    ps.setInt(2, entryId);
                }
        );
    }

    @Override
    public void replaceEvalAdoptCriteria(Integer proAdoptId, List<Integer> evalSelectedEntryIds) {
        jdbcTemplate.update("DELETE FROM dbo.EvalAdoptCriteria WHERE ProAdoptID = ?", proAdoptId);
        if (evalSelectedEntryIds == null || evalSelectedEntryIds.isEmpty()) return;
        jdbcTemplate.batchUpdate(
                "INSERT INTO dbo.EvalAdoptCriteria (ProAdoptID, ListsEntryID) VALUES (?, ?)",
                evalSelectedEntryIds,
                evalSelectedEntryIds.size(),
                (ps, entryId) -> {
                    ps.setInt(1, proAdoptId);
                    ps.setInt(2, entryId);
                }
        );
    }

    // Delete API
    @Override
    public void deleteProAdoptCascade(Integer proAdoptId) {
        jdbcTemplate.update("DELETE FROM dbo.ProAdopt WHERE ID = ?", proAdoptId);
    }

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

    private static String nullOrTrim(String s) {
        return (s == null) ? null : s.trim();
    }

    public LocalDate loadTimeLockDate() {
        final String SQL_TIMELOCK = "SELECT TOP 1 Value FROM Lists WHERE ListName = 'TIMELOCK_ACABRD'";
        String timeLockStr = jdbcTemplate.query(SQL_TIMELOCK, rs -> rs.next() ? rs.getString(1) : null);

        if (timeLockStr == null || timeLockStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(timeLockStr.trim(), DateTimeFormatter.ofPattern("yyyy/M/d"));
        } catch (Exception e) {
            // 建議 log.warn("Invalid TIMELOCK_ACABRD: {}", timeLockStr, e);
            return null;
        }
    }

    public boolean isEditable(String proRecId, LocalDate timeLockDate) {
        final String SQL_PRODATE = "SELECT ProDate FROM ProRec WHERE ID = ?";

        // 取得 proDate（可為 null）
        LocalDate proDate = jdbcTemplate.query(SQL_PRODATE, ps -> ps.setString(1, proRecId), rs -> {
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("ProDate");
                return (ts != null) ? ts.toLocalDateTime().toLocalDate() : null;
            }
            return null;
        });

        // 規則判斷
        return (timeLockDate == null) || (proDate == null) || proDate.isAfter(timeLockDate);
    }
}


