package com.hn2.cms.service.aca3001;

import com.hn2.cms.dto.aca3001.Aca3001QueryDto;
import com.hn2.cms.payload.aca3001.Aca3001QueryPayload;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;

public interface Aca3001Service {
    DataDto<Aca3001QueryDto> query(GeneralPayload<Aca3001QueryPayload> payload);
}
