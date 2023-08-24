package com.hn2.core.payload;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DatasPayload extends BasePayload {
    /** 多筆物件 */
    private java.util.List<Object> datas;
    /** 總筆數 */
    private Integer totalRecord = 0;
    /** 每頁筆數 */
    private Integer pageSize = 10;
    /** 總頁數 */
    private Integer totalPage = 0;
    /** 目前頁數 */
    private Integer currentPage = 1;
    /** 目前頁數的資料起 */
    private Integer recordStart = 0;
    /** 目前頁數的資料迄 */
    private Integer recordEnd = 0;

    private void setTotalRecord(Integer n) {
        totalRecord = n;
        if (totalRecord <= 0) {
            recordStart = 0;
            recordEnd = 0;
            return ;
        }
        if (pageSize > 0) {
            totalPage = (totalRecord - 1) / pageSize + 1;
            if (currentPage < 1)
                currentPage = 1;
            if (currentPage > totalPage)
                currentPage = totalPage;

            recordStart = pageSize * (currentPage - 1);
            recordEnd = Math.min(recordStart + pageSize, totalRecord);
        } else {
            recordStart = 0;
            recordEnd = totalRecord;
        }
    }

}