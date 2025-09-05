package com.hn2.cms.dto.aca3001;

import lombok.Data;

import java.time.LocalDate;

import java.util.List;

/**
 * Aca3001 認輔評估表查詢用 DTO
 *
 * 主要分為六個區塊：
 *
 *   Meta：案件基本控制資訊（ID、是否可編輯、鎖定日期）
 *   Header：表頭顯示資訊（分會、通知日期、案件日期）
 *   Profile：個案基本資料（姓名、身分證、卡號）
 *   DirectAdoptCriteria：直接認輔條件（選項與已選清單）
 *   EvalAdoptCriteria：認輔評估條件（選項、已選清單、評估分數）
 *   Summary：總結（服務類型、案件狀態、策略目標）
 *
 */
@Data
public class Aca3001QueryDto {
    private Meta meta;
    private Header header;
    private Profile profile;
    private DirectAdoptCriteria directAdoptCriteria;
    private EvalAdoptCriteria evalAdoptCriteria;
    private Summary summary;

    // ===== Meta：案件基本控制資訊 =====
    @Data
    public static class Meta {
        /** 保護紀錄 ID（必填，對應 ProRec.ID） */
        private String proRecId;
        /** 認輔評估表 ID（可能為 null，表示尚未建立） */
        private Integer proAdoptId;
        /** 是否允許修改（受時間鎖或業務規則影響） */
        private boolean editable = true;
        /** 鎖定日期（若不為 null，超過此日期即不可再修改） */
        private LocalDate lockDate;
    }

    // ===== Header：表頭顯示資訊 =====
    @Data
    public static class Header {
        /** 分會名稱（由 BranchID 對應 Lists.Text） */
        private String branchName;
        /** 通知日期（ProNoticeDate） */
        private String proNoticeDate;
        /** 保護日期（ProDate） */
        private String proDate;
    }

    // ===== Profile：個案基本資料 =====
    @Data
    public static class Profile {
        private String acaName;
        private String acaIdNo;
        private String acaCardNo;
    }

    // ===== DirectAdoptCriteria：直接認輔條件 =====
    @Data
    public static class DirectAdoptCriteria {
        /** 所有可供選擇的直接認輔條件 */
        private List<Option> options = List.of();
        /** 已被勾選的選項（含可能已失效/禁用的選項） */
        private List<Record> records = List.of();
        private boolean hasDiff;

        @Data
        public static class Option {
            private Integer entryId;
            private String value;
            private String text;
            private Integer sortOrder;
        }

        @Data
        public static class Record {
            private Integer entryId;
            private String text;
            /** 是否已選 */
            private boolean isSelected;
        }
    }

    // ===== EvalAdoptCriteria：認輔評估條件 =====
    @Data
    public static class EvalAdoptCriteria {
        /** 所有可供選擇的認輔評估條件 */
        private List<Option> options = List.of();
        /** 已被勾選的條件（含可能已失效/禁用的選項） */
        private List<Record> records = List.of();
        private boolean hasDiff;
        /** 各項分數與總分 */
        private EvalScore evalScores;

        @Data
        public static class Option {
            private Integer entryId;
            private String value;
            private String text;
            private Integer sortOrder;
        }

        @Data
        public static class Record {
            private Integer entryId;
            private String text;
            /** 是否已選 */
            private boolean isSelected;
        }

        /** 認輔評估各面向分數 */
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
            /** 總分（由後端計算，不由前端輸入） */
            private Integer totalScore;
            private String comment;
        }
    }

    // ===== Summary：總結／訪談紀要／策略目標 =====
    @Data
    public static class Summary {
        /** 個案勾選的服務類型 */
        private List<ServiceTypeSelected> serviceTypeSelected = List.of();
        /** 就業狀態 */
        private String proEmploymentStatus;
        /** 個案訪談紀要(輔導情形) */
        private String proStatus;
        /** 案件最終狀態（拒絕/接受/結案等） */
        private CaseStatus caseStatus;

        @Data
        public static class ServiceTypeSelected {
            /** 最末層勾選的 entryId */
            private Integer leafEntryId;
            /** 該路徑的所有節點 entryIds */
            private List<Integer> pathEntryIds = List.of();
            /** 該路徑對應的文字名稱 */
            private List<String> pathText = List.of();
            /** 路徑中是否包含禁用節點 */
            private boolean hasDisabled;
            /** 路徑中是否包含已刪除節點 Optional */
            private boolean hasDeleted;
            /** 歷史 entryIds（若有異動，保留歷史紀錄） */
            private List<Integer> historicalEntryIds = List.of();
        }

        /** 案件狀態（拒絕/接受/結案），僅能擇一 */
        @Data
        public static class CaseStatus {
            public enum CaseState {NONE, REJECT, ACCEPT, END}

            /** 預設為 NONE，表示尚未設定 */
            private CaseState caseState = CaseState.NONE;
            /** 狀態說明原因（如拒絕原因） */
            private String reason = null;
        }
    }
}
