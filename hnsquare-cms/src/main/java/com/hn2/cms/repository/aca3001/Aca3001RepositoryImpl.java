package com.hn2.cms.repository.aca3001;

import com.hn2.cms.dto.aca3001.Aca3001QueryDto;

import com.hn2.cms.payload.aca3001.Aca3001SavePayload;
import com.hn2.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    public Aca3001RepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Query API ------------------------------------------------------------------------------

    /**
     * 依 ProRecID 查詢對應的 ProAdopt 主鍵 ID。
     * <p>
     * 規則：
     * - 若查無資料 → 回傳 null
     * - 若多筆資料（理論上不該發生，因為 ProRecID 應該具唯一性） → 取第一筆
     *
     * @param proRecId 個案紀錄 ID (FK → ProRec.ID)
     * @return 對應的 ProAdopt.ID；查無資料時回傳 null
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
     * 組裝案件的 Meta 資訊。
     * <p>
     * Meta 包含：
     * - proRecId    ：個案紀錄 ID
     * - proAdoptId  ：認輔評估 ID（可能為 null，表示尚未建立）
     * - lockDate     ：系統定義的鎖定日（來源：Lists.TIMELOCK_ACABRD，可能為 null）
     * - editable     ：是否可編輯（依 proRecId 與 lockDate 判斷）
     *
     * @param proRecId   個案紀錄 ID
     * @param proAdoptId 認輔評估 ID（可為 null）
     * @return 組裝完成的 Meta DTO
     */
    @Override
    public Aca3001QueryDto.Meta computeMeta(String proRecId, Integer proAdoptId) {

        // 1) 先查鎖定日（可能為 null）
        LocalDate lockDate = loadTimeLockDate();

        // 2) 建立並填入 MetaDto
        var meta = new Aca3001QueryDto.Meta();
        meta.setProRecId(proRecId);
        meta.setProAdoptId(proAdoptId);
        meta.setLockDate(lockDate); // 可能為 null
        meta.setEditable(isEditable(proRecId, loadTimeLockDate())); // 判斷是否允許編輯

        return meta;
    }

    /**
     * 查詢並組裝案件的 Header 資訊。
     * <p>
     * Header 包含：
     * - BranchName     ：承辦分會名稱（Lists.Text）
     * - ProNoticeDate  ：通知日期（轉換為民國日期格式）
     * - ProDate        ：立案日期（轉換為民國日期格式）
     * <p>
     * 資料來源：
     * - ProRec (案件主表)
     * - ACABrd (分會對應表，透過 ID 與 ProRec.ID 關聯)
     * - Lists (代碼表，ParentID=26，對應分會名稱)
     *
     * @param proRecId 個案紀錄 ID
     * @return Header DTO；若查無資料則回傳 null
     */
    @Override
    public Aca3001QueryDto.Header computeHeader(String proRecId) {
        final String SQL_HEADER =
                "SELECT l.[Text] AS BranchName, r.ProNoticeDate, r.ProDate " +
                        "FROM dbo.ProRec r " +
                        "LEFT JOIN dbo.Lists l ON l.ParentID = 26 AND l.Value = r.CreatedByBranchID " +
                        "WHERE r.ID = ?";

        return jdbcTemplate.query(SQL_HEADER, ps -> ps.setString(1, proRecId), rs -> {
            // 1) 若查無資料 → 回傳 null
            if (!rs.next()) return null;
            // 2) 組裝 Header DTO
            var header = new Aca3001QueryDto.Header();
            header.setBranchName(rs.getString("BranchName"));
            header.setProNoticeDate(getLocalDateToROC(rs, "ProNoticeDate"));
            header.setProDate(getLocalDateToROC(rs, "ProDate"));
            return header;
        });
    }

    /**
     * 查詢並組裝案件的 Profile 資訊。
     * <p>
     * Profile 包含：
     * - acaName   ：個案姓名
     * - acaIdNo   ：身分證字號
     * - acaCardNo ：保護卡號
     * <p>
     * 資料來源：
     * - ProRec (案件主表) → 提供 proRecId 與 ACACardNo
     * - ACABrd (個案基本資料表) → 透過 ACACardNo 與 ProRec.ACACardNo 連結
     *
     * @param proRecId 個案紀錄 ID
     * @return Profile DTO；若查無資料則回傳 null
     */
    @Override
    public Aca3001QueryDto.Profile computeProfile(String proRecId) {
        final String SQL_PROFILE =
                "SELECT b.ACAName, b.ACAIDNo, b.ACACardNo " +
                        "FROM dbo.ProRec r " +
                        "JOIN dbo.ACABrd b ON b.ACACardNo = r.ACACardNo " +
                        "WHERE r.ID = ?";

        return jdbcTemplate.query(SQL_PROFILE, ps -> ps.setString(1, proRecId), rs -> {
            // 1) 查無資料 → 回傳 null
            if (!rs.next()) return null;
            // 2) 組裝 Profile DTO
            var p = new Aca3001QueryDto.Profile();
            p.setAcaName(rs.getString("ACAName"));
            p.setAcaIdNo(rs.getString("ACAIDNo"));
            p.setAcaCardNo(rs.getString("ACACardNo"));
            return p;
        });
    }

    /**
     * 組裝「直接認輔條件 (DirectAdoptCriteria)」區塊。
     *
     * <p>輸出內容：
     * <ul>
     *   <li><b>options</b>：目前可供選擇的條件（來源 Lists，ListName='PROADOPT_DAC'，僅取 IsDisabled=0）。</li>
     *   <li><b>records</b>：此 ProAdopt 的「歷史＋現行」勾選狀態：
     *     <ul>
     *       <li>歷史：DirectAdoptCriteria 中曾出現過的項目（即使該 Lists 項目已被停用亦保留）。</li>
     *       <li>現行補齊：Lists 中現行有效但未入庫的項目 → 以未勾選補齊（isSelected=false、text=Lists.Text）。</li>
     *     </ul>
     *   </li>
     *   <li><b>hasDiff</b>：用於提示是否存在「records 與現行 options 的差異」（例如 records 含已停用項）。</li>
     * </ul>
     *
     * <p>情境規則：
     * <ul>
     *   <li>proAdoptId == null：尚未建立紀錄 → records 依現行 options 產生全未勾選清單；hasDiff 依 compare 邏輯判定。</li>
     *   <li>proAdoptId != null：records 以「歷史 ∪ 現行補齊」一次查出。</li>
     * </ul>
     *
     * @param proAdoptId ProAdopt 主鍵 ID（可為 null，代表尚未建立）
     * @return DirectAdoptCriteria DTO（含 options、records、hasDiff）
     */
    @Override
    public Aca3001QueryDto.DirectAdoptCriteria computeDirectAdoptCriteria(Integer proAdoptId) {
        final String SQL_OPTIONS =
                "SELECT EntryID, Value, [Text], SortOrder FROM dbo.Lists " +
                        "WHERE ListName = 'PROADOPT_DAC' AND IsDisabled = 0 " +
                        "ORDER BY SortOrder ASC, EntryID ASC";
        // -- 取出此 ProAdopt 的歷史紀錄（不補齊 Lists 現行未入庫項目）
        //-- 顯示文字優先用歷史快照 EntryText；若當時沒存快照，退回 Lists.Text
        //-- DisabledRank：若該 entry 在 Lists 找不到或已停用 → 1，否則 0（讓有效的排前面）
        final String SQL_RECORDS_UNION =
                "SELECT " +
                        "    c.ListsEntryID                     AS EntryID, " +
                        "    COALESCE(c.EntryText, l.[Text])    AS RecordText, " +
                        "    COALESCE(c.IsSelected, CAST(0 AS bit)) AS IsSelected, " +
                        "    COALESCE(l.SortOrder, 2147483647)  AS SortOrder, " +
                        "    CASE WHEN l.EntryID IS NOT NULL AND l.IsDisabled = 0 THEN 0 ELSE 1 END AS DisabledRank " +
                        "FROM dbo.DirectAdoptCriteria c " +
                        "LEFT JOIN dbo.Lists l " +
                        "       ON l.EntryID = c.ListsEntryID " +
                        "      AND l.ListName = 'PROADOPT_DAC' " +
                        "WHERE c.ProAdoptID = ? " +
                        "ORDER BY DisabledRank ASC, SortOrder ASC, EntryID ASC";

        // 1) 建立 DTO
        var dto = new Aca3001QueryDto.DirectAdoptCriteria();

        // 2) options：所有可選條件（固定從 Lists 撈，僅取 IsDisabled=0）
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

        // 3) records：歷史
        List<Aca3001QueryDto.DirectAdoptCriteria.Record> listRecord;
        if (proAdoptId == null) {
            listRecord = List.of();
            dto.setRecords(listRecord);
        } else {
            listRecord = jdbcTemplate.query(
                    SQL_RECORDS_UNION,
                    ps -> {
                        ps.setInt(1, proAdoptId);
                    },
                    (rs, i) -> {
                        var r = new Aca3001QueryDto.DirectAdoptCriteria.Record();
                        r.setEntryId(rs.getInt("EntryID"));
                        r.setText(rs.getString("RecordText"));     // 歷史快照優先，否則用 Lists.Text
                        r.setSelected(rs.getBoolean("IsSelected")); // 無紀錄時預設 0
                        return r;
                    }
            );
        }
        dto.setRecords(listRecord);
        dto.setHasDiff(computeHasDiffDirect(listOptions, listRecord));

        return dto;
    }

    /**
     * 查詢並組裝「評估認輔條件 (EvalAdoptCriteria)」區塊。
     * <p>
     * 區塊包含：
     * - options    ：現行有效選項（Lists，ListName='PROADOPT_EAC'，IsDisabled=0）
     * - records    ：歷史＋現行（補齊）：歷史保留、現行未入庫補為未勾選，顯示文字優先用歷史快照
     * - evalScores ：分數與評語（ProAdopt）
     * - hasDiff    ：records 與現行 options 是否存在差異
     */
    @Override
    public Aca3001QueryDto.EvalAdoptCriteria computeEvalAdoptCriteria(Integer proAdoptId) {

        final String SQL_OPTIONS =
                "SELECT EntryID, Value, [Text], SortOrder FROM dbo.Lists " +
                        "WHERE ListName = 'PROADOPT_EAC' AND IsDisabled = 0 " +
                        "ORDER BY SortOrder ASC, EntryID ASC";
        //-- 取出此 ProAdopt 的歷史紀錄（不補齊 Lists 現行未入庫項目）
        //-- 顯示文字優先用歷史快照 EntryText；若當時沒存快照，退回 Lists.Text
        //-- DisabledRank：若該 entry 在 Lists 找不到或已停用 → 1，否則 0（讓有效的排前面）
        final String SQL_RECORDS_UNION =
                "SELECT " +
                        "    c.ListsEntryID                     AS EntryID, " +
                        "    COALESCE(c.EntryText, l.[Text])    AS RecordText, " +
                        "    COALESCE(c.IsSelected, CAST(0 AS bit)) AS IsSelected, " +
                        "    COALESCE(l.SortOrder, 2147483647)  AS SortOrder, " +
                        "    CASE WHEN l.EntryID IS NOT NULL AND l.IsDisabled = 0 THEN 0 ELSE 1 END AS DisabledRank " +
                        "FROM dbo.EvalAdoptCriteria c " +
                        "LEFT JOIN dbo.Lists l " +
                        "       ON l.EntryID = c.ListsEntryID " +
                        "      AND l.ListName = 'PROADOPT_EAC' " +
                        "WHERE c.ProAdoptID = ? " +
                        "ORDER BY DisabledRank ASC, SortOrder ASC, EntryID ASC";
        final String SQL_SCORES =
                "SELECT ScoreEconomy, ScoreEmployment, ScoreFamily, " +
                        "   ScoreSocial, ScorePhysical, ScorePsych, " +
                        "   ScoreParenting, ScoreLegal, ScoreResidence, " +
                        "   ScoreTotal, Comment " +
                        "FROM dbo.ProAdopt WHERE ID = ?";

        // 1) 建立 DTO
        var dto = new Aca3001QueryDto.EvalAdoptCriteria();

        // 2) options：一律載入 Lists 的有效選項
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

        // 3) selected + scores：依 proAdoptId 是否存在決定
        if (proAdoptId == null) {
            dto.setRecords(List.of());
            dto.setEvalScores(new Aca3001QueryDto.EvalAdoptCriteria.EvalScore());
        } else {
            // 3-1) records：歷史
            List<Aca3001QueryDto.EvalAdoptCriteria.Record> records =
                    jdbcTemplate.query(
                            SQL_RECORDS_UNION,
                            ps -> {
                                ps.setInt(1, proAdoptId);
                            },
                            (rs, i) -> {
                                var r = new Aca3001QueryDto.EvalAdoptCriteria.Record();
                                r.setEntryId(rs.getInt("EntryID"));
                                r.setText(rs.getString("RecordText"));     // 歷史快照優先，否則 Lists.Text
                                r.setSelected(rs.getBoolean("IsSelected"));// 無歷史則 0（未勾選）
                                return r;
                            }
                    );
            dto.setRecords(records);

            // 3-2) scores：從 ProAdopt 載入分數與評語
            Aca3001QueryDto.EvalAdoptCriteria.EvalScore scores =
                    jdbcTemplate.query(SQL_SCORES, rs -> {
                        if (rs.next()) {
                            var sc = new Aca3001QueryDto.EvalAdoptCriteria.EvalScore();
                            sc.setScoreEconomy(getNullableInt(rs, "ScoreEconomy"));
                            sc.setScoreEmployment(getNullableInt(rs, "ScoreEmployment"));
                            sc.setScoreFamily(getNullableInt(rs, "ScoreFamily"));
                            sc.setScoreSocial(getNullableInt(rs, "ScoreSocial"));
                            sc.setScorePhysical(getNullableInt(rs, "ScorePhysical"));
                            sc.setScorePsych(getNullableInt(rs, "ScorePsych"));
                            sc.setScoreParenting(getNullableInt(rs, "ScoreParenting"));
                            sc.setScoreLegal(getNullableInt(rs, "ScoreLegal"));
                            sc.setScoreResidence(getNullableInt(rs, "ScoreResidence"));
                            sc.setTotalScore(getNullableInt(rs, "ScoreTotal"));
                            sc.setComment(rs.getString("Comment"));
                            return sc;
                        }
                        return new Aca3001QueryDto.EvalAdoptCriteria.EvalScore();
                    }, proAdoptId);
            dto.setEvalScores(scores);
        }
        // 4) 差異檢查（若需要）
        dto.setHasDiff(computeHasDiffEval(listOptions, dto.getRecords()));

        return dto;
    }

    /**
     * 查詢並組裝案件的 Summary 區塊。
     * <p>
     * Summary 包含：
     * 1. serviceTypeSelected：服務類型選擇 (從 ACA_PROTECT 階層代碼表遞迴撈路徑)
     * 2. proEmploymentStatus / proStatus：來自 ProRec
     * 3. caseStatus：案件處理狀態 (REJECT / ACCEPT / END / NONE) 及理由，來自 ProAdopt
     * <p>
     * 規則：
     * - serviceTypeSelected：透過 ProDtl.ProItem → 解析個案勾選的葉子節點 → 遞迴回溯父節點，直到根節點 (Parent=37)
     * - proEmploymentStatus / proStatus：若 ProRec 存在則取值，否則為 null
     * - caseStatus：若 proAdoptId 為 null 或查無資料 → 回傳預設 CaseStatus
     *
     * @param proRecId   ProRec 主鍵 ID
     * @param proAdoptId ProAdopt 主鍵 ID (可為 null)
     * @return Summary DTO
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
                        "    AND c.ParentID <> 37 " + // 遇到根節點 (Parent=37) 停止
                        ") " +
                        "SELECT LeafEntryID, EntryID, [Text], [Level], IsDisabled, IsDeleted " +
                        "FROM cte " +
                        "ORDER BY LeafEntryID, [Level] ASC " +  //先依葉子 ID，再依層級排序
                        "OPTION (MAXRECURSION 32);"; //限制遞迴最多 32 層，避免無限迴圈
        final String SQL_EMPLOYMENTSTATUS_AND_PROSTATUS =
                "SELECT l.[Text] AS ProEmploymentStatusText, " +
                        "       r.ProStatus AS ProStatus " +
                        "FROM dbo.ProRec r " +
                        "LEFT JOIN dbo.Lists l " +
                        "  ON l.ListName = 'ACA_EMPLOYMENT_STATUS' " +
                        " AND l.Value = r.Pro_EmploymentStaus " +
                        "WHERE r.ID = ?";
        final String SQL_CASESTATUS =
                "SELECT CaseReject, " +
                        "ReasonReject, " +
                        "CaseAccept, " +
                        "ReasonAccept, " +
                        "CaseEnd, " +
                        "ReasonEnd " +
                        "FROM dbo.ProAdopt " +
                        "WHERE ID = ?";
        // 1) 建立 Summary DTO
        var summary = new Aca3001QueryDto.Summary();

        // 2) 服務類型選擇 (serviceTypeSelected)
        // - 對每個 LeafEntryId，回溯完整路徑直到根節點
        // - 收集「是否禁用/刪除」旗標，供前端顯示
        List<Aca3001QueryDto.Summary.ServiceTypeSelected> listService = jdbcTemplate.query(SQL_SERVICE, rs -> {

            // 以 LeafEntryID 分組，一個 Leaf 對應一條完整路徑
            Map<Integer, Aca3001QueryDto.Summary.ServiceTypeSelected> byLeaf = new LinkedHashMap<>();

            // 每一列 = 路徑上的一個節點（父/祖先...直到葉），SQL 已依 LeafEntryID, Level 升冪排序
            while (rs.next()) {
                int leafId = rs.getInt("LeafEntryID"); // 此列屬於哪個葉節點
                int entryId = rs.getInt("EntryID"); // 路徑中的某個節點 ID（不一定是葉節點）
                if (entryId == 37) continue; // 略過根節點（保護類別），不納入路徑

                String text = rs.getString("Text");
                boolean dis = rs.getBoolean("IsDisabled");
                boolean del = rs.getBoolean("IsDeleted");

                // 初始化 leaf 容器
                var leafDto = byLeaf.computeIfAbsent(leafId, k -> {
                    var x = new Aca3001QueryDto.Summary.ServiceTypeSelected();
                    x.setLeafEntryId(k);
                    x.setPathEntryIds(new ArrayList<>());
                    x.setPathText(new ArrayList<>());
                    x.setHistoricalEntryIds(new ArrayList<>());
                    x.setHasDisabled(false);
                    return x;
                });

                // 加入路徑節點
                leafDto.getPathEntryIds().add(entryId);
                leafDto.getPathText().add(text);

                // 設定狀態旗標
                if (dis) leafDto.setHasDisabled(true);
                if (del) leafDto.setHasDeleted(true);

                // 收集歷史節點（以 IsDisabled 為準）
                if (dis) leafDto.getHistoricalEntryIds().add(entryId);
            }

            // 將分組結果由 Map 轉為 List；若無資料則回傳空清單（避免回傳 null）
            return byLeaf.isEmpty()
                    ? List.of()
                    : new ArrayList<>(byLeaf.values());
        }, proRecId);

        summary.setServiceTypeSelected(listService);

        // 3) ProRec 的 ProEmploymentStatus / ProStatus
        jdbcTemplate.query(SQL_EMPLOYMENTSTATUS_AND_PROSTATUS, rs -> {
            if (rs.next()) {
                summary.setProEmploymentStatus(rs.getString("ProEmploymentStatusText"));
                summary.setProStatus(rs.getString("ProStatus"));
            }
            return null;
        }, proRecId);

        // 4) ProAdopt 的 CaseStatus
        // - 若 proAdoptId == null → 尚未建立 → 預設 CaseStatus
        // - 否則撈取 DB 欄位判斷狀態與理由
        if (proAdoptId == null) {
            summary.setCaseStatus(new Aca3001QueryDto.Summary.CaseStatus());
        } else {
            Aca3001QueryDto.Summary.CaseStatus caseStatus = jdbcTemplate.query(SQL_CASESTATUS, rs -> {

                if (!rs.next()) {
                    return new Aca3001QueryDto.Summary.CaseStatus(); // 查無 → 預設值
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


    //Save API --------------------------------------------------------------------------------

    /**
     * 新增一筆 ProAdopt 主表資料，並回傳新紀錄的 ID。
     *
     * @param proRecId        個案紀錄 ID（FK → ProRec.ID）
     * @param scores          九項評估分數 + 評語
     * @param caseReject      狀態：是否「拒絕」
     * @param reasonReject    拒絕理由（允許空字串，不為 null）
     * @param caseAccept      狀態：是否「接受」
     * @param reasonAccept    接受理由
     * @param caseEnd         狀態：是否「結案」
     * @param reasonEnd       結案理由
     * @param createdByUserId 建立者使用者 ID
     * @return 新增紀錄的自動流水號 ID
     */
    @Override
    public Integer insertProAdopt(String proRecId, Aca3001SavePayload.Scores scores, boolean caseReject, String reasonReject, boolean caseAccept, String reasonAccept, boolean caseEnd, String reasonEnd, Integer createdByUserId) {
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
        // 執行插入並回傳新 ID
        // - queryForObject(sql, Integer.class, params...)
        //   → 執行後會直接取 OUTPUT INSERTED.ID 的值並轉為 Integer
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

    /**
     * 更新指定的 ProAdopt 主表資料。
     *
     * @param proAdoptId       ProAdopt 主鍵 ID
     * @param scores           九項評估分數 + 評語
     * @param caseReject       狀態：是否「拒絕」
     * @param reasonReject     拒絕理由（允許空字串，不為 null）
     * @param caseAccept       狀態：是否「接受」
     * @param reasonAccept     接受理由
     * @param caseEnd          狀態：是否「結案」
     * @param reasonEnd        結案理由
     * @param modifiedByUserId 修改者使用者 ID
     */
    @Override
    public void updateProAdopt(Integer proAdoptId, Aca3001SavePayload.Scores scores, boolean caseReject, String reasonReject, boolean caseAccept, String reasonAccept, boolean caseEnd, String reasonEnd, Integer modifiedByUserId) {
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
        // 執行更新
        // - 使用 jdbcTemplate.update()
        // - 傳入分數、評論、狀態與理由、修改者 ID 與目標主鍵 ID
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

    @Transactional
    @Override
    public void upsertDirectAdoptCriteria(int proAdoptId, List<Integer> selectedEntryIds, boolean refreshSnapshot, boolean isNew) {
        // ====== A) 更版模式：刪除舊關聯 → 依現行 Lists 重建 ======
        if (refreshSnapshot) {
            // 0) 全刪
            jdbcTemplate.update("DELETE FROM dbo.DirectAdoptCriteria WHERE ProAdoptID = ?", proAdoptId);

            // 1) 先插入「本次勾選」(IsSelected=1)，只納入現行有效 Lists（IsDisabled=0）
            if (selectedEntryIds != null && !selectedEntryIds.isEmpty()) {
                jdbcTemplate.batchUpdate(
                        "INSERT INTO dbo.DirectAdoptCriteria (ProAdoptID, ListsEntryID, EntryText, IsSelected) " +
                                "SELECT ?, l.EntryID, l.[Text], 1 " +
                                "FROM dbo.Lists l " +
                                "WHERE l.ListName = 'PROADOPT_DAC' AND l.IsDisabled = 0 AND l.EntryID = ?",
                        selectedEntryIds,
                        selectedEntryIds.size(),
                        (ps, entryId) -> {
                            ps.setInt(1, proAdoptId);
                            ps.setInt(2, entryId);
                        }
                );
            }

            // 2) 再補齊「現行有效但尚未入庫」的選項（IsSelected=0）
            jdbcTemplate.update(
                    "INSERT INTO dbo.DirectAdoptCriteria (ProAdoptID, ListsEntryID, EntryText, IsSelected) " +
                            "SELECT ?, l.EntryID, l.[Text], 0 " +
                            "FROM dbo.Lists l " +
                            "LEFT JOIN dbo.DirectAdoptCriteria c " +
                            "  ON c.ProAdoptID = ? AND c.ListsEntryID = l.EntryID " +
                            "WHERE l.ListName = 'PROADOPT_DAC' AND l.IsDisabled = 0 " +
                            "  AND c.ProAdoptID IS NULL",
                    ps -> {
                        ps.setInt(1, proAdoptId);
                        ps.setInt(2, proAdoptId);
                    }
            );

            return; // 更版完成，結束
        }

        // ====== B) 一般模式（非更版）：保留歷史，只有新增才補齊 ======
        // 1) 歸零選取狀態
        jdbcTemplate.update(
                "UPDATE dbo.DirectAdoptCriteria SET IsSelected = 0 WHERE ProAdoptID = ?",
                proAdoptId
        );

        // 2) 勾選當次選項；不存在則 INSERT（帶快照）
        if (selectedEntryIds != null && !selectedEntryIds.isEmpty()) {
            for (Integer entryId : selectedEntryIds) {
                final String sqlUpdate =
                        "UPDATE dbo.DirectAdoptCriteria SET IsSelected = 1 WHERE ProAdoptID = ? AND ListsEntryID = ?";

                int updated = jdbcTemplate.update(sqlUpdate, ps -> {
                    ps.setInt(1, proAdoptId);
                    ps.setInt(2, entryId);
                });

                if (updated == 0) {
                    jdbcTemplate.update(
                            "INSERT INTO dbo.DirectAdoptCriteria (ProAdoptID, ListsEntryID, EntryText, IsSelected) " +
                                    "SELECT ?, ?, l.[Text], 1 FROM dbo.Lists l " +
                                    "WHERE l.EntryID = ? AND l.ListName = 'PROADOPT_DAC' AND l.IsDisabled = 0",
                            ps -> {
                                ps.setInt(1, proAdoptId);
                                ps.setInt(2, entryId);
                                ps.setInt(3, entryId);
                            }
                    );
                }
            }
        }

        // 3) 只有「新增」才補齊未勾選（IsSelected=0）
        if (isNew) {
            jdbcTemplate.update(
                    "INSERT INTO dbo.DirectAdoptCriteria (ProAdoptID, ListsEntryID, EntryText, IsSelected) " +
                            "SELECT ?, l.EntryID, l.[Text], 0 " +
                            "FROM dbo.Lists l " +
                            "LEFT JOIN dbo.DirectAdoptCriteria c " +
                            "  ON c.ProAdoptID = ? AND c.ListsEntryID = l.EntryID " +
                            "WHERE l.ListName = 'PROADOPT_DAC' AND l.IsDisabled = 0 " +
                            "  AND c.ProAdoptID IS NULL",
                    ps -> {
                        ps.setInt(1, proAdoptId);
                        ps.setInt(2, proAdoptId);
                    }
            );
        }
//        // 1) 全設為未勾選（保留歷史）
//        jdbcTemplate.update(
//                "UPDATE dbo.DirectAdoptCriteria SET IsSelected = 0 WHERE ProAdoptID = ?",
//                proAdoptId
//        );
//
//        // 2) 逐一把「本次勾選」設為 1；若不存在就插入（帶 Lists.Text 快照）
//        if (selectedEntryIds != null && !selectedEntryIds.isEmpty()) {
//            for (Integer entryId : selectedEntryIds) {
//                // 根據 refreshSnapshot 切換 UPDATE SQL
//                final String sqlUpdate = refreshSnapshot
//                        ? // 刷新快照：覆寫 EntryText = Lists.Text
//                        "UPDATE c SET c.IsSelected = 1, c.EntryText = l.[Text] " +
//                                "FROM dbo.DirectAdoptCriteria c " +
//                                "JOIN dbo.Lists l ON l.EntryID = c.ListsEntryID AND l.ListName = 'PROADOPT_DAC' " +
//                                "WHERE c.ProAdoptID = ? AND c.ListsEntryID = ?"
//                        : // 保留快照：不動 EntryText
//                        "UPDATE dbo.DirectAdoptCriteria " +
//                                "SET IsSelected = 1 " +
//                                "WHERE ProAdoptID = ? AND ListsEntryID = ?";
//
//                int updated = jdbcTemplate.update(sqlUpdate, ps -> {
//                    ps.setInt(1, proAdoptId);
//                    ps.setInt(2, entryId);
//                });
//
//                //如果過去從沒存過這個 entryId → UPDATE 找不到任何列可更新 → update==0 → 就會進入 INSERT
//                if (updated == 0) {
//                    jdbcTemplate.update(
//                            "INSERT INTO dbo.DirectAdoptCriteria (ProAdoptID, ListsEntryID, EntryText, IsSelected) " +
//                                    "SELECT ?, ?, l.[Text], 1 " +
//                                    "FROM dbo.Lists l " +
//                                    "WHERE l.EntryID = ? AND l.ListName = 'PROADOPT_DAC' " +
//                                    "  AND l.IsDisabled = 0",               // <── 新增這條件
//                            ps -> {
//                                ps.setInt(1, proAdoptId);
//                                ps.setInt(2, entryId);
//                                ps.setInt(3, entryId);
//                            }
//                    );
//                }
//            }
//        }
//        // 3) 補齊：只有「新增」才做；更新一律不補
//        // 注意：這段只針對「現行有效」的 options（IsDisabled=0），不會新增已停用的項目
//        if (isNew) {
//            jdbcTemplate.update(
//                    "INSERT INTO dbo.DirectAdoptCriteria (ProAdoptID, ListsEntryID, EntryText, IsSelected) " +
//                            "SELECT ?, l.EntryID, l.[Text], 0 " +
//                            "FROM dbo.Lists l " +
//                            "LEFT JOIN dbo.DirectAdoptCriteria c " +
//                            "  ON c.ProAdoptID = ? AND c.ListsEntryID = l.EntryID " +
//                            "WHERE l.ListName = 'PROADOPT_DAC' AND l.IsDisabled = 0 " +
//                            "  AND c.ProAdoptID IS NULL",
//                    ps -> {
//                        ps.setInt(1, proAdoptId);
//                        ps.setInt(2, proAdoptId);
//                    }
//            );
//        }

    }

    @Transactional
    @Override
    public void upsertEvalAdoptCriteria(int proAdoptId, List<Integer> selectedEntryIds, boolean refreshSnapshot, boolean isNew) {
        // ====== A) 更版模式：刪除舊關聯 → 依現行 Lists 重建 ======
        if (refreshSnapshot) {
            jdbcTemplate.update("DELETE FROM dbo.EvalAdoptCriteria WHERE ProAdoptID = ?", proAdoptId);

            if (selectedEntryIds != null && !selectedEntryIds.isEmpty()) {
                jdbcTemplate.batchUpdate(
                        "INSERT INTO dbo.EvalAdoptCriteria (ProAdoptID, ListsEntryID, EntryText, IsSelected) " +
                                "SELECT ?, l.EntryID, l.[Text], 1 " +
                                "FROM dbo.Lists l " +
                                "WHERE l.ListName = 'PROADOPT_EAC' AND l.IsDisabled = 0 AND l.EntryID = ?",
                        selectedEntryIds,
                        selectedEntryIds.size(),
                        (ps, entryId) -> {
                            ps.setInt(1, proAdoptId);
                            ps.setInt(2, entryId);
                        }
                );
            }

            jdbcTemplate.update(
                    "INSERT INTO dbo.EvalAdoptCriteria (ProAdoptID, ListsEntryID, EntryText, IsSelected) " +
                            "SELECT ?, l.EntryID, l.[Text], 0 " +
                            "FROM dbo.Lists l " +
                            "LEFT JOIN dbo.EvalAdoptCriteria c " +
                            "  ON c.ProAdoptID = ? AND c.ListsEntryID = l.EntryID " +
                            "WHERE l.ListName = 'PROADOPT_EAC' AND l.IsDisabled = 0 " +
                            "  AND c.ProAdoptID IS NULL",
                    ps -> {
                        ps.setInt(1, proAdoptId);
                        ps.setInt(2, proAdoptId);
                    }
            );

            return; // 更版完成
        }

        // ====== B) 一般模式（非更版） ======
        jdbcTemplate.update(
                "UPDATE dbo.EvalAdoptCriteria SET IsSelected = 0 WHERE ProAdoptID = ?",
                proAdoptId
        );

        if (selectedEntryIds != null && !selectedEntryIds.isEmpty()) {
            for (Integer entryId : selectedEntryIds) {
                final String sqlUpdate =
                        "UPDATE dbo.EvalAdoptCriteria SET IsSelected = 1 WHERE ProAdoptID = ? AND ListsEntryID = ?";

                int updated = jdbcTemplate.update(sqlUpdate, ps -> {
                    ps.setInt(1, proAdoptId);
                    ps.setInt(2, entryId);
                });

                if (updated == 0) {
                    jdbcTemplate.update(
                            "INSERT INTO dbo.EvalAdoptCriteria (ProAdoptID, ListsEntryID, EntryText, IsSelected) " +
                                    "SELECT ?, ?, l.[Text], 1 FROM dbo.Lists l " +
                                    "WHERE l.EntryID = ? AND l.ListName = 'PROADOPT_EAC' AND l.IsDisabled = 0",
                            ps -> {
                                ps.setInt(1, proAdoptId);
                                ps.setInt(2, entryId);
                                ps.setInt(3, entryId);
                            }
                    );
                }
            }
        }

        if (isNew) {
            jdbcTemplate.update(
                    "INSERT INTO dbo.EvalAdoptCriteria (ProAdoptID, ListsEntryID, EntryText, IsSelected) " +
                            "SELECT ?, l.EntryID, l.[Text], 0 " +
                            "FROM dbo.Lists l " +
                            "LEFT JOIN dbo.EvalAdoptCriteria c " +
                            "  ON c.ProAdoptID = ? AND c.ListsEntryID = l.EntryID " +
                            "WHERE l.ListName = 'PROADOPT_EAC' AND l.IsDisabled = 0 " +
                            "  AND c.ProAdoptID IS NULL",
                    ps -> {
                        ps.setInt(1, proAdoptId);
                        ps.setInt(2, proAdoptId);
                    }
            );
        }
//        jdbcTemplate.update(
//                "UPDATE dbo.EvalAdoptCriteria SET IsSelected = 0 WHERE ProAdoptID = ?",
//                proAdoptId
//        );
//
//        if (selectedEntryIds != null && !selectedEntryIds.isEmpty()) {
//            for (Integer entryId : selectedEntryIds) {
//                final String sqlUpdate = refreshSnapshot
//                        ? "UPDATE c SET c.IsSelected = 1, c.EntryText = l.[Text] " +
//                        "FROM dbo.EvalAdoptCriteria c " +
//                        "JOIN dbo.Lists l ON l.EntryID = c.ListsEntryID AND l.ListName = 'PROADOPT_EAC' " +
//                        "WHERE c.ProAdoptID = ? AND c.ListsEntryID = ?"
//                        : "UPDATE dbo.EvalAdoptCriteria " +
//                        "SET IsSelected = 1 " +
//                        "WHERE ProAdoptID = ? AND ListsEntryID = ?";
//
//                int updated = jdbcTemplate.update(sqlUpdate, ps -> {
//                    ps.setInt(1, proAdoptId);
//                    ps.setInt(2, entryId);
//                });
//
//                if (updated == 0) {
//                    jdbcTemplate.update(
//                            "INSERT INTO dbo.EvalAdoptCriteria (ProAdoptID, ListsEntryID, EntryText, IsSelected) " +
//                                    "SELECT ?, ?, l.[Text], 1 " +
//                                    "FROM dbo.Lists l " +
//                                    "WHERE l.EntryID = ? AND l.ListName = 'PROADOPT_EAC' " +
//                                    "  AND l.IsDisabled = 0",               // <── 新增這條件
//                            ps -> {
//                                ps.setInt(1, proAdoptId);
//                                ps.setInt(2, entryId);
//                                ps.setInt(3, entryId);
//                            }
//                    );
//                }
//            }
//        }
//        // 3) 補齊：只有「新增」才做；更新一律不補
//        // 注意：這段只針對「現行有效」的 options（IsDisabled=0），不會新增已停用的項目
//        if (isNew) {
//            jdbcTemplate.update(
//                    "INSERT INTO dbo.EvalAdoptCriteria (ProAdoptID, ListsEntryID, EntryText, IsSelected) " +
//                            "SELECT ?, l.EntryID, l.[Text], 0 " +
//                            "FROM dbo.Lists l " +
//                            "LEFT JOIN dbo.EvalAdoptCriteria c " +
//                            "  ON c.ProAdoptID = ? AND c.ListsEntryID = l.EntryID " +
//                            "WHERE l.ListName = 'PROADOPT_EAC' AND l.IsDisabled = 0 " +
//                            "  AND c.ProAdoptID IS NULL",
//                    ps -> {
//                        ps.setInt(1, proAdoptId);
//                        ps.setInt(2, proAdoptId);
//                    }
//            );
//        }
    }

    // Delete API --------------------------------------------------------------------------------

    /**
     * 刪除指定的 ProAdopt 主表紀錄（含其相關子表資料）
     * <p>
     * 注意：
     * - 命名為 "Cascade" 表示需同時刪除關聯子表
     * - 建議在執行此方法前，確保該紀錄可刪除（由 Service 層做時間鎖等檢查）
     * - 若 DB 已設定外鍵 ON DELETE CASCADE，此方法只需刪主表即可
     * - 若 DB 無 Cascade，則應在此處先刪子表，再刪主表
     *
     * @param proAdoptId ProAdopt 主鍵 ID
     */
    @Override
    public void deleteProAdoptCascade(Integer proAdoptId) {
        // 1) 刪除主表自動刪除子表（DB 有設定 ON DELETE CASCADE）
        jdbcTemplate.update("DELETE FROM dbo.ProAdopt WHERE ID = ?", proAdoptId);
    }

    // ---------- 私有方法區 ----------

    /**
     * 從 ResultSet 讀取指定欄位的 Timestamp，並轉換為民國日期格式 (ROC)。
     * <p>
     * 規則：
     * - 若資料庫欄位值為 null → 回傳 null
     * - 若非 null → 轉成 java.util.Date，再呼叫 DateUtil.date2Roc() 格式化
     *
     * @param rs          查詢結果集
     * @param columnLabel 欄位名稱
     * @return 以「yyy年M月d日」格式表示的民國日期字串；若欄位為 null 則回傳 null
     * @throws SQLException 若 ResultSet 操作發生錯誤
     */
    private static String getLocalDateToROC(ResultSet rs, String columnLabel) throws SQLException {
        // 1) 讀取 Timestamp 欄位
        Timestamp ts = rs.getTimestamp(columnLabel); // 讀出該欄位的 Timestamp
        // 2) 若為 null → 直接回傳 null
        if (ts == null) {
            return null;
        }
        // 3) 轉換為 java.util.Date，並以民國年格式化
        return DateUtil.date2Roc(
                new java.util.Date(ts.getTime()),
                DateUtil.DateFormat.yyy年M月d日
        );
    }

    /**
     * 若字串為 null → 回傳 null；否則回傳去除前後空白後的字串。
     *
     * @param s 原始字串
     * @return null 或去除前後空白的字串
     */
    private static String nullOrTrim(String s) {
        return (s == null) ? null : s.trim();
    }

    /**
     * 載入系統設定的「時間鎖定日」(TIMELOCK_ACABRD)。
     * <p>
     * 資料來源：
     * - Lists 表中，ListName = 'TIMELOCK_ACABRD'，取第一筆 Value
     * <p>
     * 規則：
     * - 若 Value 為 null 或空白 → 回傳 null
     * - 若可解析成 yyyy/M/d 格式 → 轉為 LocalDate
     * - 若解析失敗 → 回傳 null (建議 log.warn)
     *
     * @return LocalDate 鎖定日；若不存在或格式錯誤則回傳 null
     */
    public LocalDate loadTimeLockDate() {
        final String SQL_TIMELOCK = "SELECT TOP 1 Value FROM Lists WHERE ListName = 'TIMELOCK_ACABRD'";

        // 1) 查詢設定值 (可能為 null)
        String timeLockStr = jdbcTemplate.query(SQL_TIMELOCK, rs -> rs.next() ? rs.getString(1) : null);

        // 2) 若空白/無設定 → 回傳 null
        if (timeLockStr == null || timeLockStr.isBlank()) {
            return null;
        }
        // 3) 嘗試解析 yyyy/M/d 格式
        try {
            return LocalDate.parse(timeLockStr.trim(), DateTimeFormatter.ofPattern("yyyy/M/d"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判斷指定案件是否可編輯。
     * <p>
     * 規則：
     * - 若 timeLockDate 為 null → 可編輯
     * - 若 proDate (案件日期) 為 null → 可編輯
     * - 否則：若 proDate > timeLockDate → 可編輯；否則不可編輯
     *
     * @param proRecId     ProRec 主鍵 ID
     * @param timeLockDate 系統鎖定日（可為 null）
     * @return true = 可編輯；false = 不可編輯
     */
    public boolean isEditable(String proRecId, LocalDate timeLockDate) {
        final String SQL_PRODATE = "SELECT ProDate FROM ProRec WHERE ID = ?";

        // 1) 查詢 ProDate (可能為 null)
        LocalDate proDate = jdbcTemplate.query(SQL_PRODATE, ps -> ps.setString(1, proRecId), rs -> {
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("ProDate");
                return (ts != null) ? ts.toLocalDateTime().toLocalDate() : null;
            }
            return null;
        });

        // 2) 套用規則判斷
        return (timeLockDate == null) || (proDate == null) || proDate.isAfter(timeLockDate);
    }

    /**
     * 比對 options(現行 Lists) 與 records(快照) 是否有差異：
     * - 筆數不同 → 有差異
     * - 任一 entryId 在 records 缺少 → 有差異
     * - 同 entryId 的 text 不同（trim 後比較）→ 有差異
     */
    private boolean computeHasDiffDirect(
            List<Aca3001QueryDto.DirectAdoptCriteria.Option> options,
            List<Aca3001QueryDto.DirectAdoptCriteria.Record> records
    ) {
        Map<Integer, String> optionMap = options.stream().collect(
                Collectors.toMap(
                        Aca3001QueryDto.DirectAdoptCriteria.Option::getEntryId,
                        o -> nullOrTrim(o.getText()),
                        (a, b) -> a // 若意外有重複鍵，保留第一個
                )
        );

        Map<Integer, String> recordMap = records.stream().collect(
                Collectors.toMap(
                        Aca3001QueryDto.DirectAdoptCriteria.Record::getEntryId,
                        r -> nullOrTrim(r.getText()),
                        (a, b) -> a
                )
        );

        // 1) 筆數不同（新增/缺少）
        if (optionMap.size() != recordMap.size()) return true;

        // 2) 逐鍵比對存在性與文字
        for (Map.Entry<Integer, String> e : optionMap.entrySet()) {
            Integer id = e.getKey();
            String optText = e.getValue();
            String recText = recordMap.get(id); // null → records 缺少此 entryId
            if (recText == null) return true;
            if (!Objects.equals(optText, recText)) return true;
        }
        return false; // 完全一致
    }

    /**
     * Overloaded 版本：專供 EvalAdoptCriteria 使用
     */
    private boolean computeHasDiffEval(
            List<Aca3001QueryDto.EvalAdoptCriteria.Option> options,
            List<Aca3001QueryDto.EvalAdoptCriteria.Record> records
    ) {
        Map<Integer, String> optionMap = options.stream().collect(
                Collectors.toMap(
                        Aca3001QueryDto.EvalAdoptCriteria.Option::getEntryId,
                        o -> nullOrTrim(o.getText()),
                        (a, b) -> a
                )
        );

        Map<Integer, String> recordMap = records.stream().collect(
                Collectors.toMap(
                        Aca3001QueryDto.EvalAdoptCriteria.Record::getEntryId,
                        r -> nullOrTrim(r.getText()),
                        (a, b) -> a
                )
        );

        if (optionMap.size() != recordMap.size()) return true;

        for (Map.Entry<Integer, String> e : optionMap.entrySet()) {
            String recText = recordMap.get(e.getKey());
            if (recText == null) return true;
            if (!Objects.equals(e.getValue(), recText)) return true;
        }
        return false;
    }

    private static Integer getNullableInt(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

}


