package com.hn2.cms.service.aca4001.erase.rule.tableConfig;

import com.hn2.cms.service.aca4001.erase.rule.EraseRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Configuration
public class ACAFamiliesRulesConfig {

    @Bean
    @Order(1)
    public EraseRule acaFamiliesRule() {
        EraseRule r = new EraseRule();
        r.setSchema("dbo");
        r.setTable("ACAFamilies");
        r.setIdColumn("ID");

        // 父表仍是 ACABrd（cmd.idsOf("ACABrd") 會給 ACACardNo）
        r.setParentTable("ACABrd");

        // 子表以 FamCardNo 過濾
        r.setParentFkColumn("FamCardNo");

        // ★ 新增：先把 ACACardNo 映射成 FamCardNo
        r.setParentIdLookupTable("ACABrd");
        r.setParentIdLookupSrcColumn("ACACardNo"); // 入口：ACACardNo
        r.setParentIdLookupDstColumn("FamCardNo"); // 轉成：FamCardNo

        r.setWhitelist(List.of(
                "FaiIDNo",
                "FaiName",
                "FaiSex",
                "FaiNationality",
                "IsContact",
                "FaiBrith",
                "FaiAddress",
                "IsAcaAddress",
                "FaiTel",
                "IsAcaTel",
                "FaiMobile",
                "FaiAppellation",
                "FaiPassport",
                "FaiCareer",
                "FaiOther",
                "FaiPostal",
                "IsDead",
                "ACACardNo",
                "CreatedByUserID",
                "ModifiedByUserID"
        ));

        r.setDateCols(Set.of("FaiBrith"));
        r.setIntCols(Set.of("CreatedByUserID", "ModifiedByUserID"));

        var eraseSet = new LinkedHashMap<String, Object>();
        eraseSet.put("CreatedByUserID", -2);
        eraseSet.put("ModifiedByUserID", -2);
        eraseSet.put("isERASE", 1);
        eraseSet.put("ModifiedOnDate", "${NOW}");
        r.setEraseSet(eraseSet);

        var restoreExtra = new LinkedHashMap<String, Object>();
        restoreExtra.put("isERASE", 0);
        restoreExtra.put("ModifiedOnDate", "${NOW}");
        restoreExtra.put("ModifiedByUserID", ":uid");
        r.setRestoreExtraSet(restoreExtra);

        return r;
    }
}
