package com.hn2.cms.repository.aca3001;

import com.hn2.cms.dto.aca3001.Aca3001QueryDto;


public interface Aca3001Repository {

    Integer findProAdoptIdByProRecId(String proRecId);

    Aca3001QueryDto.Meta computeMeta(String proRecId, Integer proAdoptId);

    Aca3001QueryDto.Header computeHeader(String proRecId);

    Aca3001QueryDto.Profile computeProfile(String proRecId);

    Aca3001QueryDto.DirectAdoptCriteria computeDirectAdoptCriteria(Integer proAdoptId);

    Aca3001QueryDto.EvalAdoptCriteria computeEvalAdoptCriteria(Integer proAdoptId);

    Aca3001QueryDto.Summary computeSummary(String proRecId, Integer proAdoptId);
}
