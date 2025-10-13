package com.hn2.cms.service.aca4001.erase.configRule.tableConfig;

import com.hn2.cms.service.aca4001.erase.configRule.EraseRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Configuration
public class ProjectDrugVisitRulesConfig {

    @Bean
    public EraseRule projectDrugVisitRule() {
        EraseRule r = new EraseRule();
        r.setSchema("dbo");
        r.setTable("ProjectDrugVisit");
        r.setIdColumn("ID");

        // 子表設定：隸屬 ProRec，以 ProRecID 關聯
        r.setParentTable("ProRec");
        r.setParentFkColumn("ProRecID");

        // 對應原 Target 白名單欄位
        r.setWhitelist(List.of(
                "SocietyMemo",
                "MarrigeStatus",
                "FaiStatus",
                "FaiSupport",
                "FaiSupportMemo",
                "SocSupport",
                "SocSupportFriend",
                "SocSupportBoss",
                "SocSupportOther",
                "Live",
                "LiveMemo",
                "LifeMemo",
                "SocStatus",
                "SocStatusMemo",
                "MonthlyPsy",
                "MonthlyPsyMemo",
                "PsyStatus2",
                "CasePlan",
                "CreatedByUserID",
                "ModifiedByUserID"
        ));

        // 無日期欄位需要正規化；整數欄位如下（視你的 schema 調整）
        r.setDateCols(Set.of());
        r.setIntCols(Set.of("CreatedByUserID", "ModifiedByUserID"));

        // 清空策略：白名單未覆寫者預設設為 NULL；以下為固定覆寫
        var eraseSet = new LinkedHashMap<String, Object>();
        eraseSet.put("CreatedByUserID", -2);
        eraseSet.put("ModifiedByUserID", -2);
        eraseSet.put("isERASE", 1);
        eraseSet.put("ModifiedOnDate", "${NOW}");
        r.setEraseSet(eraseSet);

        // 還原時的追加欄位
        var restoreExtra = new LinkedHashMap<String, Object>();
        restoreExtra.put("isERASE", 0);
        restoreExtra.put("ModifiedOnDate", "${NOW}");
        restoreExtra.put("ModifiedByUserID", ":uid"); // 由程式綁定操作者
        r.setRestoreExtraSet(restoreExtra);

        return r;
    }
}