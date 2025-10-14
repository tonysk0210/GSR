package com.hn2.cms.service.aca4001.erase.rules.tableConfig;

import com.hn2.cms.service.aca4001.erase.rules.EraseTableConfigPojo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Configuration
public class MemoConfig {

    @Bean
    @Order(3)  // after ACAFamilies & Career, before ACABrd
    public EraseTableConfigPojo memoRule() {
        EraseTableConfigPojo r = new EraseTableConfigPojo();
        r.setSchema("dbo");
        r.setTable("Memo");
        r.setIdColumn("ID");

        r.setParentTable("ACABrd");
        r.setParentFkColumn("ACACardNo");

        r.setWhitelist(List.of(
                "MemoDate",
                "MemoNote",
                "MemoWorkers",
                "CreatedByUserID",
                "ModifiedByUserID"
        ));
        r.setDateCols(Set.of("MemoDate"));
        r.setIntCols(Set.of("CreatedByUserID", "ModifiedByUserID"));

        var eraseSet = new LinkedHashMap<String, Object>();
        eraseSet.put("CreatedByUserID", -2);
        eraseSet.put("ModifiedByUserID", -2);
        eraseSet.put("isERASE", 1);
        eraseSet.put("ModifiedOnDate", "${NOW}");
        r.setEraseExtraSet(eraseSet);

        var restoreExtra = new LinkedHashMap<String, Object>();
        restoreExtra.put("isERASE", 0);
        restoreExtra.put("ModifiedOnDate", "${NOW}");
        restoreExtra.put("ModifiedByUserID", ":uid");
        r.setRestoreExtraSet(restoreExtra);

        return r;
    }
}