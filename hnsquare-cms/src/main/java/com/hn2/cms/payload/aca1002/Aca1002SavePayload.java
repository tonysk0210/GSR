package com.hn2.cms.payload.aca1002;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hn2.cms.model.AcaBrdEntity;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class Aca1002SavePayload {
    /** 項目編號列表 */

    private NamData nam;

    private AcaBrdEntity aca;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NamData {
        private String itemId;

    }

}


