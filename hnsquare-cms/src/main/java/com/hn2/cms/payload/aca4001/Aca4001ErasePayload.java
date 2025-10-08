package com.hn2.cms.payload.aca4001;

import lombok.Data;

import java.util.List;

@Data
public class Aca4001ErasePayload {
    private String acaCardNo;
    private Integer docNum;
    private String eraseReason;
    private List<String> selectedProRecIds;
    private List<String> selectedCrmRecIds;
    private Boolean isOver18;
    private Integer operatorUserId;
}
