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

@Service
public class Aca3001ServiceImpl implements Aca3001Service {

    private final Aca3001Repository repo;

    @Autowired
    public Aca3001ServiceImpl(Aca3001Repository repo) {
        this.repo = repo;
    }

    public DataDto<Aca3001QueryDto> query(GeneralPayload<Aca3001QueryPayload> payload) {
        Aca3001QueryPayload req = payload.getData();
        if (req == null || req.getProRecId() == null || req.getProRecId().isBlank()) {
            return new DataDto<>(null, new ResponseInfo(0, "proRecId 不可為空"));
        }

        Aca3001RepositoryImpl.ProfileRow row = repo.findProfileByProRecId(req.getProRecId());
        if (row == null) {
            return new DataDto<>(null, new ResponseInfo(0, "查無資料"));
        }

        Aca3001QueryDto res = new Aca3001QueryDto();
        Aca3001QueryDto.Meta meta = new Aca3001QueryDto.Meta();
        meta.setProRecId(req.getProRecId());
        meta.setProAdoptId(null);
        meta.setEditable(true);
        meta.setLockDate(null);
        res.setMeta(meta);

        Aca3001QueryDto.Profile profile = new Aca3001QueryDto.Profile();
        profile.setAcaName(row.getAcaName());
        profile.setAcaIdNo(row.getAcaIdNo());
        profile.setAcaCardNo(row.getAcaCardNo());
        res.setProfile(profile);

        res.setHeader(null);
        res.setDirectAdoptCriteria(null);
        res.setEvalAdoptCriteria(null);
        res.setSummary(null);

        return new DataDto<>(res, new ResponseInfo(1, "查詢成功"));
    }
}
