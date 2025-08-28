package com.hn2.cms.service.aca2003;


import com.hn2.cms.dto.aca2003.Aca2003SaveResponse;
import com.hn2.cms.payload.aca2003.Aca2003SavePayload;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;

public interface Aca2003Service {
    DataDto<Aca2003SaveResponse> save(GeneralPayload<Aca2003SavePayload> payload);
}
