package com.hn2.cms.service.aca4001.erase.configRule.tableConfig;

import com.hn2.cms.service.aca4001.erase.configRule.EraseRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Configuration
public class MemoRulesConfig {

    @Bean
    @Order(3) // after ACAFamilies & Career, before ACABrd
    public EraseRule memoRule() {
        EraseRule r = new EraseRule();
        r.setSchema("dbo");
        r.setTable("Memo");
        r.setIdColumn("ID");

        // parent is ACABrd; filter Memo by ACACardNo
        r.setParentTable("ACABrd");
        r.setParentFkColumn("ACACardNo");
        // no lookup mapping needed; Memo already has ACACardNo

        r.setWhitelist(List.of(
                "MemoDate",
                "MemoNote",
                "MemoWorkers",
                "CreatedByUserID",
                "ModifiedByUserID"
        ));
        r.setDateCols(Set.of("MemoDate"));
        r.setIntCols(Set.of("CreatedByUserID", "ModifiedByUserID"));

        // erase: default NULL for whitelist, override these:
        var eraseSet = new LinkedHashMap<String, Object>();
        eraseSet.put("CreatedByUserID", -2);
        eraseSet.put("ModifiedByUserID", -2);
        eraseSet.put("isERASE", 1);
        eraseSet.put("ModifiedOnDate", "${NOW}");
        r.setEraseSet(eraseSet);

        // restore: extra fields
        var restoreExtra = new LinkedHashMap<String, Object>();
        restoreExtra.put("isERASE", 0);
        restoreExtra.put("ModifiedOnDate", "${NOW}");
        restoreExtra.put("ModifiedByUserID", ":uid");
        r.setRestoreExtraSet(restoreExtra);

        return r;
    }
}