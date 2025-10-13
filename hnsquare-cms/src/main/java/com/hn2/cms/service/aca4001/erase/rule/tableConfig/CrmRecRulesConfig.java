package com.hn2.cms.service.aca4001.erase.rule.tableConfig;

import com.hn2.cms.service.aca4001.erase.rule.EraseRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Configuration
public class CrmRecRulesConfig {

    @Bean
    public EraseRule crmRecRule() {
        EraseRule r = new EraseRule();
        r.setSchema("dbo");
        r.setTable("CrmRec");
        r.setIdColumn("ID");

        // 鏡像/清空/還原的白名單欄位
        r.setWhitelist(List.of(
                "ProSource1",
                "ProNoticeDep",
                "CrmCrime1",
                "CrmCrime2",
                "CrmCrime3",
                "CrmTerm",
                "CrmChaDate",
                "CrmDischarge",
                "CrmDisDate",
                "CrmTrain",
                "CrmCert",
                "CrmMemo",
                "CrmRemission",
                "Crm_ReleaseDate",
                "Crm_Release",
                "Crm_NoJail",
                "Crm_Sentence",
                "Crm_VerdictDate",
                "CreatedByUserID",
                "ModifiedByUserID"
        ));

        // 正規化設定（與原 Target 對齊）
        r.setDateCols(Set.of("CrmChaDate", "CrmDisDate", "Crm_ReleaseDate", "Crm_VerdictDate"));
        r.setIntCols(Set.of("CreatedByUserID", "ModifiedByUserID", "Crm_Sentence", "CrmTerm"));

        // 清空策略（白名單未覆蓋者預設為 NULL）
        var eraseSet = new LinkedHashMap<String, Object>();
        eraseSet.put("isERASE", 1);
        eraseSet.put("ModifiedOnDate", "${NOW}");
        eraseSet.put("CreatedByUserID", -2);
        eraseSet.put("ModifiedByUserID", -2);
        r.setEraseSet(eraseSet);

        // 還原追加欄位
        var restoreExtra = new LinkedHashMap<String, Object>();
        restoreExtra.put("isERASE", 0);
        restoreExtra.put("ModifiedOnDate", "${NOW}");
        restoreExtra.put("ModifiedByUserID", ":uid"); // 程式注入操作者
        r.setRestoreExtraSet(restoreExtra);

        return r;
    }
}