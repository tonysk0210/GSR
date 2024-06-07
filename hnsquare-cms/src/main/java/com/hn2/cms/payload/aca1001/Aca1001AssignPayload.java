package com.hn2.cms.payload.aca1001;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Data
public class Aca1001AssignPayload {
    /** 項目編號列表 */
    @NotEmpty(message = "項目編號列表不可為空")
    private List<String> itemIdList;
    /** 份派人員 */
    @NotNull(message = "簽收人員為必填欄位")
    private String acaUser;
    /** 簽收日期 */
    @NotNull(message = "簽收日期為必填欄位")
    private LocalDate signDate;
    /** 簽收人員 */
    @NotNull(message = "簽收人員為必填欄位")
    private String signUser;
}
