package com.hn2.cms.service;

import com.hn2.cms.dto.Aca1001QueryDto;
import com.hn2.cms.payload.aca1001.Aca1001QueryPayload;
import com.hn2.cms.payload.aca1001.Aca1001SignPayload;
import com.hn2.cms.payload.aca1001.Aca1001TransPortPayload;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;

import java.util.List;

public interface Aca1001Service {
    DataDto<List<Aca1001QueryDto>> queryList(GeneralPayload<Aca1001QueryPayload> payload);
    DataDto<Void> signList(GeneralPayload<Aca1001SignPayload> payload);
    DataDto<Void> transPort(GeneralPayload<Aca1001TransPortPayload> payload);
}
