package com.hn2.cms.dto.aca4001;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Aca4001RestoreQueryDto {
    @JsonProperty("isErased")
    private boolean erased;  // 對應 ACABrd.IsERASE
}