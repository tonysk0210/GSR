package com.hn2.cms.repository.impl;


import com.hn2.cms.dto.SysCodeQueryDto;
import com.hn2.cms.payload.sys.SysCodeQueryPayload;
import com.hn2.cms.repository.SysCodeRepository;
import com.hn2.util.Sql2oHelper;
import com.hn2.util.SqlStringHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;

@Repository
public class SysCodeRepositoryImpl implements SysCodeRepository {
    @Autowired
    SqlStringHelper sqlStringHelper;
    @Autowired
    Sql2oHelper sql2oHelper;

    @Override
    public  List<SysCodeQueryDto> codeList(SysCodeQueryPayload payload) {
        String select = "select  value, text, entryId as id , isDisabled,isDeleted " +
                " from Lists where case when 0 = :parentId then parentId else :parentId end = parentId " +
                " and listName = :codeKind and level = :level order by isDeleted,isDisabled, SortOrder  ";

        HashMap<String, Object> params = new HashMap<>();
        params.put("parentId", payload.getParentId());
        params.put("codeKind", payload.getCodeKind());
        params.put("level", payload.getLevel());



        return sql2oHelper.queryList(select, params, SysCodeQueryDto.class);
    }


}
