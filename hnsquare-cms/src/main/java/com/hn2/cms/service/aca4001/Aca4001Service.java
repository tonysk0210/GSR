package com.hn2.cms.service.aca4001;

import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto;
import com.hn2.cms.payload.aca4001.Aca4001ErasePayload;
import com.hn2.cms.payload.aca4001.Aca4001EraseQueryPayload;
import com.hn2.cms.payload.aca4001.Aca4001RestorePayload;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;

public interface Aca4001Service {
    DataDto<Aca4001EraseQueryDto> eraseQuery(GeneralPayload<Aca4001EraseQueryPayload> payload);

    // 刪掉 userName、branchId
    DataDto<Void> erase(GeneralPayload<Aca4001ErasePayload> payload, String userId, String userIp);

    DataDto<Void> restore(GeneralPayload<Aca4001RestorePayload> payload, String userId, String userIp);
}
