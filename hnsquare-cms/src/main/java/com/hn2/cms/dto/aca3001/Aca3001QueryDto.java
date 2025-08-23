package com.hn2.cms.dto.aca3001;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class Aca3001QueryDto {
    private Meta meta;
    private Header header;
    private Profile profile;
    private DirectAdoptCriteria directAdoptCriteria;
    private EvalAdoptCriteria evalAdoptCriteria;
    private Summary summary;

    // --------- Meta 區塊 ----------
    @Data
    public static class Meta {
        private String proRecId; // 保護紀錄編號
        private Integer proAdoptId; // 認輔評估表 ID（若未建立則 null）
        private boolean editable;    // 是否可編輯（依時間鎖決定）
        private LocalDate lockDate;     // 鎖定日期（null 表示尚未鎖）
    }

    // --------- Header 區塊 ----------
    @Data
    public static class Header {
        private String branchName;    // 分會名稱
        private String proNoticeDate; // 申請通知日期
        private String proDate;       // 保護日期
    }

    // --------- Profile 區塊 ----------
    @Data
    public static class Profile {
        private String acaName;   // 更生人姓名
        private String acaIdNo;   // 身分證字號
        private String acaCardNo; // 建檔編號
    }

    // --------- DirectAdoptCriteria (直接認輔條件) ----------
    @Data
    public static class DirectAdoptCriteria {
        private List<Option> options = List.of();    // 可選清單（從 Lists 來）
        private List<Selected> selected = List.of(); // 已勾選的條件

        @Data
        public static class Option {
            private Integer entryId;
            private String value;
            private String text;
            private Integer sortOrder;
        }

        @Data
        public static class Selected {
            private Integer entryId;
            private String value;
            private String text;
            private boolean isDisabled; // 是否已被法規刪除/禁用
        }
    }

    // --------- EvalAdoptCriteria (評估條件) ----------
    @Data
    public static class EvalAdoptCriteria {
        private List<Option> options;    // 可選清單
        private List<Selected> selected; // 已勾選
        private EvalScore evalScores;           // 九項評估分數

        @Data
        public static class Option {
            private Integer entryId;
            private String value;
            private String text;
            private Integer sortOrder;
        }

        @Data
        public static class Selected {
            private Integer entryId;
            private String value;
            private String text;
            private boolean isDisabled;
        }

        @Data
        public static class EvalScore {
            private Integer scoreEconomy;
            private Integer scoreEmployment;
            private Integer scoreFamily;
            private Integer scoreSocial;
            private Integer scorePhysical;
            private Integer scorePsych;
            private Integer scoreParenting;
            private Integer scoreLegal;
            private Integer scoreResidence;
            private Integer totalScore; // 後端計算
            private String comment;
        }
    }

    // --------- Summary (總結/訪談紀要、策略目標) ----------
    @Data
    public static class Summary {
        //        private List<ServiceTypeOption> serviceTypeOptions = List.of();
        private List<ServiceTypeSelected> serviceTypeSelected = List.of();
        private String proEmploymentStatus; // 若為代碼建議改為 value+text
        private String proStatus;           // 同上
        private CaseStatus caseStatus;

        @Data
        public static class ServiceTypeSelected {
            private Integer leafEntryId;
            private List<Integer> pathEntryIds = List.of();
            private List<String> pathText = List.of();
            private boolean hasDisabled;           // 路徑是否含禁用節點(預設)
            private boolean hasDeleted;            // 路徑是否含已刪除節點
            private List<Integer> historicalEntryIds = List.of(); // lazy 時可為空陣列
        }

        @Data
        public static class CaseStatus {
            private StatusFlag reject;
            private StatusFlag accept;
            private StatusFlag end;
        }

        @Data
        public static class StatusFlag {
            private boolean flag;
            private String reason;
        }
    }
}
