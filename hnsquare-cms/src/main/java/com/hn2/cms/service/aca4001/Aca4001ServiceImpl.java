package com.hn2.cms.service.aca4001;

import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto;
import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto.*;
import com.hn2.cms.payload.aca4001.Aca4001EraseQueryPayload;
import com.hn2.cms.repository.aca4001.Aca4001Repository;
import com.hn2.core.dto.DataDto;
import com.hn2.core.dto.ResponseInfo;
import com.hn2.core.payload.GeneralPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class Aca4001ServiceImpl implements Aca4001Service {

    private final Aca4001Repository repo;

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
        dto.setProRecListBefore18(proIds.stream().map(id -> {
            ProRec p = new ProRec();
            p.setId(id);
            return p;
        }).collect(toList()));

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
}
