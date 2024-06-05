package com.hn2.cms.payload.aca1001;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hn2.core.payload.BasePayload;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Aca1001QueryPayload extends BasePayload {
    /** 收文日期(起) */
    private LocalDate recvDateS;
    /** 收文日期(迄) */
    private LocalDate recvDateE;
    /** 更生人名稱 */
    private String namName;
    /** 簽收分會(會別) */
    private String signProtName;
    /** 簽收分會代碼(會別) */
    private String signProtNo;
    /** 簽收狀態 - 0:未簽收 1:已簽收 */
    private String signState;
}
