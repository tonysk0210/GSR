package com.hn2.cms.payload.aca1002;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class Aca1002CompareAcaPayload {
    /** 項目編號列表 */
    @NotEmpty(message = "主鍵值不可為空")
    private String itemId;

}
