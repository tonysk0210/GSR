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
import com.hn2.cms.service.aca4001.erase.command.EraseCommand;
import com.hn2.cms.service.aca4001.erase.command.RestoreCommand;
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
    private final GenericEraseService genericEraseService; // 用於執行通用的塗銷與還原邏輯

    /**
     * 依個案卡號與(可選)日期區間，查詢其在「滿 18 歲以前」的 CrmRec/ProRec 清單，
     * 並回傳輔助判斷旗標（是否已滿 18、最新 ProRec 是否結案、主檔是否已塗銷）。
     * 流程：
     * 1) 驗證外層 payload 與必要欄位 (acaCardNo / start<=end)。
     * 2) 讀取個案生日與「滿 18 歲當日」(eighteenthStart)；若查無人或生日為 null，回傳對應訊息。
     * 3) 判斷是否已滿 18；未滿 18 → 直接回空清單與旗標。
     * 4) 已滿 18 → 組成查詢時間區間 (eighteenthStart 排除；[start, endInclusive] 可選)。
     * 5) 依條件撈出 CrmRec/ProRec 的 ID 清單，再批次補齊欄位成 DTO 清單。
     * 6) 查詢「最新 ProRec 是否結案」與「主檔是否已塗銷」，設定回傳 DTO 的旗標。
     *
     * @param payload GeneralPayload 包裹的請求物件，內含 acaCardNo / startDate / endDate 字串
     * @return 查詢結果與訊息
     * @throws IllegalArgumentException 當必要欄位缺漏或日期區間不正確時
     */
    @Override
    @Transactional(readOnly = true)
    public DataDto<Aca4001EraseQueryDto> eraseQuery(GeneralPayload<Aca4001EraseQueryPayload> payload) {

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

        // ★ 生日為 null 的處理（避免後續計算 NPE / 邏輯錯誤）
        if (pb.getBirthDate() == null) {
            return new DataDto<>(null, new ResponseInfo(0, "個案生日不可為null"));
        }

        Aca4001EraseQueryDto dto = new Aca4001EraseQueryDto();

        // 以系統當日判斷是否已滿 18（today >= eighteenthStart）
        LocalDate today = LocalDate.now();
        boolean over18 = !today.isBefore(pb.getEighteenthStart()); // today >= 18歲當日
        dto.setOver18(over18);

        // 未滿 18：直接回空清單與訊息
        if (!over18) {
            dto.setProRecListBefore18(List.of());
            dto.setCrmRecListBefore18(List.of());
            return new DataDto<>(dto, new ResponseInfo(1, "查詢成功：個案未滿18"));
        }

        // 2) 已滿18 → 組區間 [start, end]，end 含當天
        LocalDateTime eighteenthStart = pb.getEighteenthStart().atStartOfDay();
        LocalDateTime startTs = (start == null) ? null : start.atStartOfDay();
        LocalDateTime endInclusive = (end == null) ? null : end.atTime(23, 59, 59, 999_000_000);

        // 3) 針對未滿18歲個案，撈18歲前的犯罪紀錄+保護紀錄 ID 清單
        List<String> crmIds = repo.findCrmRecIdsBefore18(acaCardNo, eighteenthStart, startTs, endInclusive);
        List<String> proIds = repo.findProRecIdsBefore18(acaCardNo, eighteenthStart, startTs, endInclusive);

        // CrmRec：用犯罪紀錄ID清單補齊CrmRec.dto欄位
        List<Aca4001EraseQueryDto.CrmRec> crmRecs = repo.findCrmRecsByIds(crmIds);
        dto.setCrmRecListBefore18(crmRecs);

        // ProRec：用保護紀錄ID清單補齊ProRec.dto欄位
        List<Aca4001EraseQueryDto.ProRec> proRecs = repo.findProRecsByIds(proIds);
        dto.setProRecListBefore18(proRecs);

        // 4) 判斷最新 ProRec 是否結案
        Boolean latestClosed = repo.findLatestProRecClosed(acaCardNo);
        dto.setLatestProRecClosed(latestClosed != null && latestClosed);

        // 5) 判斷 ACABrd 是否已塗銷
        Boolean erased = repo.findPersonErased(acaCardNo);
        dto.setErased(erased != null && erased);

        // 已滿 18 的正常回覆
        return new DataDto<>(dto, new ResponseInfo(1, "查詢成功：個案已滿18"));
    }

    /**
     * 執行塗銷（erase）：
     * - 若個案「已滿 18」：只塗銷前端勾選的 CrmRec / ProRec，及其關聯表
     * - 若個案「未滿 18」：塗銷該卡號底下「所有」 CrmRec / ProRec，及其關聯表，並同時觸發以 ACABrd 為父表的相依目標。
     * 流程：
     * 1) 驗證 payload 與 acaCardNo。
     * 2) 判定滿 18 與否（若前端未傳 isOver18，為了相容舊行為，預設視為已滿 18）。
     * 3) 建立 tableToIds 對應：滿 18 → 使用者選取；未滿 18 → DB 查出全部 ID，並放入 ACABrd 以驅動 DependentEraseTarget。
     * 4) 建立 EraseCommand 交給 GenericEraseService 執行（鏡像 → 清空 → 稽核）。
     * 5) 回傳結果訊息。
     * 交易性：
     * - 使用 @Transactional，方法內任何未捕捉的 Runtime 例外將導致整個交易回滾。
     *
     * @param payload 包含 acaCardNo、使用者選取的紀錄 ID、以及可選的 isOver18/docNum/eraseReason
     * @param userId  操作人 UserID（用於稽核）
     * @param userIp  操作人 IP（用於稽核）
     * @return 空資料主體 + 成功訊息（依滿/未滿 18 帶入不同字串）
     * @throws IllegalArgumentException acaCardNo 缺漏或為空時拋出
     */
    @Override
    @Transactional
    public DataDto<Void> erase(GeneralPayload<Aca4001ErasePayload> payload, Integer userId, String userIp) {
        var req = payload.getData();
        if (req == null || req.getAcaCardNo() == null || req.getAcaCardNo().isBlank())
            throw new IllegalArgumentException("acaCardNo 不可為空");

        // 2) 判定是否已滿 18：
        //    - 若前端有帶 isOver18 就採用
        //    - 若沒帶，為了相容舊版行為，預設當作「已滿 18」
        Boolean over18Flag = req.getIsOver18();
        boolean isOver18 = (over18Flag != null) ? over18Flag : true;

        // 3) 整理「要塗銷的表與其 ID 清單」的對應表
        var tableToIds = new java.util.HashMap<String, List<String>>();

        if (isOver18) {
            // (A) 已滿 18 → 只塗銷使用者選取的紀錄（可能為空清單，服務會自行跳過）
            tableToIds.put("CrmRec", java.util.Optional.ofNullable(req.getSelectedCrmRecIds()).orElse(List.of()));
            tableToIds.put("ProRec", java.util.Optional.ofNullable(req.getSelectedProRecIds()).orElse(List.of()));
        } else {
            // (B) 未滿 18 → 全案塗銷
            //     依卡號把該個案底下的所有 CrmRec / ProRec 的 ID 全撈出
            List<String> allCrmIds = repo.findAllCrmRecIdsByAcaCardNo(req.getAcaCardNo());
            List<String> allProIds = repo.findAllProRecIdsByAcaCardNo(req.getAcaCardNo());

            tableToIds.put("CrmRec", allCrmIds);
            tableToIds.put("ProRec", allProIds);

            // 另外放入父表 ACABrd 的 key，讓 DependentEraseTarget（例如 Families / Career / Memo / ACABrd 本身）
            // 能以 parentTableName() == "ACABrd" 及此卡號清單作為刪空依據。
            tableToIds.put("ACABrd", List.of(req.getAcaCardNo()));
        }

        // 4) 組成 EraseCommand（包含人員、IP、發文文號、塗銷原因等資訊）交給共用服務處理
        EraseCommand cmd = EraseCommand.builder()
                .acaCardNo(req.getAcaCardNo())
                .tableToIds(tableToIds)
                .operatorUserId(userId)
                .operatorIp(userIp)
                .docNum(req.getDocNum())
                .eraseReason(req.getEraseReason())
                .build();

        // 5) 執行實際塗銷（鏡像 → 清空；成功後寫一筆 ERASE 入塗銷異動表）
        genericEraseService.eraseRows(cmd);

        // 6) 回傳訊息（依滿/未滿 18 顯示不同成功字串）
        return new DataDto<>(null, new ResponseInfo(1,
                isOver18 ? "成功塗銷（滿18：依使用者選取）" : "成功塗銷（未滿18：全案＋關聯表）"));
    }

    /**
     * 還原前查詢（Restore-Query）：
     * 依 ACACardNo 檢查主檔 ACABrd 是否被標記為已塗銷 (IsErase)。
     * <p>
     * 流程：
     * 1) 驗證 payload 與 acaCardNo。
     * 2) 呼叫 repo.findPersonErased(acaCardNo) 取得目前塗銷狀態（可能為 null 表示查無此人）。
     * 3) 組裝 Aca4001RestoreQueryDto（只帶 erased 旗標），以及對應訊息字串。
     * <p>
     * 回傳：
     * - DataDto<Aca4001RestoreQueryDto>，其中 dto.erased =
     * - true  ：ACABrd.IsErase = 1
     * - false ：ACABrd.IsErase = 0 或查無資料（當 erased == null 時此方法還是把 dto 設為 false，但訊息會提示查無）
     * - ResponseInfo(1, msg) 描述當前狀態（或查無資料）
     *
     * @param payload 包含 acaCardNo 的查詢請求
     * @return 查詢結果與訊息
     * @throws IllegalArgumentException 當 payload.data 或 acaCardNo 缺漏/空白
     */
    @Override
    public DataDto<Aca4001RestoreQueryDto> restoreQuery(GeneralPayload<Aca4001RestoreQueryPayload> payload) {
        // 1) 基本驗證：payload 與 data 不可為空
        if (payload == null || payload.getData() == null) {
            throw new IllegalArgumentException("data 不可為空");
        }
        var req = payload.getData();
        String acaCardNo = req.getAcaCardNo() == null ? "" : req.getAcaCardNo().trim();
        if (acaCardNo.isEmpty()) {
            throw new IllegalArgumentException("acaCardNo 不可為空");
        }

        // 2) 直接查主檔 ACABrd 的塗銷旗標（可能回 null：表示查無此卡號/未找到有效資料）
        Boolean erased = repo.findPersonErased(acaCardNo); // 可能為 null

        // 3) 組 DTO：null 視為 false（僅旗標用途）；訊息中另行說明
        var dto = new Aca4001RestoreQueryDto();
        dto.setErased(erased != null && erased);

        // 4) 組訊息：明確說明三種狀態（查無 / 已塗銷 / 未塗銷）
        String msg = (erased == null) ? "查無 ACABrd 資料" : (erased ? "此個案目前為已塗銷 (isERASE=1)" : "此個案目前為未塗銷 (isERASE=0)");

        // 5) 回傳結果（成功碼固定 1；內容以訊息區分）
        return new DataDto<>(dto, new ResponseInfo(1, msg));
    }

    /**
     * 依 ACACardNo 執行「還原」。
     * 流程：
     * 1) 驗證 payload 與必填參數（acaCardNo）。
     * 2) 組成 RestoreCommand（帶操作者 userId / userIp / 還原原因）。
     * 3) 委派給 genericEraseService.restoreAllByAcaCardNo(cmd)：
     * - 讀取 ACA_EraseMirror 中該 ACACardNo 的鏡像資料
     * - 逐表解密＋校驗（AES-GCM + SHA-256），通過者寫回原表
     * - 成功後寫一筆 RESTORE 稽核，並清理鏡像（依你的 service 實作）
     * 4) 回傳成功訊息。
     * 交易特性：
     * - 方法加上 @Transactional；若過程中任一步拋出 Runtime 例外，整個還原交易將回滾，
     * 不會產生成功稽核，鏡像也不會被刪除（便於調查）。
     *
     * @param payload 包含 acaCardNo 與 restoreReason 的請求
     * @param userId  操作者 ID（將寫入稽核；型別需與資料庫欄位一致）
     * @param userIp  來源 IP（寫入稽核）
     * @return 空資料主體 + 成功訊息
     * @throws IllegalArgumentException 當 acaCardNo 缺漏或空白
     */
    @Override
    @Transactional
    public DataDto<Void> restore(GeneralPayload<Aca4001RestorePayload> payload, String userId, String userIp) {
        var req = payload.getData();
        if (req == null || req.getAcaCardNo() == null || req.getAcaCardNo().isBlank())
            throw new IllegalArgumentException("acaCardNo 不可為空");

        // 建立還原指令物件：包含個案編號、操作者資訊、還原原因
        RestoreCommand cmd = RestoreCommand.builder()
                .acaCardNo(req.getAcaCardNo())
                .operatorUserId(userId)
                .operatorIp(userIp)
                .restoreReason(req.getRestoreReason())
                .build();

        // 執行實際還原流程：
        // - 讀鏡像 → 解密/校驗 → 分表寫回 → 寫 RESTORE 入塗銷異動表 → 清理鏡像（視你的 service 實作）
        genericEraseService.restoreAllByAcaCardNo(cmd);

        // 回傳成功訊息
        return new DataDto<>(null, new ResponseInfo(1, "還原成功 for ACACardNo=" + req.getAcaCardNo()));
    }

    /**
     * 塗銷異動紀錄查詢（Audit-Query）。
     * 流程：
     * 1) 透過 repo.findAuditRows() 讀取全部塗銷/還原的塗銷異動資料列。
     * 2) 封裝到 Aca4001AuditQueryDto.items 後回傳。
     * 回傳：
     * - DataDto<Aca4001AuditQueryDto>，成功碼固定 1，訊息「查詢成功」。
     * - 當查無資料時，items 應為空清單（而非 null）。
     *
     * @return 包含稽核項目的回應包裝物件
     */
    @Override
    public DataDto<Aca4001AuditQueryDto> auditQuery() {
        // 1) 讀取稽核資料列（由 repo 控制排序/篩選/分頁等）
        var rows = repo.findAuditRows();

        // 2) 封裝成回傳 DTO
        var dto = new Aca4001AuditQueryDto();
        dto.setItems(rows);

        return new DataDto<>(dto, new ResponseInfo(1, "查詢成功"));
    }

    //Helper method for eraseQuery
    /*Helper method for eraseQuery*/
    private static LocalDate parseDateOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s); // 預期 yyyy-MM-dd
    }
}
