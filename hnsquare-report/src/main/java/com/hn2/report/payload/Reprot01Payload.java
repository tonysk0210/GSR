package com.hn2.report.payload;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class Reprot01Payload {
    /** 項目編號列表 */
    @NotEmpty(message = "項目編號列表不可為空")
    private List<String> itemIdList;
    /** 簽收人員 */
    @NotNull(message = "列印人員為必填欄位")
    private String printUser;
}
