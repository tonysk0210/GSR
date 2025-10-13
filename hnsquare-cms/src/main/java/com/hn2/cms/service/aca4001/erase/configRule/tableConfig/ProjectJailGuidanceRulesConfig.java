package com.hn2.cms.service.aca4001.erase.configRule.tableConfig;

import com.hn2.cms.service.aca4001.erase.configRule.EraseRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Configuration
public class ProjectJailGuidanceRulesConfig {

    @Bean
    public EraseRule projectJailGuidanceRule() {
        EraseRule r = new EraseRule();
        r.setSchema("dbo");
        r.setTable("ProjectJailGuidance");
        r.setIdColumn("ID");

        // 子表設定：隸屬 ProRec，外鍵 ProRecID
        r.setParentTable("ProRec");
        r.setParentFkColumn("ProRecID");

        // 由原 Target 的 whitelistColumns 對應
        r.setWhitelist(List.of(
                "FamilyStatus",
                "IsFamilyStatusMemo",
                "NotFamilyStatusMemo",
                "SentenceVisitStatus",
                "SentenceVisitStatusMemo",
                "PreJob",
                "PreJobMemo",
                "Temperament",
                "TemperamentMemo",
                "GuidanceIntention",
                "Job",
                "JobMemo",
                "TrainingMemo",
                "LicenseMemo",
                "PsyCounseling",
                "AnotherCase",
                "AnotherCaseMemo",
                "Other",
                "Assist",
                "IsWilling",
                "CreatedByUserID",
                "ModifiedByUserID"
        ));

        // 原 Target 僅規範帳號欄位為整數
        r.setIntCols(Set.of("CreatedByUserID", "ModifiedByUserID"));
        // 無日期欄位需要規範，若未來新增可：r.setDateCols(Set.of(...));

        // 清空策略：白名單欄位預設為 NULL，以下覆寫固定值
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
        restoreExtra.put("ModifiedByUserID", ":uid"); // 程式會綁定操作者
        r.setRestoreExtraSet(restoreExtra);

        return r;
    }
}