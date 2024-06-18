package com.hn2.cms.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Aca1001QueryDto {
    /** 項目編號 */
    private String itemId;
    /** 簽收狀態 */
    private String signState;
    /** 簽收日期 */
    private LocalDate signDate;
    /** 發文日期 */
    private LocalDate rsDt;
    /** 簽收分會(代碼) */
    private String signProtNo;
    /** 簽收分會(分會別) */
    private String signProtName;
    /** 收文日期 */
    private LocalDate recvDate;
    /** 分派承辦人 */
    private String acaUser;
    /** 分派承辦人簽收日期 */
    private LocalDate acaReceiptDate;

    /** 更生人姓名 */
    private String namName;
    /** 更生人性別 */
    private String namSex;
    /** 更生人戶籍地址 */
    private String namAddr;
    /** 罪名 */
    private String namCnames;
}
