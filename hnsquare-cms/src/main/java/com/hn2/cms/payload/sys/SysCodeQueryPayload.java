package com.hn2.cms.payload.sys;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Data
public class SysCodeQueryPayload {
    /** 項目編號列表 */
    @NotEmpty(message = "缺少父層資訊")
    private String parentId;
    /** 簽收日期 */
    @NotNull(message = "缺少代碼種類")
    private String codeKind;
    /** 簽收人員 */
    @NotNull(message = "缺少層級")
    private String level;
}
