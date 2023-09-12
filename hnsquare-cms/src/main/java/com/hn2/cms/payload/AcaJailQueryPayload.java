package com.hn2.cms.payload;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AcaJailQueryPayload {
    /** 收文日期(起) */
    private LocalDate recvDateS;
    /** 收文日期(迄) */
    private LocalDate recvDateE;
    /** 更生人名稱 */
    private String namName;
    /** 簽收分會(會別) */
    private String signProtName;
    /** 簽收狀態 - 0:未簽收 1:已簽收 */
    private String signState;
}
