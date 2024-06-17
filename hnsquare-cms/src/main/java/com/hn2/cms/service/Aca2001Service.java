package com.hn2.cms.service;


import com.hn2.cms.payload.Aca2001.Aca2001SavePayload;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;

import java.util.List;

public interface Aca2001Service {
    DataDto<Void> save(GeneralPayload<Aca2001SavePayload> payload);
}
