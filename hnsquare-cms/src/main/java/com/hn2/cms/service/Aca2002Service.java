package com.hn2.cms.service;


import com.hn2.cms.dto.Aca2002CrmRecQueryDto;
import com.hn2.cms.payload.Aca2002.Aca2002QueryPayload;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;

public interface Aca2002Service {
    DataDto<Aca2002CrmRecQueryDto> query(GeneralPayload<Aca2002QueryPayload> payload);
}
