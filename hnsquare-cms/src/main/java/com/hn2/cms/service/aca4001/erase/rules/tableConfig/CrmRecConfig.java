package com.hn2.cms.service.aca4001.erase.rules.tableConfig;

import com.hn2.cms.service.aca4001.erase.rules.EraseTableConfigPojo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Configuration
public class CrmRecConfig {

    @Bean
    public EraseTableConfigPojo crmRecRule() {
        EraseTableConfigPojo r = new EraseTableConfigPojo();
        r.setSchema("dbo");
        r.setTable("CrmRec");
        r.setIdColumn("ID");

        // --- 1️⃣ 白名單欄位（允許被鏡像 / 清空 / 還原的欄位）---
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

        // --- 2️⃣ 正規化設定 ---
        // 在還原時，這些欄位會根據型態進行自動轉換（日期 / 整數）。
        r.setDateCols(Set.of("CrmChaDate", "CrmDisDate", "Crm_ReleaseDate", "Crm_VerdictDate"));
        r.setIntCols(Set.of("CreatedByUserID", "ModifiedByUserID", "Crm_Sentence", "CrmTerm"));

        // --- 3️⃣ 清空（Erase）策略 ---
        // 預設：白名單中未在 eraseSet 出現的欄位都會被設為 NULL。
        // eraseExtraSet 中的欄位則會被覆蓋為指定值。
        var eraseSet = new LinkedHashMap<String, Object>();
        eraseSet.put("isERASE", 1);                             // 記錄狀態：設為塗銷中（isERASE=1）
        eraseSet.put("ModifiedOnDate", "${NOW}");               // 修改時間：設為目前時間
        eraseSet.put("CreatedByUserID", -2);                    // 建立者與修改者設為 -2
        eraseSet.put("ModifiedByUserID", -2);
        r.setEraseExtraSet(eraseSet);

        // --- 4️⃣ 還原（Restore）策略 ---
        var restoreExtra = new LinkedHashMap<String, Object>();
        restoreExtra.put("isERASE", 0);
        restoreExtra.put("ModifiedOnDate", "${NOW}");
        restoreExtra.put("ModifiedByUserID", ":uid"); // 程式注入操作者
        r.setRestoreExtraSet(restoreExtra);

        return r;
    }
}