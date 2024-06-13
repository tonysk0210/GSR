package com.hn2.cms.dto;

import lombok.Data;

@Data
public class SysCodeQueryDto {
    /**
     * 代碼
     */
    private String value;
    /**
     * 代碼名稱
     */
    private String text;
    /**
     * 父層
     */
    private String Id;
    /**
     * 展示  0:false 1: true 前端灰色表示
     */
    private String isDisabled;
    /**
     * 刪除 0:false 1: true 前端灰色表示並增加"(刪除)"文字
     */
    private String isDeleted;
}

