package com.hn2.cms.service.aca4001.erase.rules.tableConfig;

import com.hn2.cms.service.aca4001.erase.rules.EraseTableConfigPojo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Configuration
public class ACAFamiliesConfig {

    @Bean
    @Order(1)
    public EraseTableConfigPojo acaFamiliesRule() {
        EraseTableConfigPojo r = new EraseTableConfigPojo();
        r.setSchema("dbo");
        r.setTable("ACAFamilies");
        r.setIdColumn("ID");

        /* =========================
         * 🔗 父子關聯設定
         * =========================
         * 此表屬於 ACABrd 的子表。
         * 當 ACABrd 以 ACACardNo 為依據執行塗銷時，
         * ACAFamilies 必須根據對應的 FamCardNo 找出相關紀錄。
         */
        r.setParentTable("ACABrd");
        r.setParentFkColumn("FamCardNo");

        /* =========================
         * 🔄 父鍵映射設定（Lookup Mapping）
         * =========================
         * 因為父表 ACABrd 提供的 key 是 ACACardNo，
         * 但本表以 FamCardNo 為過濾依據，因此需要先查出對應關係：
         *    ACABrd.ACACardNo → ACABrd.FamCardNo
         *
         * 執行時會先從 ACABrd 找出 FamCardNo 清單，再用來處理本表。
         */
        r.setParentIdLookupTable("ACABrd");          // 查詢來源表
        r.setParentIdLookupSrcColumn("ACACardNo");   // 來源欄位（輸入 key）
        r.setParentIdLookupDstColumn("FamCardNo");   // 轉換結果欄位（輸出 key）

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
        r.setEraseExtraSet(eraseSet);

        var restoreExtra = new LinkedHashMap<String, Object>();
        restoreExtra.put("isERASE", 0);
        restoreExtra.put("ModifiedOnDate", "${NOW}");
        restoreExtra.put("ModifiedByUserID", ":uid");
        r.setRestoreExtraSet(restoreExtra);

        return r;
    }
}
