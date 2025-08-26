package com.hn2.cms.service.aca3001;

import com.hn2.cms.dto.aca3001.Aca3001QueryDto;
import com.hn2.cms.dto.aca3001.Aca3001SaveResponse;
import com.hn2.cms.payload.aca3001.Aca3001DeletePayload;
import com.hn2.cms.payload.aca3001.Aca3001QueryPayload;
import com.hn2.cms.payload.aca3001.Aca3001SavePayload;
import com.hn2.cms.repository.aca3001.Aca3001Repository;

import com.hn2.core.dto.DataDto;
import com.hn2.core.dto.ResponseInfo;
import com.hn2.core.payload.GeneralPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class Aca3001ServiceImpl implements Aca3001Service {

    private final Aca3001Repository repo;

    @Autowired
    public Aca3001ServiceImpl(Aca3001Repository repo) {
        this.repo = repo;
    }

    /**
     * 查詢認輔評估畫面需要的所有資料。
     * <p>
     * - payload 與其中的 proRecId 必須有效，否則直接回傳錯誤。
     * - 若查無個案 Profile（代表 ProRec/ACABrd 也不存在或關聯不成立），直接回傳「查無資料」。
     * - meta/header/profile/direct/eval/summary 由 repository 各自負責查詢與組裝。
     * - proAdoptId 可能為 null（表示尚未建立認輔評估），其下游 compute* 方法需能處理此情境。
     *
     * @param payload
     * @return DataDto 包含查詢結果 DTO 及回應資訊
     */
    @Override
    @Transactional(readOnly = true)
    public DataDto<Aca3001QueryDto> query(GeneralPayload<Aca3001QueryPayload> payload) {

        // 1) 基本輸入驗證
        final String proRecId = (payload == null || payload.getData() == null) ? null : payload.getData().getProRecId();
        if (proRecId == null || proRecId.isBlank()) {
            return new DataDto<>(null, new ResponseInfo(0, "proRecId 不可為空"));
        }

        // 2) 驗證個案存在性（Profile 不存在代表 ProRec/ACABrd 關聯也不成立）
        Aca3001QueryDto.Profile profile = repo.computeProfile(proRecId);
        if (profile == null) {
            return new DataDto<>(null, new ResponseInfo(0, "查無資料"));
        }
        // 3) 取得（可能為 null 的）proAdoptId：null 表示尚未建立認輔評估
        Integer proAdoptId = repo.findProAdoptIdByProRecId(proRecId);

        // 4) 組裝 DTO（各區塊由 repository 專責方法提供）
        Aca3001QueryDto dto = new Aca3001QueryDto();
        dto.setMeta(repo.computeMeta(proRecId, proAdoptId));
        dto.setHeader(repo.computeHeader(proRecId));
        dto.setProfile(profile);
        dto.setDirectAdoptCriteria(repo.computeDirectAdoptCriteria(proAdoptId));
        dto.setEvalAdoptCriteria(repo.computeEvalAdoptCriteria(proAdoptId));
        dto.setSummary(repo.computeSummary(proRecId, proAdoptId));
        return new DataDto<>(dto, new ResponseInfo(1, "查詢成功"));
    }

    //Save API
    @Override
    @Transactional
    public DataDto<Aca3001SaveResponse> save(GeneralPayload<Aca3001SavePayload> payload) {
        // --- 0) 基本取值/檢核 ---
        final String proRecId = (payload == null || payload.getData() == null) ? null : payload.getData().getProRecId();
        if (proRecId == null || proRecId.isBlank()) {
            return new DataDto<>(null, new ResponseInfo(0, "proRecId 不可為空"));
        }

        // === 先做 isEditable 檢核 ===
        LocalDate timeLockDate = repo.loadTimeLockDate(); // 方法：查 Lists.TIMELOCK_ACABRD
        boolean editable = repo.isEditable(proRecId, timeLockDate);
        if (!editable) {
            return new DataDto<>(null, new ResponseInfo(0, "已逾鎖定日，不可編輯"));
        }

        Aca3001SavePayload p = payload.getData();

        // 狀態 ↔ reason 檢核
        // --- 0) 取值與規則（不把空字串轉成 null；僅做 trim） ---
        var st = p.getCaseStatus().getState();
        String reasonRaw = p.getCaseStatus().getReason();
        if (reasonRaw != null) reasonRaw = reasonRaw.trim(); // 可保留空字串

        boolean caseReject = (st == Aca3001SavePayload.CaseStatus.State.REJECT);
        boolean caseAccept = (st == Aca3001SavePayload.CaseStatus.State.ACCEPT);
        boolean caseEnd = (st == Aca3001SavePayload.CaseStatus.State.END);

        // 對應三個 reason：NONE → 全部 NULL；其他 → 選中的那個用 reasonRaw（null 也補成 "" 以通過 IS NOT NULL）
        String reasonReject = null, reasonAccept = null, reasonEnd = null;
        switch (st) {
            case NONE:
                reasonReject = null;
                reasonAccept = null;
                reasonEnd = null;
                caseReject = false;
                caseAccept = false;
                caseEnd = false;
                break;
            case REJECT:
                reasonReject = (reasonRaw == null ? "" : reasonRaw);
                reasonAccept = null;
                reasonEnd = null;
                caseAccept = false;
                caseEnd = false;
                break;
            case ACCEPT:
                reasonAccept = (reasonRaw == null ? "" : reasonRaw);
                reasonReject = null;
                reasonEnd = null;
                caseReject = false;
                caseEnd = false;
                break;
            case END:
                reasonEnd = (reasonRaw == null ? "" : reasonRaw);
                reasonReject = null;
                reasonAccept = null;
                caseReject = false;
                caseAccept = false;
                break;
        }

        // --- 1) 新增或更新 ProAdopt 主表 ---
        Integer proAdoptId = p.getProAdoptId();
        String message;

        if (proAdoptId == null) {
            // --- 嚴格遵循：null 就新增，不做存在性查詢 ---
            try {
                // 回傳新紀錄的 proAdoptId
                proAdoptId = repo.insertProAdopt(
                        p.getProRecId(), p.getScores(),
                        caseReject, reasonReject,
                        caseAccept, reasonAccept,
                        caseEnd, reasonEnd,
                        p.getAudit() == null ? null : p.getAudit().getCreatedByUserId()
                );
                message = "新增成功";
            } catch (DataIntegrityViolationException ex) { // or DuplicateKeyException
                // 這裡大多數是 UQ_ProAdopt_ProRecID 違反，代表同 ProRecID 已有一筆
                return new DataDto<>(null, new ResponseInfo(0, "此 ProRecID 已存在認輔評估，請改用更新或重新讀取畫面。"));
            }
        } else {
            // --- 更新前保險檢查：proAdoptID 與 proRecId 是否一致 ---
            Integer chk = repo.findProAdoptIdByProRecId(p.getProRecId());
            if (chk == null || !chk.equals(proAdoptId)) {
                return new DataDto<>(null, new ResponseInfo(0, "proAdoptId 與 proRecId 不一致"));
            }
            repo.updateProAdopt(
                    proAdoptId, p.getScores(),
                    caseReject, reasonReject,
                    caseAccept, reasonAccept,
                    caseEnd, reasonEnd,
                    p.getAudit() == null ? null : p.getAudit().getModifiedByUserId()
            );
            message = "更新成功";
        }

        // 覆蓋子表 // 此時 proAdoptId 一定有效
        repo.replaceDirectAdoptCriteria(proAdoptId, p.getDirectSelectedEntryIds());
        repo.replaceEvalAdoptCriteria(proAdoptId, p.getEvalSelectedEntryIds());

        // === 建立回傳 DTO ===
        int total = calcScoreTotal(p.getScores()); // 若 DB 有計算欄位想拿 DB 值，可改成查 DB
        String finalReason;
        switch (st) {
            case REJECT:
                finalReason = reasonReject;
                break;
            case ACCEPT:
                finalReason = reasonAccept;
                break;
            case END:
                finalReason = reasonEnd;
                break;
            default:
                finalReason = null;
                break;
        }

        Aca3001SaveResponse resp = Aca3001SaveResponse.builder()
                .proAdoptId(proAdoptId)
                .proRecId(proRecId)
                .editable(editable)
                .scoreTotal(total)
                .state(st)
                .reason(finalReason)
                .message(message)   // "新增成功" / "更新成功"
                .build();

        return new DataDto<>(resp, new ResponseInfo(1, message));
    }

    // 小工具：用 payload 內的九項分數計總分
    private static int calcScoreTotal(Aca3001SavePayload.Scores s) {
        return s.getEconomy()
                + s.getEmployment()
                + s.getFamily()
                + s.getSocial()
                + s.getPhysical()
                + s.getPsych()
                + s.getParenting()
                + s.getLegal()
                + s.getResidence();
    }

    //Delete API
    @Override
    @Transactional
    public DataDto<Void> delete(GeneralPayload<Aca3001DeletePayload> payload) {
        final String proRecId = (payload == null || payload.getData() == null)
                ? null : payload.getData().getProRecId();
        if (proRecId == null || proRecId.isBlank()) {
            return new DataDto<>(null, new ResponseInfo(0, "proRecId 不可為空"));
        }

        // 1) 找出對應的 ProAdoptID（代表是否曾「儲存過此表」）
        Integer proAdoptId = repo.findProAdoptIdByProRecId(proRecId);
        if (proAdoptId == null) {
            // 依規格：未儲存過則按鈕應該隱藏；若仍打到 API，就友善回覆
            return new DataDto<>(null, new ResponseInfo(0, "尚未儲存過此表，無可刪除資料"));
        }

        // 2) 鎖定日檢核（與 save 同規則）
        LocalDate timeLockDate = repo.loadTimeLockDate(); // Lists.TIMELOCK_ACABRD
        boolean editable = repo.isEditable(proRecId, timeLockDate);
        if (!editable) {
            return new DataDto<>(null, new ResponseInfo(0, "已逾鎖定日，不可刪除"));
        }

        // 3) 進行實體刪除（先子表後主表）
        repo.deleteProAdoptCascade(proAdoptId);

        // 4) 成功
        return new DataDto<>(null, new ResponseInfo(1, "刪除成功"));
    }
}
