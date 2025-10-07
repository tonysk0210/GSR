package com.hn2.cms.dto.aca1002;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Aca1002QueryDto {
    /** 項目編號 */
    private String itemId;
    /** 承辦人簽收狀態 */
    private String acaState;
    /** 承辦人簽收日期 */
    private LocalDate acaReceiptDate;

    /** 簽收分會(代碼) */
    private String signProtNo;
    /** 簽收分會(分會別) */
    private String signProtName;

    /** 發文日期 */
    private String recvDate;
    /** 更生人姓名 */
    private String namName;
    /** 更生人性別 */
    private String namSex;
    /** 更生人戶籍地址 */
    private String namAddr;
    /** 罪名 */
    private String namCnames;
}
