package com.hn2.cms.service.aca2003;

import com.hn2.cms.dto.aca2003.Aca2003DetailView;
import com.hn2.cms.dto.aca2003.Aca2003QueryDto;
import com.hn2.cms.dto.aca2003.Aca2003SaveResponse;
import com.hn2.cms.model.aca2003.AcaDrugUseEntity;
import com.hn2.cms.payload.aca2003.Aca2003DeletePayload;
import com.hn2.cms.payload.aca2003.Aca2003QueryByCardPayload;
import com.hn2.cms.payload.aca2003.Aca2003QueryByIdPayload;
import com.hn2.cms.payload.aca2003.Aca2003SavePayload;
import com.hn2.cms.repository.aca2003.Aca2003Repository;
import com.hn2.core.dto.DataDto;
import com.hn2.core.dto.ResponseInfo;
import com.hn2.core.payload.GeneralPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Optional;

/**
 * Aca2003ServiceImpl
 * <p>
 * 提供「毒品濫用資料表 (AcaDrugUse)」的應用服務：
 * - save: 新增或更新（軟規則驗證 + 一致性檢查）
 * - queryById: 依 ID 查詢詳情（含 ACABrd 連動欄位）
 * - queryLatestByCardNo: 依 ACACardNo 取最新一筆（ID 最大）
 * - softDelete: 軟刪除（isDeleted=1）
 * <p>
 * 設計要點：
 * - 業務規則集中在 Service（資料層只負責查存）
 * - 軟刪除統一過濾 (isDeleted=0 or null)
 * - 例外訊息採友善字串，便於前端顯示
 */
@Service
public class Aca2003ServiceImpl implements Aca2003Service {

    // ====== 常數訊息（避免日後多處修改） ======
    private static final String MSG_PAYLOAD_EMPTY = "payload 不可為空";
    private static final String MSG_ID_EMPTY = "id 不可為空";
    private static final String MSG_USER_ID_EMPTY = "userId 不可為空";
    private static final String MSG_CARD_EMPTY = "acaCardNo 不可為空";
    private static final String MSG_PROREC_EMPTY = "proRecId 不可為空";
    private static final String MSG_DATA_NOT_FOUND = "指定資料不存在";
    private static final String MSG_DATA_DELETED = "指定資料已刪除";
    private static final String MSG_DUP_ACTIVE = "相同「個案編號」+「保護紀錄編號」的有效資料已存在";
    private static final String MSG_ACABRD_NOT_FOUND = "指定的「個案編號」(ACACardNo) 不存在於有效的 「個案基本資料」ACABrd";
    private static final String MSG_REC_MISMATCH = "指定 「保護紀錄」(ProRecId) 與 「個案編號」(ACACardNo) 不一致";

    private final Aca2003Repository repo;

    @Autowired
    public Aca2003ServiceImpl(Aca2003Repository repo) {
        this.repo = repo;
    }

    // ============================================================
    // save API：新增或更新
    // ============================================================

    /**
     * 新增或更新 AcaDrugUse 資料。
     * - 新增：id = null，需檢查 ACABrd 存在、ProRec 與 ACACardNo 一致、且不可有有效重覆
     * - 更新：id != null，主鍵欄位不可變更，只能更新業務欄位
     */
    @Override
    @Transactional
    public DataDto<Aca2003SaveResponse> save(GeneralPayload<Aca2003SavePayload> payload) {
        // ---- 0) 入口檢核 ----
        if (payload == null || payload.getData() == null) {
            return fail(MSG_PAYLOAD_EMPTY);
        }
        var p = payload.getData();
        if (p.getUserId() == null) return fail(MSG_USER_ID_EMPTY);
        if (isBlank(p.getAcaCardNo())) return fail(MSG_CARD_EMPTY);
        if (isBlank(p.getProRecId())) return fail(MSG_PROREC_EMPTY);

        // 正規化基本欄位（trim）
        final String card = p.getAcaCardNo().trim();
        final String rec = p.getProRecId().trim();
        final Timestamp now = new Timestamp(System.currentTimeMillis());

        try {
            // ---- A) 應用層的一致性檢查 ----
            // 1) ACABrd 必須存在且有效
            if (repo.existsActiveAcaBrd(card) == 0) {
                return fail(MSG_ACABRD_NOT_FOUND);
            }
            // 2) ProRec(ID, ACACardNo) 必須相符
            if (repo.matchProRecWithCard(rec, card) == 0) {
                return fail(MSG_REC_MISMATCH);
            }

            // ---- B) 分支：新增 or 更新 ----
            if (p.getId() == null) {
                return create(p, card);   // 新增
            } else {
                return update(p, card, rec); // 更新
            }

        } catch (DataIntegrityViolationException ex) {
            // 兜底：DB 層唯一衝突（2627/2601），轉為友善訊息
            if (isUniqueKeyViolation(ex)) {
                return fail(MSG_DUP_ACTIVE);
            }
            throw ex; // 非預期例外：往上丟交由全域處理器
        }
    }

    /**
     * 新增流程（抽出提升可讀性）
     */
    private DataDto<Aca2003SaveResponse> create(Aca2003SavePayload p, String card) {
        // 是否已存在同 (ACACardNo, ProRecId) 且未刪除的資料
        if (repo.countActive(card, p.getProRecId().trim()) > 0) {
            return fail(MSG_DUP_ACTIVE);
        }
        var e = new AcaDrugUseEntity();
        copyFieldsForCreate(p, e); // 鍵值 + 業務欄位
        e.setCreatedByUserId(p.getUserId());
        e.setCreatedOnDate(now());
        e.setIsDeleted(Boolean.FALSE);
        e.setCreatedByBranchId(repo.findCreatedByBranchIdByAcaCardNo(card)); // 由 ACABrd 帶入
        e = repo.save(e);
        return ok(e.getId(), "新增成功");
    }

    /**
     * 建立時：寫入鍵值 + 其它欄位
     */
    private static void copyFieldsForCreate(Aca2003SavePayload p, AcaDrugUseEntity e) {
        // 建立時：寫入鍵值 + 其它欄位
        e.setAcaCardNo(trim(p.getAcaCardNo()));
        e.setProRecId(trim(p.getProRecId()));
        e.setDrgUserText(trimToNull(p.getDrgUserText()));
        e.setOprFamilyText(trimToNull(p.getOprFamilyText()));
        e.setOprFamilyCareText(trimToNull(p.getOprFamilyCareText()));
        e.setOprSupportText(trimToNull(p.getOprSupportText()));
        e.setOprContactText(trimToNull(p.getOprContactText()));
        e.setOprReferText(trimToNull(p.getOprReferText()));
        e.setAddr(trimToNull(p.getAddr()));
        e.setOprAddr(trimToNull(p.getOprAddr()));
    }

    /**
     * 更新流程（抽出提升可讀性）
     */
    private DataDto<Aca2003SaveResponse> update(Aca2003SavePayload p, String card, String rec) {
        Optional<AcaDrugUseEntity> opt = repo.findById(p.getId());
        if (opt.isEmpty()) return fail(MSG_DATA_NOT_FOUND);

        var exist = opt.get();
        if (Boolean.TRUE.equals(exist.getIsDeleted())) {
            return fail(MSG_DATA_DELETED);
        }
        // 前端帶的鍵值需與資料庫一致（不可修改主鍵/關聯鍵）
        if (!card.equals(exist.getAcaCardNo()) || !rec.equals(exist.getProRecId())) {
            return fail("「個案編號」(" + card + ")、無此「保護紀錄」資料(" + rec + ")");
        }

        // 僅覆寫非鍵值欄位
        copyFieldsForUpdate(p, exist);
        exist.setModifiedByUserId(p.getUserId());
        exist.setModifiedOnDate(now());
        repo.save(exist);
        return ok(p.getId(), "更新成功");
    }

    /**
     * 更新時：不可改鍵值，只覆寫業務欄位
     */
    private static void copyFieldsForUpdate(Aca2003SavePayload p, AcaDrugUseEntity e) {
        // 更新時：不可改鍵值，只覆寫業務欄位
        e.setDrgUserText(trimToNull(p.getDrgUserText()));
        e.setOprFamilyText(trimToNull(p.getOprFamilyText()));
        e.setOprFamilyCareText(trimToNull(p.getOprFamilyCareText()));
        e.setOprSupportText(trimToNull(p.getOprSupportText()));
        e.setOprContactText(trimToNull(p.getOprContactText()));
        e.setOprReferText(trimToNull(p.getOprReferText()));
        e.setAddr(trimToNull(p.getAddr()));
        e.setOprAddr(trimToNull(p.getOprAddr()));
    }

    /**
     * SQL Server 唯一衝突（2627 unique constraint；2601 duplicate index）
     */
    private static boolean isUniqueKeyViolation(DataIntegrityViolationException ex) {
        Throwable t = ex.getMostSpecificCause();
        while (t != null) {
            if (t instanceof java.sql.SQLException) {
                java.sql.SQLException se = (java.sql.SQLException) t;
                int code = se.getErrorCode();
                if (code == 2627 || code == 2601) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }

    // ============================================================
    // query APIs
    // ============================================================

    /**
     * 依 ID 查詢詳情（含 ACABrd 左連結欄位）
     */
    @Override
    public DataDto<Aca2003QueryDto> queryById(GeneralPayload<Aca2003QueryByIdPayload> payload) {
        if (payload == null || payload.getData() == null || payload.getData().getId() == null) {
            return new DataDto<>(null, new ResponseInfo(0, MSG_ID_EMPTY));
        }
        Integer id = payload.getData().getId();

        return repo.findDetailById(id)
                .map(this::toDto)
                .map(dto -> new DataDto<>(dto, new ResponseInfo(1, "查詢成功")))
                .orElseGet(() -> new DataDto<>(null, new ResponseInfo(0, "查無資料")));
    }

    /**
     * 依 ACACardNo 取最新一筆（ID 最大）
     */
    public DataDto<Aca2003QueryDto> queryLatestByCardNo(GeneralPayload<Aca2003QueryByCardPayload> payload) {
        if (payload == null || payload.getData() == null || isBlank(payload.getData().getAcaCardNo())) {
            return new DataDto<>(null, new ResponseInfo(0, MSG_CARD_EMPTY));
        }
        final String card = payload.getData().getAcaCardNo().trim();

        return repo.findLatestDetailByCardNo(card)
                .map(this::toDto)
                .map(dto -> new DataDto<>(dto, new ResponseInfo(1, "查詢成功")))
                .orElseGet(() -> new DataDto<>(null, new ResponseInfo(0, "查無資料")));
    }

    /**
     * 將 Projection 轉為 DTO（Controller 統一輸出此 DTO）
     */
    private Aca2003QueryDto toDto(Aca2003DetailView v) {
        return new Aca2003QueryDto(
                v.getId(),
                v.getCreatedOnDate(),
                v.getCreatedByBranchId(),
                v.getDrgUserText(),
                v.getOprFamilyText(),
                v.getOprFamilyCareText(),
                v.getOprSupportText(),
                v.getOprContactText(),
                v.getOprReferText(),
                v.getAddr(),
                v.getOprAddr(),
                v.getAcaCardNo(),
                v.getAcaName(),
                v.getAcaIdNo()
        );
    }

    // ============================================================
    // delete API：軟刪除
    // ============================================================

    /**
     * 軟刪除（isDeleted=1），保留資料於 DB，不做物理刪除。
     */
    @Override
    @Transactional
    public DataDto<Aca2003SaveResponse> softDelete(GeneralPayload<Aca2003DeletePayload> payload) {
        if (payload == null || payload.getData() == null) {
            return new DataDto<>(null, new ResponseInfo(0, MSG_PAYLOAD_EMPTY));
        }

        var p = payload.getData();
        if (p.getId() == null) return new DataDto<>(null, new ResponseInfo(0, MSG_ID_EMPTY));
        if (p.getUserId() == null) return new DataDto<>(null, new ResponseInfo(0, MSG_USER_ID_EMPTY));

        Optional<AcaDrugUseEntity> opt = repo.findById(p.getId());
        if (opt.isEmpty()) {
            return new DataDto<>(null, new ResponseInfo(0, MSG_DATA_NOT_FOUND));
        }

        var exist = opt.get();
        if (Boolean.TRUE.equals(exist.getIsDeleted())) {
            return new DataDto<>(null, new ResponseInfo(0, MSG_DATA_DELETED));
        }

        exist.setIsDeleted(Boolean.TRUE);
        exist.setModifiedByUserId(p.getUserId());
        exist.setModifiedOnDate(now());
        repo.save(exist);

        return new DataDto<>(new Aca2003SaveResponse(exist.getId()), new ResponseInfo(1, "刪除成功"));
    }

    // ============================================================
    // Helpers：時間、字串、錯誤轉換、回應包裝
    // ============================================================

    /**
     * 統一成功回傳包裝
     */
    private static DataDto<Aca2003SaveResponse> ok(Integer id, String msg) {
        return new DataDto<>(new Aca2003SaveResponse(id), new ResponseInfo(1, msg));
    }

    /**
     * 統一失敗回傳包裝
     */
    private static DataDto<Aca2003SaveResponse> fail(String msg) {
        return new DataDto<>(null, new ResponseInfo(0, msg));
    }

    /**
     * 產生當前時間戳（集中一處，便於日後替換 Clock/TimeProvider）
     */
    private static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trim(String s) {
        return (s == null) ? null : s.trim();
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }


}
