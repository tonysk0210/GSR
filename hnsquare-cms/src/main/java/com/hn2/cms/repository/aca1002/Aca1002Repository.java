package com.hn2.cms.repository.aca1002;

import com.hn2.cms.dto.aca1002.Aca1002QueryDto;
import com.hn2.cms.payload.aca1002.Aca1002QueryPayload;
import com.hn2.core.payload.PagePayload;

import java.util.List;

public interface Aca1002Repository {
    List<Aca1002QueryDto> queryList(Aca1002QueryPayload payload, PagePayload pagePayload);
    Integer countSearch(Aca1002QueryPayload payload);
}
