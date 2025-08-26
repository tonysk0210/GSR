package com.hn2.cms.payload.aca3001;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class Aca3001QueryPayload {
    @NotBlank(message = "proRecId 不可為空!!")
    private String proRecId;
}