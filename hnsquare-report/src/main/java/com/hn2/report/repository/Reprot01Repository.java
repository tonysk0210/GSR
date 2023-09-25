package com.hn2.report.repository;

import com.hn2.report.dto.Insert_to_SUP_AfterCare_Print_Log_DTO;
import com.hn2.report.dto.Reprot01Dto;
import com.hn2.report.payload.Reprot01Payload;

import java.util.List;

public interface Reprot01Repository {
    List<Reprot01Dto> getList(Reprot01Payload payload);

    void insertToSUP_AfterCare_Print_Log( List<Insert_to_SUP_AfterCare_Print_Log_DTO> insertDTOList);
}
