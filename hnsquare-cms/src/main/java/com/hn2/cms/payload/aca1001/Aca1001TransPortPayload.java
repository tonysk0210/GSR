package com.hn2.cms.payload.aca1001;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class Aca1001TransPortPayload {
    /** 項目編號列表 */
    @NotEmpty(message = "項目編號列表不可為空")
    private List<String> itemIdList;
    /** 簽收分會 */
    @NotNull(message = "簽收分會代碼為必填欄位")
    private String signProtNo;
    /** 簽收分會 */
    @NotNull(message = "簽收分會為必填欄位")
    private String signProtName;
}
