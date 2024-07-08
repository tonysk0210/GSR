package com.hn2.cms.payload.aca2001;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hn2.cms.model.AcaBrdEntity;
import lombok.Data;

@Data
public class Aca2001SavePayload {
    /** 項目編號列表 */

    private NamData nam;

    private AcaBrdEntity aca;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NamData {
        private String itemId;

    }

}


