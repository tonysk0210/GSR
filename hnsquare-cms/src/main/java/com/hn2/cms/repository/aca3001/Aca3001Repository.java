package com.hn2.cms.repository.aca3001;

import com.hn2.cms.dto.aca3001.Aca3001QueryDto;


public interface Aca3001Repository {

    // 基本存在性與鍵值
    Integer findProAdoptIdByProRecId(String proRecId);

    // 既存資料（ACABrd/ProRec/ProDtl）
    Aca3001QueryDto.Profile findProfileByProRecId(String proRecId);
    Aca3001QueryDto.Header  findHeaderByProRecId(String proRecId);
    Aca3001QueryDto.Summary loadSummaryBasics(String proRecId);

    // ProAdopt（已存在 → 修改）
    Aca3001QueryDto.DirectAdoptCriteria loadDirectCriteria(Integer proAdoptId);  // 你可以回傳 Aca3001QueryDto.DirectAdoptCriteria
    Aca3001QueryDto.EvalAdoptCriteria loadEvalCriteria(Integer proAdoptId);    // 回傳 Aca3001QueryDto.EvalAdoptCriteria
    Aca3001QueryDto.Summary.CaseStatus loadCaseStatus (Integer proAdoptId); // 回傳 Aca3001QueryDto.Summary.CaseStatus


    // ProAdopt（不存在 → 新建初始化）
    Aca3001QueryDto.DirectAdoptCriteria initDirectCriteriaOptions(); // options from Lists, selected = []
    Aca3001QueryDto.EvalAdoptCriteria initEvalCriteriaOptions();   // options from Lists, selected = [], evalScore defaults

}
