package com.hn2.cms.service.aca4001.erase.configRule.tableConfig;

import com.hn2.cms.service.aca4001.erase.configRule.EraseRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Configuration
public class ProRecRulesConfig {

    @Bean
    public EraseRule proRecRule() {
        EraseRule r = new EraseRule();
        r.setSchema("dbo");
        r.setTable("ProRec");
        r.setIdColumn("ID");
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
        r.setIntCols(Set.of("HasPreviousPlight", "CreatedByUserID", "ModifiedByUserID"));

        // 清空策略（白名單未覆蓋者預設為 NULL；以下為覆寫）
        var eraseSet = new LinkedHashMap<String, Object>();
        eraseSet.put("isERASE", 1);
        eraseSet.put("ModifiedOnDate", "${NOW}");
        eraseSet.put("CreatedByUserID", -2);
        eraseSet.put("ModifiedByUserID", -2);
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