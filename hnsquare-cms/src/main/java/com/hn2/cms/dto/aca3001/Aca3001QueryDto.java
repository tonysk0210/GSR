package com.hn2.cms.dto.aca3001;

import lombok.Data;

@Data
public class Aca3001QueryDto {
    private Meta meta;
    private Header header;          // 先不填，可為 null
    private Profile profile;
    private Object directAdoptCriteria; // 先 null
    private Object evalAdoptCriteria;   // 先 null
    private Object summary;             // 先 null

    // getters/setters

    @Data
    public static class Meta {
        private String proRecId;
        private Integer proAdoptId; // 目前未知就回 null
        private boolean editable;    // 測試先給 true
        private String lockDate;     // 先 null
        // getters/setters
    }

    @Data
    public static class Header {
        private String branchName;
        private String proNoticeDate;
        private String proDate;
        // getters/setters
    }

    @Data
    public static class Profile {
        private String acaName;
        private String acaIdNo;
        private String acaCardNo;
        // getters/setters
    }
}