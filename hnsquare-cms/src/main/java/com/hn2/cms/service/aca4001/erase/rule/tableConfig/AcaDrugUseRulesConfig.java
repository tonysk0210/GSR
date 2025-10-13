package com.hn2.cms.service.aca4001.erase.rule.tableConfig;

import com.hn2.cms.service.aca4001.erase.rule.EraseRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Configuration
public class AcaDrugUseRulesConfig {

    @Bean
    public EraseRule acaDrugUseRule() {
        EraseRule r = new EraseRule();
        r.setSchema("dbo");
        r.setTable("AcaDrugUse");
        r.setIdColumn("ID");

        // 子表設定：歸屬 ProRec
        r.setParentTable("ProRec");
        r.setParentFkColumn("ProRecID");

        // 對應 whitelistColumns()
        r.setWhitelist(List.of(
                "Addr",
                "OprAddr",
                "DrgUserText",
                "OprFamilyText",
                "OprFamilyCareText",
                "CreatedByBranchID",
                "CreatedByUserID",
                "ModifiedByUserID"
        ));

        // 對應 intColsNorm()
        r.setIntCols(Set.of("CreatedByUserID", "ModifiedByUserID"));

        // 清空策略（其餘白名單欄位預設為 NULL；以下覆寫）
        var eraseSet = new LinkedHashMap<String, Object>();
        eraseSet.put("CreatedByBranchID", "");
        eraseSet.put("CreatedByUserID", -2);
        eraseSet.put("ModifiedByUserID", -2);
        eraseSet.put("isERASE", 1);
        eraseSet.put("ModifiedOnDate", "${NOW}");
        r.setEraseSet(eraseSet);

        // 還原時追加欄位
        var restoreExtra = new LinkedHashMap<String, Object>();
        restoreExtra.put("isERASE", 0);
        restoreExtra.put("ModifiedOnDate", "${NOW}");
        restoreExtra.put("ModifiedByUserID", ":uid"); // 由程式注入操作者
        r.setRestoreExtraSet(restoreExtra);

        return r;
    }
}