package com.hn2.cms.service.aca3001;

import com.hn2.cms.dto.aca3001.Aca3001QueryDto;
import com.hn2.cms.payload.aca3001.Aca3001QueryPayload;
import com.hn2.cms.repository.aca3001.Aca3001Repository;
import com.hn2.cms.repository.aca3001.Aca3001RepositoryImpl;
import com.hn2.core.dto.DataDto;
import com.hn2.core.dto.ResponseInfo;
import com.hn2.core.payload.GeneralPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class Aca3001ServiceImpl implements Aca3001Service {

    private final Aca3001Repository repo;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public Aca3001ServiceImpl(Aca3001Repository repo, JdbcTemplate jdbcTemplate) {
        this.repo = repo;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public DataDto<Aca3001QueryDto> query(GeneralPayload<Aca3001QueryPayload> payload) {
        //check on controller 層已經驗證過 proRecId 不為空
        if (payload == null || payload.getData() == null ||
                payload.getData().getProRecId() == null || payload.getData().getProRecId().isBlank()) {
            return new DataDto<>(null, new ResponseInfo(0, "proRecId 不可為空"));
        }
        // 取得 proRecId
        final String proRecId = payload.getData().getProRecId();

        // 先看基本資料是否存在（沒有就視為查無資料）
        Aca3001QueryDto.Profile profile = repo.findProfileByProRecId(proRecId);
        if (profile == null) {
            return new DataDto<>(null, new ResponseInfo(0, "查無資料"));
        }

        // 取得 header（ACABrd/ProRec），這些屬於既存資料表
        Aca3001QueryDto.Header header = repo.findHeaderByProRecId(proRecId);

        // 判斷是否已有 ProAdopt（存在 → 修改；不存在 → 新建）
        Integer proAdoptId = repo.findProAdoptIdByProRecId(proRecId);

        // 組裝回應
        Aca3001QueryDto res = new Aca3001QueryDto();

        // compute Meta
        Aca3001QueryDto.Meta meta = computeMeta(proRecId, proAdoptId, jdbcTemplate);
        res.setMeta(meta);

        // Header / Profile（既有資料）
        res.setHeader(header);
        res.setProfile(profile);


        if (proAdoptId != null) {
            // 情境：載入 options + selected
            res.setDirectAdoptCriteria(repo.loadDirectCriteria(proAdoptId));
            res.setEvalAdoptCriteria(repo.loadEvalCriteria(proAdoptId));

            // Summary = basics(含 serviceTypeSelected) + ProAdopt 案件狀態
            Aca3001QueryDto.Summary summary = repo.loadSummaryBasics(proRecId);
            Aca3001QueryDto.Summary.CaseStatus cs = repo.loadCaseStatus(proAdoptId);
            if (cs == null) cs = offCaseStatus(); // 保險
            summary.setCaseStatus(cs);
            res.setSummary(summary);
        } else {
            // ===== 新建情境：初始化 ProAdopt 需要的區塊 =====
            // options 從 Lists 來，selected 空、scores 預設 0
            res.setDirectAdoptCriteria(repo.initDirectCriteriaOptions()); // options 有，selected 空
            res.setEvalAdoptCriteria(repo.initEvalCriteriaOptions());     // options 有，selected 空 + evalScore 預設
            // summary 基本欄位來自 ACABrd/ProRec/ProDtl，唯 caseStatus 初始化
            Aca3001QueryDto.Summary summaryBase =
                    java.util.Optional.ofNullable(repo.loadSummaryBasics(proRecId))
                            .orElseGet(Aca3001QueryDto.Summary::new); //對 summaryBase 做安全初始化。
            summaryBase.setCaseStatus(offCaseStatus());
            res.setSummary(summaryBase);

        }

        return new DataDto<>(res, new ResponseInfo(1, "查詢成功"));
    }

    /**
     * 用於新建情境的 CaseStatus，所有狀態都為 off。
     * 這個方法是為了在 ProAdopt 不存在時初始化 Summary 的 CaseStatus。
     */
    private static Aca3001QueryDto.Summary.CaseStatus offCaseStatus() {
        Aca3001QueryDto.Summary.StatusFlag off = new Aca3001QueryDto.Summary.StatusFlag();
        off.setFlag(false);
        off.setReason(null);

        Aca3001QueryDto.Summary.CaseStatus cs = new Aca3001QueryDto.Summary.CaseStatus();
        cs.setReject(off);
        cs.setAccept(off);
        cs.setEnd(off);
        return cs;
    }

    private Aca3001QueryDto.Meta computeMeta(String proRecId, Integer proAdoptId, JdbcTemplate jdbcTemplate) {
        final String SQL_PROREC_DATE =
                "SELECT ProDate FROM ProRec WHERE ID = ?";
        final String SQL_TIMELOCK =
                "SELECT TOP 1 Value FROM Lists WHERE ListName = 'TIMELOCK_ACABRD'";

        // 1) 安全取得 ProDate（允許為 null）
        LocalDate proDate = jdbcTemplate.query(SQL_PROREC_DATE, ps -> ps.setString(1, proRecId), rs -> {
            if (rs.next()) {
                java.sql.Timestamp ts = rs.getTimestamp("ProDate");
                return (ts != null) ? ts.toLocalDateTime().toLocalDate() : null;
            }
            return null;
        });

        // 2) 取得 timelock（可為 null/空）
        String timelockStr = jdbcTemplate.query(SQL_TIMELOCK, rs -> rs.next() ? rs.getString(1) : null);

        // 3) 嘗試解析 yyyy/M/d → LocalDate（允許不補零）
        LocalDate timelockDate = null;
        if (timelockStr != null && !timelockStr.isBlank()) {
            try {
                timelockDate = java.time.LocalDate.parse(timelockStr.trim(),
                        java.time.format.DateTimeFormatter.ofPattern("yyyy/M/d"));
            } catch (Exception ignore) {
                // 解析失敗 → 視為沒有 timelock
            }
        }

        // 4) 建立 Meta
        Aca3001QueryDto.Meta meta = new Aca3001QueryDto.Meta();
        meta.setProRecId(proRecId);
        meta.setProAdoptId(proAdoptId);

        // 規則：proDate 為 null → editable=true，lockDate 仍以 timelock 值為準（可能為 null）
        if (proDate == null) {
            meta.setEditable(true);
            meta.setLockDate(timelockDate); // 可能是 null（若沒有或解析失敗）
            return meta;
        }

        // 有 proDate：若無 timelock，就視為可編輯
        if (timelockDate == null) {
            meta.setEditable(true);
            meta.setLockDate(null);
            return meta;
        }

        // 有 timelock：proDate <= timelock → 鎖住；否則可編輯
        if (!proDate.isAfter(timelockDate)) {
            meta.setEditable(false);
            meta.setLockDate(timelockDate);
        } else {
            meta.setEditable(true);
            meta.setLockDate(null);
        }
        return meta;
    }
}
