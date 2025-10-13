package com.hn2.cms.service.aca4001.erase.configRule.tableConfig;

import com.hn2.cms.service.aca4001.erase.configRule.EraseRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Configuration
public class ProjectDrugCloseRulesConfig {

    @Bean
    public EraseRule projectDrugCloseRule() {
        EraseRule r = new EraseRule();
        r.setSchema("dbo");
        r.setTable("ProjectDrugClose");
        r.setIdColumn("ID");

        // 子表：掛在 ProRec，FK 欄位 ProRecID
        r.setParentTable("ProRec");
        r.setParentFkColumn("ProRecID");

        // 對應 Target 的白名單欄位
        r.setWhitelist(List.of(
                "OpenMemo",
                "CloseMemo",
                "CreatedByUserID",
                "ModifiedByUserID"
        ));

        // 無日期欄位需要正規化
        r.setDateCols(Set.of());
        r.setIntCols(Set.of("CreatedByUserID", "ModifiedByUserID"));

        // Erase：白名單未覆寫者預設 NULL；以下為固定覆寫
        var eraseSet = new LinkedHashMap<String, Object>();
        eraseSet.put("CreatedByUserID", -2);
        eraseSet.put("ModifiedByUserID", -2);
        eraseSet.put("isERASE", 1);
        eraseSet.put("ModifiedOnDate", "${NOW}");
        r.setEraseSet(eraseSet);

        // Restore：追加欄位
        var restoreExtra = new LinkedHashMap<String, Object>();
        restoreExtra.put("isERASE", 0);
        restoreExtra.put("ModifiedOnDate", "${NOW}");
        restoreExtra.put("ModifiedByUserID", ":uid"); // 程式在執行時綁定操作者
        r.setRestoreExtraSet(restoreExtra);

        return r;
    }
}