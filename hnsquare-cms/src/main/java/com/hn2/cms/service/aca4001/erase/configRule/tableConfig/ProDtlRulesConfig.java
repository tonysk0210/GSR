package com.hn2.cms.service.aca4001.erase.configRule.tableConfig;

import com.hn2.cms.service.aca4001.erase.configRule.EraseRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Configuration
public class ProDtlRulesConfig {

    @Bean
    public EraseRule proDtlRule() {
        EraseRule r = new EraseRule();
        r.setSchema("dbo");
        r.setTable("ProDtl");
        r.setIdColumn("ID");
        r.setParentTable("ProRec");        // 子表歸屬
        r.setParentFkColumn("ProRecID");   // 父鍵欄位
        r.setWhitelist(List.of(
                "ProCost",
                "ProDtlDate",
                "ProDtlEndMemo",
                "ProDtlTrackOther",
                "ProPlace",
                "ProMaterial_G",
                "IsTrainingCompleted",
                "HasLicenses",
                "ProContent",
                "InterviewPlace",
                "CreatedByUserID",
                "ModifiedByUserID"
        ));
        r.setDateCols(Set.of("ProDtlDate"));
        r.setIntCols(Set.of("CreatedByUserID", "ModifiedByUserID"));

        // 清空策略（若 DB NOT NULL，IsTrainingCompleted/HasLicenses 可視需求改 0）
        var eraseSet = new LinkedHashMap<String, Object>();
        eraseSet.put("CreatedByUserID", -2);
        eraseSet.put("ModifiedByUserID", -2);
        eraseSet.put("isERASE", 1);
        eraseSet.put("ModifiedOnDate", "${NOW}");
        r.setEraseSet(eraseSet);

        // 還原時的追加欄位
        var restoreExtra = new LinkedHashMap<String, Object>();
        restoreExtra.put("isERASE", 0);
        restoreExtra.put("ModifiedOnDate", "${NOW}");
        restoreExtra.put("ModifiedByUserID", ":uid"); // 由程式帶入操作者
        r.setRestoreExtraSet(restoreExtra);

        return r;
    }
}