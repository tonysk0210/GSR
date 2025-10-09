package com.hn2.cms.payload.aca4001;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class Aca4001EraseQueryPayload {
    @NotBlank(message = "acaCardNo 不可為空")
    private String acaCardNo;
    private String startDate;
    private String endDate;
}
