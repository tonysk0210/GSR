package com.hn2.cms.dto.aca4001;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class Aca4001EraseQueryDto {
    @JsonProperty("isOver18")
    private boolean isOver18;       // 是否滿18
    @JsonProperty("isLatestProRecClosed")
    private boolean isLatestProRecClosed;      // 是否結案(最新的保護紀錄是否已結案)
    @JsonProperty("isErased")
    private boolean isErased;      // 個案是否已塗銷
    private List<CrmRec> crmRecListBefore18; // 客服紀錄ID清單
    private List<ProRec> proRecListBefore18; // 保護紀錄ID清單

    @Data
    public static class CrmRec {
        private String id;                 // CrmRec.ID
        private String recordDate;      // CreatedOnDate -> 民國 yyy/MM/dd
        private String branchName;         // CreatedByBranchID
        private String jailAgency;         // ProNoticeDep
        private String crimeName1;         // CrmCrime1
        private String crimeName2;         // CrmCrime2
        private String crimeName3;         // CrmCrime3
        private String noJailReason;       // Crm_NoJail
        private String verdictDate;     // Crm_VerdictDate -> 民國 yyy/MM/dd
        private String sentenceType;       // Crm_Sentence
        private String termText;           // CrmTerm
        private String prisonInDate;    // CrmChaDate -> 民國 yyy/MM/dd
        private String releasePlanDate; // Crm_ReleaseDate -> 民國 yyy/MM/dd
        private String prisonOutDate;   // CrmDisDate -> 民國 yyy/MM/dd
        private String prisonOutReason;    // CrmDischarge
        private String remission;          // CrmRemission
        private String trainType;          // CrmTrain
        private String memo;               // CrmMemo
    }

    @Data
    public static class ProRec {
        private String id;             // ProRec.ID
        // 之後可加：private LocalDate proDate; ...
    }


    /**
     * 只在 Service 內使用的小型資料結構
     */
    @Data
    public static class PersonBirth {
        private LocalDate birthDate;       // 只保留日期(避免時間干擾)
        private LocalDate eighteenthStart; // 18歲當天00:00
    }
}
