package com.hn2.cms.service.aca4001.erase.command;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public final class RestoreCommand {
    private final String acaCardNo;
    private final String operatorUserId;
    private final String operatorIp;
    private final String restoreReason; // 可為 null

    @Builder
    public RestoreCommand(String acaCardNo, String operatorUserId, String operatorIp, String restoreReason) {
        this.acaCardNo = acaCardNo;
        this.operatorUserId = operatorUserId;
        this.operatorIp = operatorIp;
        this.restoreReason = restoreReason;
    }
}