package com.hn2.cms.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AcaJailQueryDto {
    /** 項目編號 */
    private String itemId;
    /** 簽收狀態 */
    private String signState;
    /** 簽收日期 */
    private LocalDate signDate;
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
