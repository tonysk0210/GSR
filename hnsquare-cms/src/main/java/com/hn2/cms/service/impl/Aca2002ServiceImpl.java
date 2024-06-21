package com.hn2.cms.service.impl;

import com.hn2.cms.dto.Aca2002CrmRecQueryDto;
import com.hn2.cms.model.CrmRecEntity;
import com.hn2.cms.payload.Aca2002.Aca2002QueryPayload;
import com.hn2.cms.repository.CrmRecRepository;
import com.hn2.cms.service.Aca2002Service;
import com.hn2.core.dto.DataDto;
import com.hn2.core.dto.ResponseInfo;
import com.hn2.core.payload.GeneralPayload;
import com.hn2.util.BusinessException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Aca2002ServiceImpl implements Aca2002Service {

    @Autowired
    CrmRecRepository crmRecRepository;
    @Autowired
    ModelMapper modelMapper;


    @Override
    public DataDto<Aca2002CrmRecQueryDto> query(GeneralPayload<Aca2002QueryPayload> payload) {
        Aca2002QueryPayload dataPayload = payload.getData();
        String acaCardNo = dataPayload.getAcaCardNo();


        CrmRecEntity crmData = (CrmRecEntity) crmRecRepository.findByAcaCartdNo( acaCardNo)
                .orElseThrow( () -> new BusinessException(("查不到資料")));


        return new DataDto<>(modelMapper.map(crmData,Aca2002CrmRecQueryDto.class) , new ResponseInfo(1, "儲存成功"));
    }


}
