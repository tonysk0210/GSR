package com.hn2.cms.payload.aca2003;

import lombok.Data;

@Data
public class Aca2003SavePayload {
    private Integer id;                 // null=新增, 非null=更新
    private String acaCardNo;           // ACACardNo
    private String drgUserText;
    private String oprFamilyText;
    private String oprFamilyCareText;
    private String oprSupportText;
    private String oprContactText;
    private String oprReferText;
    private String addr;
    private String oprAddr;
    private Integer userId;             // 操作者ID：新增→CreatedByUserID；更新→ModifiedByUserID
}
