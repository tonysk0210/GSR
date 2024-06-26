package com.hn2.cms.payload.Aca2002;

import com.hn2.cms.model.CrmRecEntity;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class Aca2002SavePayload {

    private Nam nam;
    private CrmRecEntity crm;
    @Data
    public static class Nam {
        private String itemId;
    }
}


