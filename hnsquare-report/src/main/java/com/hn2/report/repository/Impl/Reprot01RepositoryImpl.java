package com.hn2.report.repository.Impl;

import com.hn2.report.dto.Insert_to_SUP_AfterCare_Print_Log_DTO;
import com.hn2.report.dto.Reprot01Dto;
import com.hn2.report.payload.Reprot01Payload;
import com.hn2.report.repository.Reprot01Repository;
import org.simpleflatmapper.sql2o.SfmResultSetHandlerFactoryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class Reprot01RepositoryImpl implements Reprot01Repository {
    /**
     * Sql2o
     */
    @Autowired
    Sql2o sql2o;

    @Override
    public List<Reprot01Dto> getList(Reprot01Payload payload) {
        try (Connection con = sql2o.open()) {
            String sql = "SELECT   [id]\n" +
                    "      ,(select OL.ORG_NAME from Org_Lists OL where OL.ORG_CODE=SA.ORG_CODE) as ORG_CODE\n" +
                    "      ,SA.ORG_CODE as org\n" +
                    "      ,[vir_no]\n" +
                    "      ,[RS_DT]\n" +
                    "      ,[TR_USER_NAME] TR_USER\n" +
                    "      ,[TR_TEL]\n" +
                    "      ,[TR_EMAIL]\n" +
                    "      ,[PROT_NAME]\n" +
                    "      ,[NAM_CNAME]\n" +
                    "      ,[NAM_SEX]\n" +
                    "      ,[NAM_BRDT]\n" +
                    "      ,[NAM_BONP_TEXT]\n" +
                    "      ,[NAM_IDNO]\n" +
                    "      ,[NAM_HADDR_TEXT]\n" +
                    "      ,[NAM_TEL]\n" +
                    "      ,[DOCU_PROC_TEXT]\n" +
                    "      ,[NAM_EDUC_TEXT]\n" +
                    "      ,[NAM_CNAMES_TEXT]\n" +
                    "      ,[NAM_PEN_TEXT]\n" +
                    "      ,[NAM_MVDT]\n" +
                    "      ,[DOCU_OTDT]\n" +
                    "      ,[DOCU_OTOP_TEXT]\n" +
                    "      ,[SKILL_TEXT]\n" +
                    "      ,[PROTECT_TEXT]\n" +
                    "      ,[DOCU_LICENSE_TEXT]\n" +
                    "      ,[RELIG_TEXT]\n" +
                    "      ,[DOCU_VIOLENT_TEXT]\n" +
                    "      ,[DOCU_REMARK]\n" +
                    "      ,[ECONOMIC_TEXT]\n" +
                    "      ,[MARRIAGE_TEXT]\n" +
                    "      ,[RELD_NAME]\n" +
                    "      ,[RELD_NO_TEXT]\n" +
                    "      ,[RELD_TEL1]\n" +
                    "      ,[RELD_TEL2]\n" +
                    "      ,[RELD_ADDR]\n" +
                    "     , [ADDR]\n" + //新撈通訊地址
                    "     , [OPR_ADDR]\n" + //新撈出監擬住地址
                    "     , [DRG_USER_TEXT]\n" + //新撈是否使用毒品
                    "  FROM SUP_AfterCare sa\n" +
                    "where id in (:ids) \n";

            Map<String, Object> params = new HashMap<>();

            params.put("ids", payload.getItemIdList());

            try (Query query = con.createQuery(sql)) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    query.addParameter(entry.getKey(), entry.getValue());
                }

                // SimpleFlatMapper
                query.setAutoDeriveColumnNames(true); //Sql2o 會自動依欄位名稱對應到 DTO 屬性
                query.setResultSetHandlerFactoryBuilder(new SfmResultSetHandlerFactoryBuilder()); //SimpleFlatMapper（SFM）整合，支援自動 mapping + 複雜欄位型別

                return query.executeAndFetch(Reprot01Dto.class); //執行 SQL，結果自動轉成 Reprot01Dto 的列表
            }
        }
    }

    @Override
    public void insertToSUP_AfterCare_Print_Log(List<Insert_to_SUP_AfterCare_Print_Log_DTO> insertDTOList) {
        try (Connection con = sql2o.open()) {
            String sql = " INSERT INTO SUP_AfterCare_Print_Log (" +
                    "ORG_CODE, " +
                    "VIR_NO," +
                    "RS_DT, " +
                    "PRINT_PROT_NAME," +
                    "PRINT_DATE," +
                    "PRINT_USER)" +
                    "VALUES (" +
                    ":ORG_CODE, " +
                    ":VIR_NO, " +
                    ":RS_DT, " +
                    ":PRINT_PROT_NAME, " +
                    ":PRINT_DATE, " +
                    ":PRINT_USER);";

            for (Insert_to_SUP_AfterCare_Print_Log_DTO dto : insertDTOList) {
                Map<String, Object> params = new HashMap<>();
                params.put("ORG_CODE", dto.getOrg_code());
                params.put("VIR_NO", dto.getVir_no());
                params.put("RS_DT", dto.getRs_dt());
                params.put("PRINT_PROT_NAME", dto.getPrint_prot_name());
                params.put("PRINT_DATE", dto.getPrint_date());
                params.put("PRINT_USER", dto.getPrint_user());

                try (Query query = con.createQuery(sql)) {
                    for (Map.Entry<String, Object> entry : params.entrySet()) {
                        query.addParameter(entry.getKey(), entry.getValue());
                    }
                    query.executeUpdate();
                }
            }
        }
    }
}
