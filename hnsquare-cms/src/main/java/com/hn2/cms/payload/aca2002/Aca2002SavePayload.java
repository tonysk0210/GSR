package com.hn2.cms.payload.aca2002;

import com.hn2.cms.model.CrmRecEntity;
import lombok.Data;

@Data
public class Aca2002SavePayload {

    private Nam nam;
    private CrmRecEntity crm;
    @Data
    public static class Nam {
        private String itemId;
    }
}


