package com.hn2.cms.service.aca4001.erase.rules.tableConfig;

import com.hn2.cms.service.aca4001.erase.rules.EraseTableConfigPojo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Configuration
public class ProjectDrugOpenConfig {

    @Bean
    public EraseTableConfigPojo projectDrugOpenRule() {
        EraseTableConfigPojo r = new EraseTableConfigPojo();
        r.setSchema("dbo");
        r.setTable("ProjectDrugOpen");
        r.setIdColumn("ID");

        // 子表設定：隸屬 ProRec，外鍵 ProRecID
        r.setParentTable("ProRec");
        r.setParentFkColumn("ProRecID");

        // 對應原 Target 的白名單欄位
        r.setWhitelist(List.of(
                "CrmProNoticeDep",
                "CrmCrime",
                "CrmTerm",
                "CrmDisDate",
                "CrmDischarge",
                "Crime",
                "CrimeMemo",
                "Drug",
                "DrugMemo",
                "BehaviorPeriod",
                "BehaviorTimes",
                "HarmReduction",
                "DrugBehavior",
                "OtherMemo",
                "CorrectionRepeat",
                "CorrectionEffect",
                "FaiAppellation",
                "FaiCardNo",
                "FaiName",
                "FaiTel",
                "FaiAddress",
                "IsContact",
                "FileID",
                "FaiDrug",
                "FaiMemo",
                "FaiStatus",
                "FaiSupport",
                "FaiSupportMemo",
                "SocSupport",
                "SocSupportFriend",
                "SocSupportBoss",
                "SocSupportOther",
                "SocSupportAnalysis",
                "PreJob",
                "PreJobMemo",
                "Job",
                "JobMemo",
                "Live",
                "LiveMemo",
                "PsyStatus",
                "IsMeasures",
                "MeasuresMemo",
                "CloseReason",
                "ElseReason",
                "CreatedByUserID",
                "ModifiedByUserID"
        ));

        // 日期/整數正規化設定（依原 Target）
        r.setDateCols(Set.of("CrmDisDate"));
        r.setIntCols(Set.of(
                "CreatedByUserID",
                "ModifiedByUserID",
                "BehaviorTimes",
                "FileID"
        ));
        // 若 IsContact / IsMeasures 是 BIT 且 NOT NULL，可依你的 schema 改成在 eraseSet 指定 0

        // 清空策略：白名單未覆寫者預設設為 NULL；以下為固定值覆寫
        var eraseSet = new LinkedHashMap<String, Object>();
        eraseSet.put("CreatedByUserID", -2);
        eraseSet.put("ModifiedByUserID", -2);
        eraseSet.put("isERASE", 1);
        eraseSet.put("ModifiedOnDate", "${NOW}");
        r.setEraseExtraSet(eraseSet);

        // 還原時的追加欄位
        var restoreExtra = new LinkedHashMap<String, Object>();
        restoreExtra.put("isERASE", 0);
        restoreExtra.put("ModifiedOnDate", "${NOW}");
        restoreExtra.put("ModifiedByUserID", ":uid"); // 由程式綁操作者
        r.setRestoreExtraSet(restoreExtra);

        return r;
    }
}
