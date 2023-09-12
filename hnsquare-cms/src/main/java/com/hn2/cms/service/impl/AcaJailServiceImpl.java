package com.hn2.cms.service.impl;

import com.hn2.cms.dto.AcaJailQueryDto;
import com.hn2.cms.payload.AcaJailQueryPayload;
import com.hn2.cms.payload.AcaJailSignPayload;
import com.hn2.cms.payload.AcaJailTransPortPayload;
import com.hn2.cms.repository.AcaJailRepository;
import com.hn2.cms.repository.SupAfterCareRepository;
import com.hn2.cms.service.AcaJailService;
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

import java.util.List;

@Service
public class AcaJailServiceImpl implements AcaJailService {
    @Autowired
    PagePayloadValidator pagePayloadValidator;
    @Autowired
    AcaJailRepository acaJailRepository;
    @Autowired
    SupAfterCareRepository supAfterCareRepository;

    @Override
    public DataDto<List<AcaJailQueryDto>> queryList(GeneralPayload<AcaJailQueryPayload> payload) {
        var dataPayload = payload.getData();
        var pagePayload = payload.getPage();

        int count = acaJailRepository.countSearch(dataPayload);
        if (pagePayload != null && !pagePayloadValidator.checkPageExist(pagePayload, count))
            throw new BusinessException(ErrorType.RESOURCE_NOT_FOUND, "請求分頁不存在");

        var dataList = acaJailRepository.queryList(dataPayload, pagePayload);

        PageInfo pageInfo = new PageInfo();
        pageInfo.setTotalDatas((long) count);
        if(pagePayload != null){
            pageInfo.setCurrentPage(pagePayload.getPage());
            pageInfo.setPageItems(pagePayload.getPageSize());
            int i = count % pagePayload.getPageSize() == 0 ? 0 : 1;
            pageInfo.setTotalPages(count / pagePayload.getPageSize() + i);
        }

        return new DataDto<>(dataList, pageInfo, new ResponseInfo(1, "查詢成功"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataDto<Void> signList(GeneralPayload<AcaJailSignPayload> payload) {
        var payloadData = payload.getData();
        var entityList = supAfterCareRepository.findAllById(payloadData.getItemIdList());

        for (var v : entityList){
            if("1".equals(v.getSignState())){
                v.setSignDate(payloadData.getSingDate());
                v.setSignUser(payloadData.getSingUser());
                v.setSignState("1");
            }
        }

        supAfterCareRepository.saveAll(entityList);

        return new DataDto<>(null, new ResponseInfo(1, "儲存成功"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataDto<Void> transPort(GeneralPayload<AcaJailTransPortPayload> payload) {
        var payloadData = payload.getData();
        var entityList = supAfterCareRepository.findAllById(payloadData.getItemIdList());

        for (var v : entityList) v.setSignProtName(payloadData.getSignProtName());

        supAfterCareRepository.saveAll(entityList);

        return new DataDto<>(null, new ResponseInfo(1, "儲存成功"));
    }
}
