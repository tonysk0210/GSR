package com.hn2.cms.payload.aca4001;

import lombok.Data;

import java.util.List;

@Data
public class Aca4001ErasePayload {

    private String acaCardNo;
    private Integer docNum;
    private String eraseReason;
    // 這次先用 CrmRec，ProRec 先保留不使用
    private List<String> selectedProRecIds;
    private List<String> selectedCrmRecIds;
    private Boolean isOver18;

    // 新增
    private String operatorUserId;
    private String operatorUserName;
    private String operatorBranchId;
}
