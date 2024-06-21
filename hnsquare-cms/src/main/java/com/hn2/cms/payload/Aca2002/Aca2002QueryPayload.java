package com.hn2.cms.payload.Aca2002;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class Aca2002QueryPayload {
    @NotEmpty(message = "主鍵值不可為空")
    private String acaCardNo;

}


