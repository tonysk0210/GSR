package com.hn2.cms.service.report02;

import com.hn2.cms.dto.report02.Report02Dto;
import com.hn2.cms.payload.report02.Report02Payload;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;


public interface Report02Service {

    // add this overload to match the controller pattern
    DataDto<Report02Dto> query(GeneralPayload<Report02Payload> payload);
}
