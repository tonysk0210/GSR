package com.hn2.cms.service;

import com.hn2.cms.dto.SysCodeQueryDto;
import com.hn2.cms.dto.SysUserQueryDto;
import com.hn2.cms.payload.sys.SysCodeQueryPayload;
import com.hn2.core.dto.DataDto;

import java.util.List;

public interface SysService {


    DataDto<List<SysUserQueryDto>> queryList(String unit);

    DataDto<List<SysCodeQueryDto>> codeList(SysCodeQueryPayload payload);
}
