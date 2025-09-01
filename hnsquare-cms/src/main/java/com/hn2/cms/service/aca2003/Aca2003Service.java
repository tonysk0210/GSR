package com.hn2.cms.service.aca2003;


import com.hn2.cms.dto.aca2003.Aca2003DetailView;
import com.hn2.cms.dto.aca2003.Aca2003QueryDto;
import com.hn2.cms.dto.aca2003.Aca2003SaveResponse;
import com.hn2.cms.payload.aca2003.Aca2003DeletePayload;
import com.hn2.cms.payload.aca2003.Aca2003QueryByCardPayload;
import com.hn2.cms.payload.aca2003.Aca2003QueryPayload;
import com.hn2.cms.payload.aca2003.Aca2003SavePayload;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;

public interface Aca2003Service {
    DataDto<Aca2003SaveResponse> save(GeneralPayload<Aca2003SavePayload> payload);

    // 新增：依 ID 查詢詳情
    DataDto<Aca2003QueryDto> queryById(GeneralPayload<Aca2003QueryPayload> payload);

    DataDto<Aca2003QueryDto> queryLatestByCardNo(GeneralPayload<Aca2003QueryByCardPayload> payload);

    DataDto<Aca2003SaveResponse> delete(GeneralPayload<Aca2003DeletePayload> payload);
}
