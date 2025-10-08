package com.hn2.cms.dto.aca4001;

import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class Aca4001AuditQueryDto {
    private List<Row> items;

    @Data
    public static class Row {
        private Long auditId;
        private Timestamp createdOn;
        private String acaCardNo;
        private String action;        // ERASE / RESTORE
        private Integer docNum;       // 可能為 null
        private String eraseReason;   // ERASE 才會有
        private String restoreReason; // RESTORE 才會有
        private String userId;        // 原始 CreatedByUserID（用字串安全）
        private String userName;      // DNN Users.DisplayName
        private String userIp;
    }
}
