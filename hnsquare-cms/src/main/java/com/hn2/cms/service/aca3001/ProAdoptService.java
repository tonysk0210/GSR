package com.hn2.cms.service.aca3001;

import com.hn2.cms.dto.aca3001.ProAdoptViewResponse;
import com.hn2.cms.repository.aca3001.ProAdoptRepository;
import org.springframework.stereotype.Service;

@Service
public class ProAdoptService {

    private final ProAdoptRepository repo;

    public ProAdoptService(ProAdoptRepository repo) {
        this.repo = repo;
    }

    public ProAdoptViewResponse buildProfileOnlyView(String proRecId) {
        // 1) 先用 proRecId 找出 Profile 所需三欄
        ProAdoptRepository.ProfileRow row = repo.findProfileByProRecId(proRecId);
        if (row == null) return null;

        // 2) 組回傳
        ProAdoptViewResponse res = new ProAdoptViewResponse();

        ProAdoptViewResponse.Meta meta = new ProAdoptViewResponse.Meta();
        meta.setProRecId(proRecId);
        meta.setProAdoptId(null);  // 還沒建/查 ProAdopt 就先 null
        meta.setEditable(true);    // 先給 true，之後可依“時間鎖”判斷
        meta.setLockDate(null);    // 之後再補
        res.setMeta(meta);

        ProAdoptViewResponse.Profile profile = new ProAdoptViewResponse.Profile();
        profile.setAcaName(row.getAcaName());
        profile.setAcaIdNo(row.getAcaIdNo());
        profile.setAcaCardNo(row.getAcaCardNo());
        res.setProfile(profile);

        // 其他區塊先不給（null），前端好判斷「尚未載入」
        res.setHeader(null);
        res.setDirectAdoptCriteria(null);
        res.setEvalAdoptCriteria(null);
        res.setSummary(null);

        return res;
    }
}
