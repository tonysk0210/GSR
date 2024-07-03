package com.hn2.cms.service.impl;

import com.hn2.cms.dto.Aca2002CrmRecQueryDto;
import com.hn2.cms.dto.SysUserQueryDto;
import com.hn2.cms.model.CrmRecEntity;
import com.hn2.cms.payload.Aca2002.Aca2002QueryListPayload;
import com.hn2.cms.payload.Aca2002.Aca2002QueryPayload;
import com.hn2.cms.payload.Aca2002.Aca2002SavePayload;
import com.hn2.cms.repository.CrmRecRepository;
import com.hn2.cms.service.Aca2002Service;
import com.hn2.cms.service.SysService;
import com.hn2.core.dto.DataDto;
import com.hn2.core.dto.ResponseInfo;
import com.hn2.core.payload.GeneralPayload;
import com.hn2.util.BusinessException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class Aca2002ServiceImpl implements Aca2002Service {

    @Autowired
    CrmRecRepository crmRecRepository;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    SysService sysService;


    @Override
    public DataDto<Aca2002CrmRecQueryDto> query(GeneralPayload<Aca2002QueryPayload> payload) {
        Aca2002QueryPayload dataPayload = payload.getData();
        String crmRecId = dataPayload.getId();

        CrmRecEntity crmData = (CrmRecEntity) crmRecRepository.findById(crmRecId)
                .orElseThrow( () -> new BusinessException(("查不到資料")));

        return new DataDto<>(modelMapper.map(crmData,Aca2002CrmRecQueryDto.class) , new ResponseInfo(1, "儲存成功"));

    }

    @Override
    public DataDto<List<Aca2002CrmRecQueryDto>> queryList(GeneralPayload<Aca2002QueryListPayload> payload) {
        Aca2002QueryListPayload dataPayload = payload.getData();
        String acaCardNo = dataPayload.getAcaCardNo();

        List<CrmRecEntity> crmDataList = crmRecRepository.findByAcaCardNo(acaCardNo)
                .orElseThrow(() -> new BusinessException("查不到資料"));

        //List<Aca2002CrmRecQueryDto> dataList = modelMapper.map(crmData,Aca2002CrmRecQueryDto.class);
        List<Aca2002CrmRecQueryDto> dataList = crmDataList.stream()
                .map(entity -> modelMapper.map(entity, Aca2002CrmRecQueryDto.class))
                .collect(Collectors.toList());

        return new DataDto<>( dataList, new ResponseInfo(1, "查詢成功"));
    }

    @Override
    public DataDto<Object> save(GeneralPayload<Aca2002SavePayload> payload) {
        CrmRecEntity data= payload.getData().getCrm();
        data.setId(genNewCrmRecId(data.getCreatedByBranchId()));
        data.setCreatedByUserId(sysService.convertUsernameToUserId(data.getCreatedByUserId()));
        data.setModifiedByUserId(sysService.convertUsernameToUserId(data.getModifiedByUserId()));

        crmRecRepository.save(data);
        return new DataDto<>(null,  new ResponseInfo(1, data.getAcaCardNo() +":儲存成功"));
    }

    /**
     * 個案代碼
     * 編碼方式： 分會代碼(1碼) + 西元年月(6碼) + 流水號(5碼) 例如：A20131200001
     * @param createdByBranchId
     * @return
     */
    private String genNewCrmRecId(String createdByBranchId) {
        Date date = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMM");
        String datestr = sf.format(date);
        String key = createdByBranchId + datestr ;
        String no = crmRecRepository.genNewId(key+ "%'");
        return key + no;
    }

}
