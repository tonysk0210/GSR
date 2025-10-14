package com.hn2.cms.service.aca4001.erase.rules.tableConfig;

import com.hn2.cms.service.aca4001.erase.rules.EraseTableConfigPojo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Configuration
public class ProRecConfig {

    @Bean
    public EraseTableConfigPojo proRecRule() {
        EraseTableConfigPojo r = new EraseTableConfigPojo();
        r.setSchema("dbo");
        r.setTable("ProRec");
        r.setIdColumn("ID");

        // 表示這些欄位允許被清空（erase）與還原（restore）處理。
        r.setWhitelist(List.of(
                "ProPlight",
                "HasPreviousPlight",
                "PreviousPlightChangedDesc",
                "ProStatus",
                "ProFile",
                "ProMemo",
                "ProWorkerBackup",
                "CreatedByUserID",
                "ModifiedByUserID"
        ));

        // 指定這些欄位為整數型別，用於 SqlNorm.normalizeForColumn() 處理型態轉換。
        r.setIntCols(Set.of("HasPreviousPlight", "CreatedByUserID", "ModifiedByUserID"));

        // eraseSet 用來覆蓋白名單預設行為（白名單欄位預設清空為 NULL）
        // 這裡明確指定哪些欄位要設特定值，而非 NULL。
        var eraseSet = new LinkedHashMap<String, Object>();
        eraseSet.put("isERASE", 1);
        eraseSet.put("ModifiedOnDate", "${NOW}");
        eraseSet.put("CreatedByUserID", -2);
        eraseSet.put("ModifiedByUserID", -2);
        r.setEraseExtraSet(eraseSet);

        // restoreExtra 用來指定還原後要額外更新的欄位與值
        var restoreExtra = new LinkedHashMap<String, Object>();
        restoreExtra.put("isERASE", 0);
        restoreExtra.put("ModifiedOnDate", "${NOW}");
        restoreExtra.put("ModifiedByUserID", ":uid");
        r.setRestoreExtraSet(restoreExtra);

        return r;
    }
}