package com.hn2.cms.service.aca2003;

import com.hn2.cms.dto.aca2003.Aca2003SaveResponse;
import com.hn2.cms.model.aca2003.AcaDrugUseEntity;
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

@Service
public class Aca2003ServiceImpl implements Aca2003Service {

    private final Aca2003Repository repo;

    @Autowired
    public Aca2003ServiceImpl(Aca2003Repository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public DataDto<Aca2003SaveResponse> save(GeneralPayload<Aca2003SavePayload> payload) {
        // ---- 0) 取值與基本檢核 ----
        if (payload == null || payload.getData() == null) {
            return fail("payload 不可為空");
        }

        var p = payload.getData();
        if (p.getUserId() == null) return fail("userId 不可為空");
        if (isBlank(p.getAcaCardNo())) return fail("acaCardNo 不可為空");
        if (isBlank(p.getProRecId())) return fail("proRecId 不可為空");

        final String card = p.getAcaCardNo().trim();
        final String rec = p.getProRecId().trim();
        final Timestamp now = new Timestamp(System.currentTimeMillis());

        try {
            // ---- 1) 新增：只允許在「沒有有效重複」時新增 ----
            if (p.getId() == null) {
                // 「是否存在一筆 ACACardNo = ? 且 ProRecId = ?，並且沒有被刪除的紀錄？」
                if (repo.countActive(card, rec) > 0) {
                    return fail("相同「個案編號」+「保護紀錄編號」的有效資料已存在");
                }
                var e = new AcaDrugUseEntity();
                copyFieldsForCreate(p, e);        // 含鍵值 ACACardNo, ProRecId
                e.setCreatedByUserId(p.getUserId());
                e.setCreatedOnDate(now);
                e.setIsDeleted(Boolean.FALSE);
                e.setCreatedByBranchId(repo.findCreatedByBranchIdByAcaCardNo(card)); // 由 ACABrd 帶入
                e = repo.save(e);
                return ok(e.getId(), "新增成功");
            }

            // ---- 2) 更新：只更新那一筆有效資料；鍵值不可變動 ----
            Optional<AcaDrugUseEntity> opt = repo.findById(p.getId());
            if (opt.isEmpty()) return fail("指定資料不存在");
            var exist = opt.get();
            if (Boolean.TRUE.equals(exist.getIsDeleted())) {
                return fail("指定資料已刪除");
            }

            // 前端帶的 ID 與 (ACACardNo, ProRecId) 必須一致
            if (!card.equals(exist.getAcaCardNo()) || !rec.equals(exist.getProRecId())) {
                // 指定訊息格式：「個案編號ACACardNo, 無此保護紀錄資料(ProRecId)」
                return fail("ID 與指定的個案編號(" + card + ")、保護紀錄(" + rec + ")不一致");
            }

            // 僅覆寫非鍵值欄位
            copyFieldsForUpdate(p, exist);
            exist.setModifiedByUserId(p.getUserId());
            exist.setModifiedOnDate(now);
            repo.save(exist);
            return ok(p.getId(), "更新成功");

        } catch (DataIntegrityViolationException ex) {
            // 若你在 DB 有建立「過濾式唯一索引」，併發時仍可能由 DB 丟出；翻成友善訊息
            if (isUniqueKeyViolation(ex)) {
                return fail("相同「個案編號」+「保護紀錄編號」的有效資料已存在");
            }
            throw ex;
        }
    }

    // ---- helpers ----
    private static DataDto<Aca2003SaveResponse> ok(Integer id, String msg) {
        return new DataDto<>(new Aca2003SaveResponse(id), new ResponseInfo(1, msg));
    }

    private static DataDto<Aca2003SaveResponse> fail(String msg) {
        return new DataDto<>(null, new ResponseInfo(0, msg));
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

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

    private static String trim(String s) {
        return (s == null) ? null : s.trim();
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // 兜底辨識 SQL Server 唯一衝突（2627 unique constraint；2601 duplicate index）
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

}
