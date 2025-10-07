package com.hn2.cms.service.aca2001;


import com.hn2.cms.payload.aca2001.Aca2001SavePayload;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;

public interface Aca2001Service {
    DataDto<Void> save(GeneralPayload<Aca2001SavePayload> payload);
}
