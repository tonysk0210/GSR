package com.hn2.cms.repository.aca1001;

import com.hn2.cms.dto.aca1001.Aca1001QueryDto;
import com.hn2.cms.payload.aca1001.Aca1001QueryPayload;
import com.hn2.core.payload.PagePayload;

import java.util.List;

public interface Aca1001Repository {
    List<Aca1001QueryDto> queryList(Aca1001QueryPayload payload, PagePayload pagePayload);
    Integer countSearch(Aca1001QueryPayload payload);
}
