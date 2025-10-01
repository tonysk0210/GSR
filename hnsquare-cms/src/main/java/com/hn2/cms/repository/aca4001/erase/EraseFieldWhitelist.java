package com.hn2.cms.repository.aca4001.erase;

public final class EraseFieldWhitelist {
    private EraseFieldWhitelist() {
    }

    private static final String[] CRMREC_SENSITIVE_FIELDS = {
            "ProSource1", "ProNoticeDep",
            "CrmCrime1", "CrmCrime2", "CrmCrime3",
            "CrmTerm", "CrmChaDate", "CrmDischarge", "CrmDisDate",
            "CrmTrain", "CrmCert", "CrmMemo",
            "CrmRemission", "Crm_ReleaseDate", "Crm_Release",
            "Crm_NoJail", "Crm_Sentence", "Crm_VerdictDate",
            "CreatedByUserID", "ModifiedByUserID"
    };
    // 未來：public static final List<String> PROREC = List.of(...);
}