package com.hn2.cms.service.aca3001;

import com.hn2.cms.dto.aca3001.Aca3001QueryDto;
import com.hn2.cms.payload.aca3001.Aca3001QueryPayload;
import com.hn2.cms.repository.aca3001.Aca3001Repository;
import com.hn2.cms.repository.aca3001.Aca3001RepositoryImpl;
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

        // Meta
        Aca3001QueryDto.Meta meta = new Aca3001QueryDto.Meta();
        meta.setProRecId(proRecId);
        meta.setProAdoptId(proAdoptId);  // 已存在→為正整數；未建立→null
        meta.setEditable(true);          // TODO: 依時間鎖/權限計算
        meta.setLockDate(null);          // TODO: 設定時間鎖日期
        res.setMeta(meta);

        // Header / Profile（既有資料）
        res.setHeader(header);
        res.setProfile(profile);


        if (proAdoptId != null) {
            // ===== 修改情境：撈既有 ProAdopt 的所有區塊 =====
            res.setDirectAdoptCriteria(repo.loadDirectCriteria(proAdoptId));
            res.setEvalAdoptCriteria(repo.loadEvalCriteria(proAdoptId));
            // Summary 由「既有 ACABrd/ProRec/ProDtl」與「ProAdopt case 狀態」組合
            res.setSummary(repo.loadSummary(proRecId, proAdoptId));
            //*
        } else {
            // ===== 新建情境：初始化 ProAdopt 需要的區塊 =====
            // options 從 Lists 來，selected 空、scores 預設 0
            res.setDirectAdoptCriteria(repo.initDirectCriteriaOptions()); // options 有，selected 空
            res.setEvalAdoptCriteria(repo.initEvalCriteriaOptions());     // options 有，selected 空 + evalScore 預設
            // summary 基本欄位來自 ACABrd/ProRec/ProDtl，唯 caseStatus 初始化
            Aca3001QueryDto.Summary summaryBase =
                    java.util.Optional.ofNullable(repo.loadSummaryBasics(proRecId))
                            .orElseGet(Aca3001QueryDto.Summary::new); //對 summaryBase 做安全初始化。
            Aca3001QueryDto.Summary.CaseStatus caseStatus = new Aca3001QueryDto.Summary.CaseStatus();
            Aca3001QueryDto.Summary.StatusFlag off = new Aca3001QueryDto.Summary.StatusFlag();
            off.setFlag(false);
            off.setReason(null);
            caseStatus.setReject(off);
            caseStatus.setAccept(off);
            caseStatus.setEnd(off);
            summaryBase.setCaseStatus(caseStatus);
            res.setSummary(summaryBase);
        }

        return new DataDto<>(res, new ResponseInfo(1, "查詢成功"));
    }


    public DataDto<Aca3001QueryDto> query1(GeneralPayload<Aca3001QueryPayload> payload) {

        Aca3001QueryPayload req = payload.getData(); // { proRecId = "A19880100001" }

        // controller 層已經驗證過 proRecId 不為空
        /*if (req == null || req.getProRecId() == null || req.getProRecId().isBlank()) {
            return new DataDto<>(null, new ResponseInfo(0, "proRecId 不可為空"));
        }*/

        Aca3001QueryDto.Profile row = repo.findProfileByProRecId(req.getProRecId());

        // 若查無資料，回傳 null 並帶上錯誤訊息
        // 注意：這裡的 null 是業務資料，ResponseInfo 仍然會有值
        if (row == null) {
            return new DataDto<>(null, new ResponseInfo(0, "查無資料"));
        }

        // 組裝回傳的 DTO
        // 注意：這裡的 res 是業務資料，ResponseInfo 仍然會有值
        // 這裡的 res 是 Aca3001QueryDto，包含了查詢結果的各個部分
        // 這些部分會在 Aca3001QueryDto 類別中定義
        // 例如：Meta、Profile、Header、DirectAdoptCriteria、EvalAdoptCriteria、Summary 等
        Aca3001QueryDto res = new Aca3001QueryDto();

        // 組裝 Meta 部分
        Aca3001QueryDto.Meta meta = new Aca3001QueryDto.Meta();
        meta.setProRecId(req.getProRecId());
        meta.setProAdoptId(null);
        meta.setEditable(true);
        meta.setLockDate(null);
        res.setMeta(meta);

        // 組裝 profile 部分
        Aca3001QueryDto.Profile profile = new Aca3001QueryDto.Profile();
        profile.setAcaName(row.getAcaName());
        profile.setAcaIdNo(row.getAcaIdNo());
        profile.setAcaCardNo(row.getAcaCardNo());
        res.setProfile(profile);

        // 其他區塊先留空
        res.setHeader(null);
        res.setDirectAdoptCriteria(null);
        res.setEvalAdoptCriteria(null);
        res.setSummary(null);

        return new DataDto<>(res, new ResponseInfo(1, "查詢成功"));
    }
}
