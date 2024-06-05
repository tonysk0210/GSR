package com.hn2.cms.repository;



import com.hn2.cms.dto.SysUserQueryDto;


import java.util.List;

public interface SysUserRepository {


    List<SysUserQueryDto> queryList(String unit);
}