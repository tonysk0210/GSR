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
     * Query API - 查詢 ProAdopt 主表與其相關資料
     */
    @Override
    @Transactional(readOnly = true)
    public DataDto<Aca3001QueryDto> query(GeneralPayload<Aca3001QueryPayload> payload) {

        // 0) 基本檢核：proRecId 必填
        final String proRecId = (payload == null || payload.getData() == null) ? null : payload.getData().getProRecId();
        if (proRecId == null || proRecId.isBlank()) {
            return new DataDto<>(null, new ResponseInfo(0, "proRecId 不可為空"));
        }

        // 1) 驗證個案存在性
        //    - Profile 為 null 表示查無此 ProRec
        //    - 規則：若個案不存在 → ACABrd 關聯也不成立
        Aca3001QueryDto.Profile profile = repo.computeProfile(proRecId);
        if (profile == null) {
            return new DataDto<>(null, new ResponseInfo(0, "查無資料"));
        }

        // 2) 取得認輔評估主表 ID
        //    - 若為 null 表示尚未建立過 ProAdopt
        Integer proAdoptId = repo.findProAdoptIdByProRecId(proRecId);

        // 3) 組裝完整 DTO
        //    - 各區塊由 repository 提供，Service 僅負責組合
        Aca3001QueryDto dto = new Aca3001QueryDto();
        dto.setMeta(repo.computeMeta(proRecId, proAdoptId));
        dto.setHeader(repo.computeHeader(proRecId));
        dto.setProfile(profile);
        dto.setDirectAdoptCriteria(repo.computeDirectAdoptCriteria(proAdoptId));
        dto.setEvalAdoptCriteria(repo.computeEvalAdoptCriteria(proAdoptId));
        dto.setSummary(repo.computeSummary(proRecId, proAdoptId));
        return new DataDto<>(dto, new ResponseInfo(1, "查詢成功"));
    }

    /**
     * Save API - 新增或更新 ProAdopt 主表與其子表
     */
    @Override
    @Transactional
    public DataDto<Aca3001SaveResponse> save(GeneralPayload<Aca3001SavePayload> payload) {
        // 0) 基本檢核：取 proRecId，必填
        final String proRecId = (payload == null || payload.getData() == null) ? null : payload.getData().getProRecId();
        if (proRecId == null || proRecId.isBlank()) {
            return new DataDto<>(null, new ResponseInfo(0, "proRecId 不可為空"));
        }

        // 1) 鎖定日檢核：若不可編輯則直接返回
        LocalDate timeLockDate = repo.loadTimeLockDate(); // 來源：Lists.TIMELOCK_ACABRD
        boolean editable = repo.isEditable(proRecId, timeLockDate);
        if (!editable) {
            return new DataDto<>(null, new ResponseInfo(0, "已逾鎖定日，不可編輯"));
        }

        Aca3001SavePayload p = payload.getData();

        // 2) 狀態與理由檢核
        //    - REJECT / ACCEPT / END 三選一
        //    - NONE → 全部維持預設（false / null）
        //    - 只有被選到的狀態，其 reason 允許空字串（以 "" 代表有填欄但為空）
        var st = p.getCaseStatus().getState();
        String reasonRaw = p.getCaseStatus().getReason();
        String selectedReason = (reasonRaw == null) ? "" : reasonRaw.trim();

        // 預設值：全部不選、理由皆為 null
        boolean caseReject = false, caseAccept = false, caseEnd = false;
        String reasonReject = null, reasonAccept = null, reasonEnd = null;

        // 只設定「被選到」的那一個
        switch (st) {
            case REJECT:
                caseReject = true;
                reasonReject = selectedReason;
                break;
            case ACCEPT:
                caseAccept = true;
                reasonAccept = selectedReason;
                break;
            case END:
                caseEnd = true;
                reasonEnd = selectedReason;
                break;
            case NONE:
            default:
                // 保持預設即可
                break;
        }

        // 3) 新增或更新 ProAdopt 主表
        Integer proAdoptId = p.getProAdoptId();
        String message;

        if (proAdoptId == null) {
            // 規則：若 proAdoptId 為 null → 一律新增，不先查存在性
            try {
                // 回傳新紀錄的 proAdoptId
                proAdoptId = repo.insertProAdopt(
                        p.getProRecId(), p.getScores(),
                        caseReject, reasonReject,
                        caseAccept, reasonAccept,
                        caseEnd, reasonEnd,
                        p.getAudit() == null ? null : p.getAudit().getUserId()
                );
                message = "新增成功";
            } catch (DataIntegrityViolationException ex) {
                // 常見原因：違反 UQ_ProAdopt_ProRecID → 同一個 ProRecID 已存在紀錄
                return new DataDto<>(null, new ResponseInfo(0, "此 ProRecID 已存在認輔評估，請改用更新或重新讀取畫面。"));
            }
        } else {
            // 更新前保險檢查：確認 proRecId ↔ proAdoptId 是否一致
            Integer chk = repo.findProAdoptIdByProRecId(p.getProRecId());
            if (chk == null || !chk.equals(proAdoptId)) {
                return new DataDto<>(null, new ResponseInfo(0, "proAdoptId 與 proRecId 不一致"));
            }
            repo.updateProAdopt(
                    proAdoptId, p.getScores(),
                    caseReject, reasonReject,
                    caseAccept, reasonAccept,
                    caseEnd, reasonEnd,
                    p.getAudit() == null ? null : p.getAudit().getUserId()
            );
            message = "更新成功";
        }

        // 4) 更新子表 (覆蓋式更新)
        repo.replaceDirectAdoptCriteria(proAdoptId, p.getDirectSelectedEntryIds());
        repo.replaceEvalAdoptCriteria(proAdoptId, p.getEvalSelectedEntryIds());

        // 5) 建立回傳 DTO
        int total = p.getScores().getEconomy()
                + p.getScores().getEmployment()
                + p.getScores().getFamily()
                + p.getScores().getSocial()
                + p.getScores().getPhysical()
                + p.getScores().getPsych()
                + p.getScores().getParenting()
                + p.getScores().getLegal()
                + p.getScores().getResidence();

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
                .message(message) // "新增成功" / "更新成功"
                .build();

        return new DataDto<>(resp, new ResponseInfo(1, message));
    }

    /**
     * Delete API - 刪除 ProAdopt 主表及其子表
     */
    @Override
    @Transactional
    public DataDto<Void> delete(GeneralPayload<Aca3001DeletePayload> payload) {
        // 0) 基本檢核：proRecId 必填
        final String proRecId = (payload == null || payload.getData() == null) ? null : payload.getData().getProRecId();
        if (proRecId == null || proRecId.isBlank()) {
            return new DataDto<>(null, new ResponseInfo(0, "proRecId 不可為空"));
        }

        // 1) 確認是否已存在 ProAdopt 資料，若找不到代表從未儲存過
        Integer proAdoptId = repo.findProAdoptIdByProRecId(proRecId);
        if (proAdoptId == null) {
            // 規格上此時按鈕應該隱藏，但若仍呼叫 API，則回傳友善訊息
            return new DataDto<>(null, new ResponseInfo(0, "尚未儲存過此表，無可刪除資料"));
        }

        // 2) 鎖定日檢核　- 規則同 save()：若已逾鎖定日則禁止刪除
        LocalDate timeLockDate = repo.loadTimeLockDate(); // 來源：Lists.TIMELOCK_ACABRD
        boolean editable = repo.isEditable(proRecId, timeLockDate);
        if (!editable) {
            return new DataDto<>(null, new ResponseInfo(0, "已逾鎖定日，不可刪除"));
        }

        // 3) 執行實體刪除，先刪子表，再刪主表（由 repo.deleteProAdoptCascade 處理）
        repo.deleteProAdoptCascade(proAdoptId);

        // 4) 成功回覆
        return new DataDto<>(null, new ResponseInfo(1, "刪除成功"));
    }
}
