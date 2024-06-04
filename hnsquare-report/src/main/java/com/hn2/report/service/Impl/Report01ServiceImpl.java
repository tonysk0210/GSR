package com.hn2.report.service.Impl;

import com.hn2.report.dto.Insert_to_SUP_AfterCare_Print_Log_DTO;
import com.hn2.report.dto.Reprot01Dto;
import com.hn2.report.payload.Reprot01Payload;
import com.hn2.report.repository.Reprot01Repository;
import com.hn2.report.service.Report01Service;
import com.hn2.report.util.ReportFormat;
import com.hn2.report.util.ReportGenerator;
import com.hn2.util.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.hn2.report.util.ReportEnvironment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.hn2.util.DateUtil.DateFormat.yyy年M月d日;
import static com.hn2.util.DateUtil.date2Roc;

@Service
@Slf4j
public class Report01ServiceImpl implements Report01Service {
    @Autowired
    Reprot01Repository repository;

    /*** 報表 Environment*/
    @Autowired
    private ReportEnvironment reportEnvironment;

    /*** 報表 Generator*/
    @Autowired
    private ReportGenerator reportGenerator;

    @Override
    public byte[] getReport(Reprot01Payload payload) {
        List<Reprot01Dto> listDto = repository.getList(payload);

        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd :hh:mm:ss");
        String  rocDate = date2Roc(new Date(),yyy年M月d日);
        String dd = dateFormat.format(date);
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("reportNo", "Reprot01");
        params.put("reportCreateDateTime", rocDate);

        if (listDto == null || listDto.size() == 0) {
            throw new BusinessException("查無資料");
        }else {
            List<Insert_to_SUP_AfterCare_Print_Log_DTO> insertDTOList = new ArrayList<>();
            for(int i=0;i<listDto.size();i++){
                Insert_to_SUP_AfterCare_Print_Log_DTO insertDTO=new Insert_to_SUP_AfterCare_Print_Log_DTO();
                insertDTO.setOrg_code(listDto.get(i).getOrg());
                insertDTO.setVir_no(listDto.get(i).getVir_no());
                insertDTO.setRs_dt(listDto.get(i).getRs_dt());
                insertDTO.setPrint_prot_name("更生保護分會");
                insertDTO.setPrint_date(dd.substring(0,10));
                insertDTO.setPrint_user(payload.getSignUser());
                insertDTOList.add(insertDTO);
            }
            repository.insertToSUP_AfterCare_Print_Log(insertDTOList);
        }

        try {
            reportEnvironment.setFormat(ReportFormat.FORMAT_PDF);
            reportEnvironment.setFile("report01.jrxml");
            return reportGenerator.generate(reportEnvironment, listDto, params);

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new BusinessException("can't generate file!");
        }


    }
}
