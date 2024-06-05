package com.hn2.cms.service.impl;

import com.hn2.cms.dto.Aca1002QueryDto;
import com.hn2.cms.payload.aca1002.*;
import com.hn2.cms.repository.Aca1002Repository;
import com.hn2.cms.repository.SupAfterCareRepository;
import com.hn2.cms.service.Aca1002Service;
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
public class Aca1002ServiceImpl implements Aca1002Service {
    @Autowired
    PagePayloadValidator pagePayloadValidator;
    @Autowired
    Aca1002Repository Aca1002Repository;
    @Autowired
    SupAfterCareRepository supAfterCareRepository;

    @Override
    public DataDto<List<Aca1002QueryDto>> queryList(GeneralPayload<Aca1002QueryPayload> payload) {
        var dataPayload = payload.getData();
        var pagePayload = payload.getPage();

        int count = Aca1002Repository.countSearch(dataPayload);
        if (pagePayload != null && !pagePayloadValidator.checkPageExist(pagePayload, count))
            throw new BusinessException(ErrorType.RESOURCE_NOT_FOUND, "請求分頁不存在");

        var dataList = Aca1002Repository.queryList(dataPayload, pagePayload);

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
    public DataDto<Void> signList(GeneralPayload<Aca1002SignPayload> payload) {
        var payloadData = payload.getData();
        var entityList = supAfterCareRepository.findAllById(payloadData.getItemIdList());

        for (var v : entityList){
            if("0".equals(v.getSignState())){
                v.setAcaReceiptDate(payloadData.getAcaDate());
                v.setAcaUser(payloadData.getAcaReceiptUser());
                v.setSignState("1");

                //todo 需要寫入正式資料
            }
        }

        supAfterCareRepository.saveAll(entityList);

        return new DataDto<>(null, new ResponseInfo(1, "儲存成功"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataDto<Void> transPort(GeneralPayload<Aca1002TransPortPayload> payload) {
        var payloadData = payload.getData();
        var entityList = supAfterCareRepository.findAllById(payloadData.getItemIdList());

        for (var v : entityList) {
            v.setSignProtName(payloadData.getSignProtName());
            v.setSignProtNo(payloadData.getSignProtNo());
            v.setAcaReceiptDate(null);
            v.setAcaUser(null);
            v.setSignState("0");
        };

        supAfterCareRepository.saveAll(entityList);

        return new DataDto<>(null, new ResponseInfo(1, "儲存成功"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataDto<Void> goBack(GeneralPayload<Aca1002GoBackPayload> payload) {
        var payloadData = payload.getData();
        var entityList = supAfterCareRepository.findAllById(payloadData.getItemIdList());

        for (var v : entityList) {
            v.setAcaReceiptDate(null);
            v.setAcaUser(null);
            v.setSignState("0");
        };

        supAfterCareRepository.saveAll(entityList);

        return new DataDto<>(null, new ResponseInfo(1, "儲存成功"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataDto<Void> reassign(GeneralPayload<Aca1002ReassignPayload> payload) {
        var payloadData = payload.getData();
        var entityList = supAfterCareRepository.findAllById(payloadData.getItemIdList());

        for (var v : entityList) {
            v.setAcaReceiptDate(null);
            v.setAcaUser(payloadData.getAcaReceiptUser());
            v.setSignState("0");
        };

        supAfterCareRepository.saveAll(entityList);

        return new DataDto<>(null, new ResponseInfo(1, "儲存成功"));
    }
}
