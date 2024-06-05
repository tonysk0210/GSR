package com.hn2.cms.service.impl;

import com.hn2.cms.dto.SysUserQueryDto;
import com.hn2.cms.service.SysService;
import com.hn2.core.dto.DataDto;
import com.hn2.core.dto.ResponseInfo;
import com.hn2.cms.repository.SysUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysServiceImpl implements SysService {

    @Autowired
    SysUserRepository sysUserRepository;

    @Override
    public DataDto<List<SysUserQueryDto>> queryList(String unit) {


        List<SysUserQueryDto> dataList = sysUserRepository.queryList(unit);

        return new DataDto<>(dataList, null, new ResponseInfo(1, "查詢成功"));
    }


}
