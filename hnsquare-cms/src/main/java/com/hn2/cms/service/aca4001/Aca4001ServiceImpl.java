package com.hn2.cms.service.aca4001;

import com.hn2.cms.dto.aca4001.Aca4001AuditQueryDto;
import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto;
import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto.*;
import com.hn2.cms.dto.aca4001.Aca4001RestoreQueryDto;
import com.hn2.cms.payload.aca4001.Aca4001ErasePayload;
import com.hn2.cms.payload.aca4001.Aca4001EraseQueryPayload;
import com.hn2.cms.payload.aca4001.Aca4001RestorePayload;
import com.hn2.cms.payload.aca4001.Aca4001RestoreQueryPayload;
import com.hn2.cms.repository.aca4001.Aca4001Repository;
import com.hn2.cms.service.aca4001.erase.GenericEraseService;
import com.hn2.cms.service.aca4001.erase.model.EraseCommand;
import com.hn2.cms.service.aca4001.erase.model.RestoreCommand;
import com.hn2.core.dto.DataDto;
import com.hn2.core.dto.ResponseInfo;
import com.hn2.core.payload.GeneralPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class Aca4001ServiceImpl implements Aca4001Service {

    private final Aca4001Repository repo;
    private final GenericEraseService genericEraseService; // ★ 新的通用服務

    @Override
    @Transactional(readOnly = true)
    public DataDto<Aca4001EraseQueryDto> eraseQuery(GeneralPayload<Aca4001EraseQueryPayload> payload) {

        // ---- 取出 data（GeneralPayload 包住的真正 payload）----
        if (payload == null || payload.getData() == null) {
            throw new IllegalArgumentException("data 不可為空");
        }

        Aca4001EraseQueryPayload req = payload.getData();

        final String acaCardNo = (req.getAcaCardNo() == null ? "" : req.getAcaCardNo().trim());
        if (acaCardNo.isEmpty()) {
            throw new IllegalArgumentException("acaCardNo 不可為空");
        }

        LocalDate start = parseDateOrNull(req.getStartDate());
        LocalDate end = parseDateOrNull(req.getEndDate());
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("startDate 不可晚於 endDate");
        }

        // 1) 取得生日與 18 歲門檻
        PersonBirth pb = repo.findPersonBirth(acaCardNo);
        if (pb == null) {
            // 查無此人
            return new DataDto<>(null, new ResponseInfo(0, "查無此個案編號，請重新輸入"));
        }

        // ★ 新增這段：生日為 null 的處理
        if (pb.getBirthDate() == null) {
            return new DataDto<>(null, new ResponseInfo(0, "個案生日不可為null"));
        }

        Aca4001EraseQueryDto dto = new Aca4001EraseQueryDto();

        LocalDate today = LocalDate.now();
        boolean over18 = !today.isBefore(pb.getEighteenthStart()); // today >= 18歲當日
        dto.setOver18(over18);

        if (!over18) {
            dto.setProRecListBefore18(List.of());
            dto.setCrmRecListBefore18(List.of());
            return new DataDto<>(dto, new ResponseInfo(1, "查詢成功：個案未滿18"));
        }

        // 2) 已滿18 → 組區間 [start, end]，end 含當天
        LocalDateTime eighteenthStart = pb.getEighteenthStart().atStartOfDay();
        LocalDateTime startTs = (start == null) ? null : start.atStartOfDay();
        LocalDateTime endInclusive = (end == null) ? null : end.atTime(23, 59, 59, 999_000_000);

        // 3) 撈 ID 清單
        List<String> crmIds = repo.findCrmRecIdsBefore18(acaCardNo, eighteenthStart, startTs, endInclusive);
        List<String> proIds = repo.findProRecIdsBefore18(acaCardNo, eighteenthStart, startTs, endInclusive);

        //把 List<Integer> crmIds（純 ID 清單）轉成 List<CrmRec>（物件清單）後塞進 DTO
        /*dto.setCrmRecListBefore18(crmIds.stream().map(id -> {
            CrmRec c = new CrmRec();
            c.setId(id);
            return c;
        }).collect(toList()));*/

        // CrmRec：用 ID 清單把欄位補齊
        List<Aca4001EraseQueryDto.CrmRec> crmRecs = repo.findCrmRecsByIds(crmIds);
        dto.setCrmRecListBefore18(crmRecs);

        //把 List<Integer> proIds（純 ID 清單）轉成 List<ProRec>（物件清單）後塞進 DTO
        /*dto.setProRecListBefore18(proIds.stream().map(id -> {
            ProRec p = new ProRec();
            p.setId(id);
            return p;
        }).collect(toList()));*/

        // 新版：一次補齊 ProRec 欄位
        List<Aca4001EraseQueryDto.ProRec> proRecs = repo.findProRecsByIds(proIds);
        dto.setProRecListBefore18(proRecs);

        // 4) 判斷最新 ProRec 是否結案
        Boolean latestClosed = repo.findLatestProRecClosed(acaCardNo);
        dto.setLatestProRecClosed(latestClosed != null && latestClosed);

        // 5) 判斷 ACABrd 是否已塗銷
        Boolean erased = repo.findPersonErased(acaCardNo);
        dto.setErased(erased != null && erased);

        return new DataDto<>(dto, new ResponseInfo(1, "查詢成功：個案已滿18"));
    }

    private static LocalDate parseDateOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s); // 預期 yyyy-MM-dd
    }

    @Override
    @Transactional
    public DataDto<Void> erase(GeneralPayload<Aca4001ErasePayload> payload, String userId, String userIp) {
        var req = payload.getData();
        if (req == null || req.getAcaCardNo() == null || req.getAcaCardNo().isBlank())
            throw new IllegalArgumentException("acaCardNo 不可為空");

        Boolean over18Flag = req.getIsOver18();
        boolean isOver18;
        if (over18Flag != null) {
            isOver18 = over18Flag;
        } else {
            isOver18 = true; // fallback to current behavior if client didn't send the flag
        }

        var tableToIds = new java.util.HashMap<String, List<String>>();

        if (isOver18) {
            // Original behavior: only what the user selected
            tableToIds.put("CrmRec",
                    java.util.Optional.ofNullable(req.getSelectedCrmRecIds()).orElse(List.of()));
            tableToIds.put("ProRec",
                    java.util.Optional.ofNullable(req.getSelectedProRecIds()).orElse(List.of()));
        } else {
            // Under 18: erase EVERYTHING for this ACACardNo
            List<String> allCrmIds = repo.findAllCrmRecIdsByAcaCardNo(req.getAcaCardNo());
            List<String> allProIds = repo.findAllProRecIdsByAcaCardNo(req.getAcaCardNo());

            tableToIds.put("CrmRec", allCrmIds);
            tableToIds.put("ProRec", allProIds);

            // Trigger ACABrd-dependent targets (ACAFamilies, Career, Memo, ACABrd).
            // Your DependentEraseTarget(s) listen to parentTableName() == "ACABrd"
            // and will use this ACACardNo list as the parent ids.
            tableToIds.put("ACABrd", List.of(req.getAcaCardNo()));
        }

        EraseCommand cmd = EraseCommand.builder()
                .acaCardNo(req.getAcaCardNo())
                .tableToIds(tableToIds)
                .operatorUserId(userId)
                .operatorIp(userIp)
                .docNum(req.getDocNum())           // 若無此欄可移除
                .eraseReason(req.getEraseReason()) // 若無此欄可移除
                .build();

        genericEraseService.eraseRows(cmd);
        return new DataDto<>(null, new ResponseInfo(1,
                isOver18 ? "成功塗銷（滿18：依使用者選取）" : "成功塗銷（未滿18：全案＋關聯表）"));
    }

    @Override
    public DataDto<Aca4001RestoreQueryDto> restoreQuery(GeneralPayload<Aca4001RestoreQueryPayload> payload) {
        if (payload == null || payload.getData() == null) {
            throw new IllegalArgumentException("data 不可為空");
        }

        var req = payload.getData();
        String acaCardNo = req.getAcaCardNo() == null ? "" : req.getAcaCardNo().trim();
        if (acaCardNo.isEmpty()) {
            throw new IllegalArgumentException("acaCardNo 不可為空");
        }

        // 直接查 ACABrd.IsERASE
        Boolean erased = repo.findPersonErased(acaCardNo); // 可能為 null

        var dto = new Aca4001RestoreQueryDto();
        dto.setErased(erased != null && erased);

        String msg = (erased == null) ? "查無 ACABrd 資料" : (erased ? "此個案目前為已塗銷 (isERASE=1)" : "此個案目前為未塗銷 (isERASE=0)");

        return new DataDto<>(dto, new ResponseInfo(1, msg));
    }

    @Override
    @Transactional
    public DataDto<Void> restore(GeneralPayload<Aca4001RestorePayload> payload, String userId, String userIp) {
        var req = payload.getData();
        if (req == null || req.getAcaCardNo() == null || req.getAcaCardNo().isBlank())
            throw new IllegalArgumentException("acaCardNo 不可為空");

        RestoreCommand cmd = RestoreCommand.builder()
                .acaCardNo(req.getAcaCardNo())
                .operatorUserId(userId)
                .operatorIp(userIp)
                .restoreReason(req.getRestoreReason())
                .build();

        genericEraseService.restoreAllByAcaCardNo(cmd);
        return new DataDto<>(null, new ResponseInfo(1, "還原成功 for ACACardNo=" + req.getAcaCardNo()));
    }

    //audit
    @Override
    public DataDto<Aca4001AuditQueryDto> auditQuery() {
        var groups = repo.findAuditGroups(null, null, null);

        var dto = new Aca4001AuditQueryDto();
        dto.setAggregated(true);
        dto.setGroups(groups);

        return new DataDto<>(dto, new ResponseInfo(1, "查詢成功（彙總）"));
    }
}
