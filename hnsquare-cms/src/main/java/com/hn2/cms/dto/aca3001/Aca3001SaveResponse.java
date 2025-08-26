package com.hn2.cms.dto.aca3001;

import com.hn2.cms.payload.aca3001.Aca3001SavePayload;
import lombok.Builder;
import lombok.Data;

@Data
@Builder //提供鏈式方法來建立物件，讓物件的建立更加優雅和易讀。
public class Aca3001SaveResponse {
    private Integer proAdoptId;
    private String proRecId;
    private boolean editable;
    private int scoreTotal;
    private Aca3001SavePayload.CaseStatus.State state;
    private String reason;
    private String message; // inserted / updated
}
