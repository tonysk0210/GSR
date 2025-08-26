package com.hn2.cms.service.aca3001;

import com.hn2.cms.dto.aca3001.Aca3001QueryDto;
import com.hn2.cms.dto.aca3001.Aca3001SaveResponse;
import com.hn2.cms.payload.aca3001.Aca3001DeletePayload;
import com.hn2.cms.payload.aca3001.Aca3001QueryPayload;
import com.hn2.cms.payload.aca3001.Aca3001SavePayload;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;

import javax.validation.Valid;

public interface Aca3001Service {
    DataDto<Aca3001QueryDto> query(GeneralPayload<Aca3001QueryPayload> payload);

    DataDto<Aca3001SaveResponse> save(GeneralPayload<Aca3001SavePayload> payload);

    DataDto<Void> delete(GeneralPayload<Aca3001DeletePayload> payload);
}
