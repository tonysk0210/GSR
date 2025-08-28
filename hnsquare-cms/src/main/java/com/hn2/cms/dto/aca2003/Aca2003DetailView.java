package com.hn2.cms.dto.aca2003;

import java.sql.Timestamp;

public interface Aca2003DetailView {
    Integer getId();

    Timestamp getCreatedOnDate();

    String getCreatedByBranchId();

    String getDrgUserText();

    String getOprFamilyText();

    String getOprFamilyCareText();

    String getOprSupportText();

    String getOprContactText();

    String getOprReferText();

    String getAddr();

    String getOprAddr();

    String getAcaCardNo();

    String getAcaName();

    String getAcaIdNo();
}
