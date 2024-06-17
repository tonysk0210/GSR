package com.hn2.cms.service.impl;

import com.hn2.cms.model.AcaBrdEntity;
import com.hn2.cms.model.SupAfterCareEntity;
import com.hn2.cms.payload.Aca2001.Aca2001SavePayload;
import com.hn2.cms.repository.AcaBrdRepository;
import com.hn2.cms.repository.SupAfterCareRepository;
import com.hn2.cms.service.Aca2001Service;
import com.hn2.core.dto.DataDto;
import com.hn2.core.dto.PageInfo;
import com.hn2.core.dto.ResponseInfo;
import com.hn2.core.payload.GeneralPayload;
import com.hn2.core.util.PagePayloadValidator;
import com.hn2.util.BusinessException;
import com.hn2.util.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class Aca2001ServiceImpl implements Aca2001Service {

    @Autowired
    SupAfterCareRepository supAfterCareRepository;
    @Autowired
    AcaBrdRepository acaBrdRepository;


    @Override
    public DataDto<Void> save(GeneralPayload<Aca2001SavePayload> payload) {
        Aca2001SavePayload dataPayload = payload.getData();
        String itemId = dataPayload.getNam().getItemId();

        //1.查詢出A:矯正署資料 透過 itemId 查矯正署資料
        SupAfterCareEntity namData = supAfterCareRepository.findById(itemId).orElseThrow( () -> new BusinessException(("查不到資料")));
        //2.查詢出B:個案資料 鈄過查矯正署資料 身分證及簽收機關查詢個案
        AcaBrdEntity acaData = (AcaBrdEntity) acaBrdRepository.findByCreatedByBranchIdAndAcaIdNo(namData.getSignProtNo(), namData.getNamIdno())
                .orElseThrow( () -> new BusinessException(("查不到資料")));


        namData.setAcaState("3");
        namData.setSignState("3");
        namData.setUpUser(acaData.getModifiedByUserId());
        namData.setUpDateTime(LocalDate.now());

        supAfterCareRepository.save(namData);




        return new DataDto<>(null, new ResponseInfo(1, "儲存成功"));
    }


}
