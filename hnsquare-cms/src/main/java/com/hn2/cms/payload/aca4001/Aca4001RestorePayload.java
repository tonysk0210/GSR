package com.hn2.cms.payload.aca4001;

import lombok.Data;

@Data
public class Aca4001RestorePayload {
    private String acaCardNo;
    private String restoreReason;
    private String operatorUserId;
}
