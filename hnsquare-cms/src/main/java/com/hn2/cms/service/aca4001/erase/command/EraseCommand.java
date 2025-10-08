package com.hn2.cms.service.aca4001.erase.command;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@ToString
public final class EraseCommand {
    private final String acaCardNo;
    private final Map<String, List<String>> tableToIds;
    private final String operatorUserId;
    private final String operatorIp;
    private final Integer docNum;       // 可為 null
    private final String eraseReason;   // 可為 null

    @Builder
    public EraseCommand(
            String acaCardNo,
            @Singular("tableIds") Map<String, List<String>> tableToIds,
            String operatorUserId,
            String operatorIp,
            Integer docNum,
            String eraseReason
    ) {
        this.acaCardNo = acaCardNo;
        this.tableToIds = (tableToIds == null)
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(tableToIds);
        this.operatorUserId = operatorUserId;
        this.operatorIp = operatorIp;
        this.docNum = docNum;
        this.eraseReason = eraseReason;
    }

    public List<String> idsOf(String table) {
        List<String> v = tableToIds.get(table);
        return v == null ? Collections.emptyList() : Collections.unmodifiableList(v);
    }
}