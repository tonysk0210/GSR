package com.hn2.cms.payload.aca3001;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class Aca3001DeletePayload {
    @NotBlank
    private String proRecId;
}
