package com.hn2.cms.dto.aca4001;

import lombok.Data;

import java.util.List;

@Data
public class Aca4001AuditQueryDto {
    /**
     * 是否為彙總後的結果（READ-side 聚合）
     */
    private boolean aggregated;

    /**
     * 彙總後的群組清單
     */
    private List<Group> groups;

    @Data
    public static class Group {
        private String acaCardNo;
        private String action;
        private Boolean isErased;
        private Integer docNum;      // 可能為 null
        private String reason;
        private Integer recordCount; // 我們會在 SQL CAST 成 int
        private java.sql.Timestamp createdOn;
        private String userId;       // 用字串最安全
        private String userName;
        private String userIp;
    }
}
