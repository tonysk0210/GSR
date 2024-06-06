package com.hn2.cms.repository.impl;

import com.hn2.cms.dto.Aca1002QueryDto;
import com.hn2.cms.payload.aca1002.Aca1002QueryPayload;
import com.hn2.cms.repository.Aca1002Repository;
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
public class Aca1002RepositoryImpl implements Aca1002Repository {
    @Autowired
    SqlStringHelper sqlStringHelper;
    @Autowired
    Sql2oHelper sql2oHelper;

    @Override
    public List<Aca1002QueryDto> queryList(Aca1002QueryPayload payload, PagePayload pagePayload) {
        String select = "SELECT " +
                "SAC.ID itemId, " +
                "SAC.ACA_STATE, " +
                "SAC.ACA_RECEIPT_DATE, " +
                "SAC.SIGN_PROT_NO, " +
                "SAC.SIGN_PROT_NAME, " +
                "SAC.RS_DT recvDate, " +
                "SAC.NAM_CNAME namName, " +
                "SAC.NAM_SEX, " +
                "SAC.NAM_HADDR_TEXT namAddr, " +
                "SAC.NAM_CNAMES_TEXT namCnames " +
                "FROM SUP_AfterCare SAC ";

        HashMap<String, Object> params = new HashMap<>();
        select += condition(payload, params);
        select += "ORDER BY SAC.ID ";

        if (pagePayload != null)
            select += sqlStringHelper.getPageSql(pagePayload.getPage(), pagePayload.getPageSize());

        return sql2oHelper.queryList(select, params, Aca1002QueryDto.class);
    }

    @Override
    public Integer countSearch(Aca1002QueryPayload payload) {
        String select = "SELECT COUNT(1) " +
                "FROM SUP_AfterCare SAC ";

        HashMap<String, Object> params = new HashMap<>();
        select += condition(payload, params);

        return Integer.valueOf(sql2oHelper.executeScalar(select, params).toString());
    }

    private StringBuilder condition(Aca1002QueryPayload payload, Map<String, Object> params) {

        var conditionBuilder = new StringBuilder();
        conditionBuilder.append("WHERE 1=1 ");

        // 收文日期起訖
        if (!ObjectUtils.isEmpty(payload.getRecvDateS())
                || !ObjectUtils.isEmpty(payload.getRecvDateE())) {
            if (!ObjectUtils.isEmpty(payload.getRecvDateS())) {
                conditionBuilder.append("AND SAC.RS_DT >= :recvDateS ");
                params.put("recvDateS", payload.getRecvDateS());
            }
            if (!ObjectUtils.isEmpty(payload.getRecvDateE())) {
                conditionBuilder.append("AND SAC.RS_DT < :recvDateE ");
                params.put("recvDateE", payload.getRecvDateE().plusDays(1));
            }
        }

        // 更生人名稱
        if (StringUtils.hasLength(payload.getNamName())) {
            conditionBuilder.append("AND SAC.NAM_CNAME like :namName ");
            params.put("namName", "%"+payload.getNamName()+"%");
        }

        // 簽收分會
        if (StringUtils.hasLength(payload.getSignProtName())) {
            conditionBuilder.append("AND SAC.SIGN_PROT_NAME = :signProtName ");
            params.put("signProtName", payload.getSignProtName());
        }

        // 簽收分會
        if (StringUtils.hasLength(payload.getSignProtNo())) {
            conditionBuilder.append("AND SAC.SIGN_PROT_NO = :signProtNo ");
            params.put("signProtNo", payload.getSignProtNo());
        }

        // 承辦人簽收
        if (StringUtils.hasLength(payload.getAcaReceiptUser())) {
            conditionBuilder.append("AND SAC.ACA_USER = :acaUser ");
            params.put("acaUser", payload.getAcaReceiptUser());
        }

        //承辦簽收狀態
        if (StringUtils.hasLength(payload.getAcaState())) {
            conditionBuilder.append("AND isnull(SAC.ACA_STATE,'0') = :acaState ");
            params.put("acaState", payload.getAcaState());
        }



        return conditionBuilder;
    }
}
