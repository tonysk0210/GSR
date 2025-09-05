package com.hn2.cms.payload.report02;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Getter
@Setter
public class Report02Payload {
    @NotNull
    private LocalDate from;
    @NotNull
    private LocalDate to;
}