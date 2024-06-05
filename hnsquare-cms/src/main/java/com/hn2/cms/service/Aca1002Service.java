package com.hn2.cms.service;

import com.hn2.cms.dto.Aca1002QueryDto;
import com.hn2.cms.payload.aca1002.Aca1002QueryPayload;
import com.hn2.cms.payload.aca1002.Aca1002SignPayload;
import com.hn2.cms.payload.aca1002.Aca1002TransPortPayload;
import com.hn2.cms.payload.aca1002.Aca1002GoBackPayload;
import com.hn2.cms.payload.aca1002.Aca1002ReassignPayload;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;

import java.util.List;

public interface Aca1002Service {
    DataDto<List<Aca1002QueryDto>> queryList(GeneralPayload<Aca1002QueryPayload> payload);
    DataDto<Void> signList(GeneralPayload<Aca1002SignPayload> payload);
    DataDto<Void> transPort(GeneralPayload<Aca1002TransPortPayload> payload);

    DataDto<Void> goBack(GeneralPayload<Aca1002GoBackPayload> payload);

    DataDto<Void> reassign(GeneralPayload<Aca1002ReassignPayload> payload);
}
