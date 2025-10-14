package com.hn2.cms.service.aca4001.erase.rules.tableConfig;

import com.hn2.cms.service.aca4001.erase.rules.EraseTableConfigPojo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Configuration
public class ProDtlConfig {

    @Bean
    public EraseTableConfigPojo proDtlRule() {
        EraseTableConfigPojo r = new EraseTableConfigPojo();
        r.setSchema("dbo");
        r.setTable("ProDtl");
        r.setIdColumn("ID");

        // 設定這張表的父表資訊 → 說明它是「ProRec」的子表
        r.setParentTable("ProRec");        // 所屬父表名稱
        r.setParentFkColumn("ProRecID");   // 在本表中對應父表的外鍵欄位

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
        // 指定日期欄位（用於格式正規化，避免字串轉日期錯誤）
        r.setDateCols(Set.of("ProDtlDate"));
        // 指定整數欄位（確保字串轉整數正確）
        r.setIntCols(Set.of("CreatedByUserID", "ModifiedByUserID"));

        // eraseExtraSet 用來覆蓋特定欄位的值。
        var eraseSet = new LinkedHashMap<String, Object>();
        eraseSet.put("CreatedByUserID", -2);
        eraseSet.put("ModifiedByUserID", -2);
        eraseSet.put("isERASE", 1);
        eraseSet.put("ModifiedOnDate", "${NOW}");
        r.setEraseExtraSet(eraseSet);

        // restoreExtraSet 用來在還原時設定額外欄位值。
        var restoreExtra = new LinkedHashMap<String, Object>();
        restoreExtra.put("isERASE", 0);
        restoreExtra.put("ModifiedOnDate", "${NOW}");
        restoreExtra.put("ModifiedByUserID", ":uid"); // 由程式帶入操作者
        r.setRestoreExtraSet(restoreExtra);

        return r;
    }
}