package com.hn2.cms.dto.report02;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report02Dto {
    private Range range;
    private List<Item> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Range {
        private LocalDate from;
        private LocalDate to;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private String branchCode;
        private String branchName;
        private Integer sortOrder;
        private List<Org> orgs;
        private Totals totals;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Org {
        private String orgCode;
        private String orgName;
        private int pendingCount; // SIGN_STATE=0
        private int signedCount;  // SIGN_STATE=1
        private int caseCount;    // SIGN_STATE=3
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Totals {
        private int pendingCount;
        private int signedCount;
        private int caseCount;
        private int orgCount;
    }

    // Repository 用的扁平彙整列
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FlatRow {
        private String branchCode;
        private String branchName;
        private Integer sortOrder;
        private String orgCode;
        private String orgName;
        private int pendingCount;
        private int signedCount;
        private int caseCount;
    }
}