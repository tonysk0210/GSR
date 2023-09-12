package com.hn2.cms.repository;

import com.hn2.cms.dto.AcaJailQueryDto;
import com.hn2.cms.payload.AcaJailQueryPayload;
import com.hn2.core.payload.PagePayload;

import java.util.List;

public interface AcaJailRepository {
    List<AcaJailQueryDto> queryList(AcaJailQueryPayload payload, PagePayload pagePayload);
    Integer countSearch(AcaJailQueryPayload payload);
}
