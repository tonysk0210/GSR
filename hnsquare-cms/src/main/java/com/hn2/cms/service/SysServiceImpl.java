package com.hn2.cms.service;

import com.hn2.cms.dto.SysCodeQueryDto;
import com.hn2.cms.dto.SysUserQueryDto;
import com.hn2.cms.payload.sys.SysCodeQueryPayload;
import com.hn2.cms.repository.SysCodeRepository;
import com.hn2.core.dto.DataDto;
import com.hn2.core.dto.ResponseInfo;
import com.hn2.cms.repository.SysUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNumeric;

@Service
public class SysServiceImpl implements SysService {

    @Autowired
    SysUserRepository sysUserRepository;
    @Autowired
    SysCodeRepository sysCodeRepository;

    @Override
    public DataDto<List<SysUserQueryDto>> queryList(String unit) {


        List<SysUserQueryDto> dataList = sysUserRepository.queryList(unit);

        return new DataDto<>(dataList, null, new ResponseInfo(1, "查詢成功"));
    }

    @Override
    public DataDto<List<SysCodeQueryDto>> codeList(SysCodeQueryPayload payload) {


        List<SysCodeQueryDto> dataList = sysCodeRepository.codeList(payload);

        return new DataDto<>(dataList, null, new ResponseInfo(1, "查詢成功"));
    }

    /**
     * 前端傳入為員工編號(對應資料庫為username) ，轉換成資料內碼
     * @param username
     * @return
     */
    @Override
    public String convertUsernameToUserId(String username) {
        if (isNumeric(username)){
            return username;
        }
        if (StringUtils.isEmpty( username)){
            return "-1";
        }
        SysUserQueryDto user = sysUserRepository.queryByUsername(username);
        if (user == null ){
            return "-1";
        }
        return user.getUserId();
    }

}
