package com.hn2.cms.repository.aca4001;

import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto;
import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto.PersonBirth;

import java.time.LocalDateTime;
import java.util.List;

public interface Aca4001Repository {

    /**
     * 依 ACACardNo 取生日與 18 歲門檻日（若找不到回傳 null）
     */
    PersonBirth findPersonBirth(String acaCardNo);

    /**
     * 取 18 歲前的 ProRec.ID（可選日期區間半開 [startTs, endExclusive)）
     */
    List<String> findProRecIdsBefore18(String acaCardNo,
                                       LocalDateTime eighteenthStart,
                                       LocalDateTime startTs,
                                       LocalDateTime endExclusive);

    public List<Aca4001EraseQueryDto.CrmRec> findCrmRecsByIds(List<String> ids);


    /**
     * 取 18 歲前的 CrmRec.ID（可選日期區間半開 [startTs, endExclusive)）
     */
    List<String> findCrmRecIdsBefore18(String acaCardNo,
                                       LocalDateTime eighteenthStart,
                                       LocalDateTime startTs,
                                       LocalDateTime endExclusive);

    public List<Aca4001EraseQueryDto.ProRec> findProRecsByIds(List<String> ids);

    public Boolean findLatestProRecClosed(String acaCardNo);

    public Boolean findPersonErased(String acaCardNo);
}
