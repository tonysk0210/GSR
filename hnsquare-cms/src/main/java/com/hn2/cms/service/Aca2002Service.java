package com.hn2.cms.service;


import com.hn2.cms.dto.Aca2002CrmRecQueryDto;
import com.hn2.cms.payload.aca2002.Aca2002QueryListPayload;
import com.hn2.cms.payload.aca2002.Aca2002QueryPayload;
import com.hn2.cms.payload.aca2002.Aca2002SavePayload;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;

import java.util.List;

public interface Aca2002Service {
    DataDto<Aca2002CrmRecQueryDto> query(GeneralPayload<Aca2002QueryPayload> payload);

    DataDto<List<Aca2002CrmRecQueryDto>> queryList(GeneralPayload<Aca2002QueryListPayload> payload);
    DataDto<Object> save(GeneralPayload<Aca2002SavePayload> payload);
}
