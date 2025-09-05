package com.hn2.cms.payload.aca3001;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

@Data
public class Aca3001SavePayload {

    @NotBlank
    private String proRecId;

    // 由前端帶；null=新增、非null=更新
    private Integer proAdoptId;

    @NotNull @Valid
    private Scores scores;

    @NotNull @Valid
    private CaseStatus caseStatus;

    @NotNull
    private List<Integer> directSelectedEntryIds; // 可為空陣列，但非null

    @NotNull
    private List<Integer> evalSelectedEntryIds;   // 可為空陣列，但非null

    // 新增：是否刷新 EntryText 快照（mod 情境）
    private Boolean refreshSnapshot;  // 可為 null；null 視為 false

    private Audit audit;

    @Data
    public static class Scores {
        @Min(0) @Max(4) private Integer  economy;
        @Min(0) @Max(4) private Integer  employment;
        @Min(0) @Max(4) private Integer  family;
        @Min(0) @Max(4) private Integer  social;
        @Min(0) @Max(4) private Integer  physical;
        @Min(0) @Max(4) private Integer  psych;
        @Min(0) @Max(4) private Integer  parenting;
        @Min(0) @Max(4) private Integer  legal;
        @Min(0) @Max(4) private Integer  residence;
        @Size(max = 300) private String comment;
    }

    @Data
    public static class CaseStatus {
        @NotNull
        private State state;   // NONE / REJECT / ACCEPT / END
        @Size(max = 300) private String reason;
        public enum State { NONE, REJECT, ACCEPT, END }
    }

    @Data
    public static class Audit {
        private Integer userId;
    }
}
