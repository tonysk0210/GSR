package com.hn2.cms.service.aca4001.erase.rules.tableConfig;

import com.hn2.cms.service.aca4001.erase.rules.EraseTableConfigPojo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Configuration
public class CareerConfig {

    @Bean
    @Order(2) // after ACAFamilies, before Memo & ACABrd
    public EraseTableConfigPojo careerRule() {
        EraseTableConfigPojo r = new EraseTableConfigPojo();
        r.setSchema("dbo");
        r.setTable("Career");
        r.setIdColumn("ID");

        r.setParentTable("ACABrd");
        r.setParentFkColumn("ACACardNo");

        r.setWhitelist(List.of(
                "IsWishToBePosted",
                "CarType1",
                "CarType2",
                "CarType3",
                "CarSalary",
                "CarHealthOption",
                "CarHealth",
                "CarLife",
                "CarTimeOption",
                "CarTime",
                "CarPlace1",
                "CarPlace2",
                "CarPlace3",
                "CarMemo",
                "FacCardNo",
                "CarEmployment",
                "IsReplySector",
                "CarSDate",
                "CarCom",
                "CarPost",
                "CarLoc",
                "CarStat",
                "CarUnemployed",
                "CarEmployment1",
                "CarEmployment2",
                "Car_Assist",
                "CarPlaceBackup",
                "Crime",
                "CrimeNotice",
                "License",
                "Experiment",
                "Driver",
                "Bike",
                "CreatedByUserID",
                "ModifiedByUserID"
        ));
        r.setDateCols(Set.of("CarSDate"));
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