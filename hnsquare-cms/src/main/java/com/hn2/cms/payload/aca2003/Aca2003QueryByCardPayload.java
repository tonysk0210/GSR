package com.hn2.cms.payload.aca2003;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class Aca2003QueryByCardPayload {
    @NotBlank
    private String acaCardNo;
}
