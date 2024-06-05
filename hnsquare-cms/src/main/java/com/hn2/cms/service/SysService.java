package com.hn2.cms.service;

import com.hn2.cms.dto.SysUserQueryDto;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;

import java.util.List;

public interface SysService {


    DataDto<List<SysUserQueryDto>> queryList(String unit);
}
