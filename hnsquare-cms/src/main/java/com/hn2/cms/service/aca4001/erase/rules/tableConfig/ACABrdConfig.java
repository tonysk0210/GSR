package com.hn2.cms.service.aca4001.erase.rules.tableConfig;

import com.hn2.cms.service.aca4001.erase.rules.EraseTableConfigPojo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Configuration
public class ACABrdConfig {

    @Bean
    @Order(4) // 讓此表在多個規則中的執行順序較後（子表先處理）
    public EraseTableConfigPojo acaBrdRule() {
        EraseTableConfigPojo r = new EraseTableConfigPojo();
        r.setSchema("dbo");
        r.setTable("ACABrd");
        r.setIdColumn("ID");

        // ACABrd 的需求是「用 ACACardNo 來批次處理 ACABrd 自己」，而非用 ACABrd.ID。因此必須把 ACABrd 這個規則「包裝成子規則」：
        // EraseRestoreExecutor 才會走到 eraseByParent(...) 與 loadRowsByParentIds(...) 這條路。
        r.setParentTable("ACABrd");
        r.setParentFkColumn("ACACardNo");

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