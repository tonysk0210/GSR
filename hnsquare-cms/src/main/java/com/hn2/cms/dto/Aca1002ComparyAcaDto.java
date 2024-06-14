package com.hn2.cms.dto;

import com.hn2.cms.model.AcaBrdEntity;
import com.hn2.cms.model.SupAfterCareEntity;
import lombok.Data;

import java.time.LocalDate;

@Data
public class Aca1002ComparyAcaDto {
    /** 項目編號 */
    private SupAfterCareEntity nam;
    /** 承辦人簽收狀態 */
    private AcaBrdEntity aca;

}
