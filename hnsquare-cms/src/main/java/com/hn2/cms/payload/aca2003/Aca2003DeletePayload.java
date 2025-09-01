package com.hn2.cms.payload.aca2003;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class Aca2003DeletePayload {
    @NotNull
    private Integer id;

    @NotNull
    private Integer userId;
}
