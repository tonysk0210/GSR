package com.hn2.cms.service.aca4001;

import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto;
import com.hn2.cms.payload.aca4001.Aca4001ErasePayload;
import com.hn2.cms.payload.aca4001.Aca4001EraseQueryPayload;
import com.hn2.cms.payload.aca4001.Aca4001RestorePayload;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;

public interface Aca4001Service {
    DataDto<Aca4001EraseQueryDto> eraseQuery(GeneralPayload<Aca4001EraseQueryPayload> payload);

    // ★ 新增：統一由門面服務處理塗銷請求
    DataDto<Void> erase(GeneralPayload<Aca4001ErasePayload> payload, String userId, String userName, String userIp, String branchId);

    DataDto<Void> restore(GeneralPayload<Aca4001RestorePayload> payload, String userId, String userName, String userIp, String branchId);
}
