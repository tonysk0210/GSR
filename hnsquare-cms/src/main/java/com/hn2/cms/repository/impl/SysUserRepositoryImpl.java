package com.hn2.cms.repository.impl;


import com.hn2.cms.dto.SysUserQueryDto;

import com.hn2.cms.repository.Aca1001Repository;
import com.hn2.cms.repository.SysUserRepository;
import com.hn2.core.payload.PagePayload;
import com.hn2.util.Sql2oHelper;
import com.hn2.util.SqlStringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SysUserRepositoryImpl implements SysUserRepository {
    @Autowired
    SqlStringHelper sqlStringHelper;
    @Autowired
    Sql2oHelper sql2oHelper;

    @Override
    public  List<SysUserQueryDto> queryList(String unit) {
        String select = "SELECT " +
                "FROM CaseManagementDnnDB.dbo.users u\n" +
                "INNER JOIN CaseManagementDnnDB.dbo.UserPortals p\n" +
                "\tON u.UserID = p.UserId\n" +
                "LEFT JOIN CaseManagementDnnDB.dbo.UserProfile up\n" +
                "\tON up.UserID = u.UserID\n" +
                "WHERE isnull(u.isdeleted, 0) = 0\n" +
                "\tAND u.createdbyuserid > 0\n" +
                "\tAND p.Authorised = 1\n" +
                "\tAND isnull((SELECT propertyvalue\n" +
                "\t\t\t\tFROM CaseManagementDnnDB.dbo.ProfilePropertyDefinition c\n" +
                "\t\t\t\tWHERE up.PropertyDefinitionID = c.PropertyDefinitionID\n" +
                "\t\t\t\t\tAND c.PropertyName = 'Branch'\n" +
                "\t\t\t\t), '') = :unit \n" +
                "\tAND isnull((SELECT propertyvalue\n" +
                "\t\t\t\tFROM CaseManagementDnnDB.dbo.ProfilePropertyDefinition c\n" +
                "\t\t\t\tWHERE up.PropertyDefinitionID = c.PropertyDefinitionID\n" +
                "\t\t\t\t\tAND c.PropertyName = 'isEmployee'\n" +
                "\t\t\t\t), '0') = '0'" +

                "FROM CaseManagementDnnDB..Users   where unit = :unit  ORDER BY SAC.ID  ";

        HashMap<String, Object> params = new HashMap<>();
        params.put("unit", unit);

        return sql2oHelper.queryList(select, params, SysUserQueryDto.class);
    }


}
