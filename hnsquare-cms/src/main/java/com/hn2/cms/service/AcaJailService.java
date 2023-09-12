package com.hn2.cms.service;

import com.hn2.cms.dto.AcaJailQueryDto;
import com.hn2.cms.payload.AcaJailQueryPayload;
import com.hn2.cms.payload.AcaJailSignPayload;
import com.hn2.cms.payload.AcaJailTransPortPayload;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;

import java.util.List;

public interface AcaJailService {
    DataDto<List<AcaJailQueryDto>> queryList(GeneralPayload<AcaJailQueryPayload> payload);
    DataDto<Void> signList(GeneralPayload<AcaJailSignPayload> payload);
    DataDto<Void> transPort(GeneralPayload<AcaJailTransPortPayload> payload);
}
