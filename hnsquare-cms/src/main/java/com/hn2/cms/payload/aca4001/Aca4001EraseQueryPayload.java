package com.hn2.cms.payload.aca4001;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class Aca4001EraseQueryPayload {
    @NotBlank(message = "acaCardNo 不可為空")
    private String acaCardNo;

    // 建議用 yyyy-MM-dd；若可為空就不要加 @NotBlank
    private String startDate;
    private String endDate;
}
