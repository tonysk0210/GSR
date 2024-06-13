package com.hn2.cms.repository;


import com.hn2.cms.dto.SysCodeQueryDto;
import com.hn2.cms.dto.SysUserQueryDto;
import com.hn2.cms.payload.sys.SysCodeQueryPayload;

import java.util.List;

public interface SysCodeRepository {
    List<SysCodeQueryDto> codeList(SysCodeQueryPayload payload);
}