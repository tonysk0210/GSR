package com.hn2.cms.service.aca3001;

import com.hn2.cms.dto.aca3001.Aca3001QueryDto;
import com.hn2.cms.payload.aca3001.Aca3001QueryPayload;
import com.hn2.cms.repository.aca3001.Aca3001Repository;

import com.hn2.core.dto.DataDto;
import com.hn2.core.dto.ResponseInfo;
import com.hn2.core.payload.GeneralPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
