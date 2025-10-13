package com.hn2.cms.service.aca4001.erase.configRule.tableConfig;

import com.hn2.cms.service.aca4001.erase.configRule.EraseRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Configuration
public class ACABrdRulesConfig {

    /**
     * ACABrd 使用 ACACardNo 作為「父鍵」批次處理依據。
     * 在規則上將 parentTable 設為自身（ACABrd），parentFkColumn=ACACardNo，
     * 這樣 runRule*() 會從 cmd.idsOf("ACABrd") 取得 ACACardNo 清單，並用於 WHERE ACACardNo IN (...)
     */
    @Bean
    @Order(4)
    public EraseRule acaBrdRule() {
        EraseRule r = new EraseRule();
        r.setSchema("dbo");
        r.setTable("ACABrd");
        r.setIdColumn("ID");

        // 自身為「父」，以 ACACardNo 作為批次鍵
        r.setParentTable("ACABrd");       // cmd.idsOf("ACABrd") 裝的是 ACACardNo 清單
        r.setParentFkColumn("ACACardNo"); // 子查詢/更新用的 FK 欄位

        r.setWhitelist(List.of(
                "FamCardNo",
                "ACAIDNo",
                "ACA_Nationality",
                "ACA_Passport",
                "ACAName",
                "ACASex",
                "ACABirth",
                "ACAArea",
                "ACATel",
                "ACAMobile",
                "ACATel2",
                "ACAMobile2",
                "ACAEmail",
                "ACAFax",
                "ResidenceAddress",
                "ResidencePostal",
                "PermanentAddress",
                "PermanentPostal",
                "ACALiaison",
                "ACALiaisonTel",
                "ACALiaisonRelation",
                "ACALiaisonMobile",
                "ACALiaisonAddr",
                "ACAEdu",
                "ACAMarry",
                "ACARelig",
                "ACACareer",
                "ACASkill",
                "ACAOther",
                "ACA_Economic",
                "ACAHome",
                "ACA_Interest",
                "IsBlackList",
                "CreatedByUserID",
                "ModifiedByUserID"
        ));

        r.setDateCols(Set.of("ACABirth"));
        r.setIntCols(Set.of("CreatedByUserID", "ModifiedByUserID"));

        // Erase：白名單未覆寫者預設設為 NULL；以下為固定覆寫
        var eraseSet = new LinkedHashMap<String, Object>();
        eraseSet.put("CreatedByUserID", -2);
        eraseSet.put("ModifiedByUserID", -2);
        eraseSet.put("isERASE", 1);
        eraseSet.put("ModifiedOnDate", "${NOW}");
        r.setEraseSet(eraseSet);

        // Restore：追加欄位（由程式在執行時綁定 :uid）
        var restoreExtra = new LinkedHashMap<String, Object>();
        restoreExtra.put("isERASE", 0);
        restoreExtra.put("ModifiedOnDate", "${NOW}");
        restoreExtra.put("ModifiedByUserID", ":uid");
        r.setRestoreExtraSet(restoreExtra);

        return r;
    }
}