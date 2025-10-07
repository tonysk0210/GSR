package com.hn2.cms.service.aca1001;

import com.hn2.cms.dto.aca1001.Aca1001QueryDto;
import com.hn2.cms.model.SupAfterCareEntity;
import com.hn2.cms.payload.aca1001.Aca1001AssignPayload;
import com.hn2.cms.payload.aca1001.Aca1001QueryPayload;
import com.hn2.cms.payload.aca1001.Aca1001SignPayload;
import com.hn2.cms.payload.aca1001.Aca1001TransPortPayload;
import com.hn2.cms.repository.aca1001.Aca1001Repository;
import com.hn2.cms.repository.SupAfterCareRepository;
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
public class Aca1001ServiceImpl implements Aca1001Service {
    @Autowired
    PagePayloadValidator pagePayloadValidator;
    @Autowired
    Aca1001Repository aca1001Repository;
    @Autowired
    SupAfterCareRepository supAfterCareRepository;

    @Override
    public DataDto<List<Aca1001QueryDto>> queryList(GeneralPayload<Aca1001QueryPayload> payload) {
        var dataPayload = payload.getData();
        var pagePayload = payload.getPage();

        int count = aca1001Repository.countSearch(dataPayload);
        if (pagePayload != null && !pagePayloadValidator.checkPageExist(pagePayload, count))
            throw new BusinessException(ErrorType.RESOURCE_NOT_FOUND, "請求分頁不存在");

        var dataList = aca1001Repository.queryList(dataPayload, pagePayload);

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
    public DataDto<Void> signList(GeneralPayload<Aca1001SignPayload> payload) {
        var payloadData = payload.getData();
        var entityList = supAfterCareRepository.findAllById(payloadData.getItemIdList());

        for (var v : entityList){

                v.setSignDate(payloadData.getSignDate());
                v.setSignUser(payloadData.getSignUser());
                v.setSignState("1");

        }

        supAfterCareRepository.saveAll(entityList);

        return new DataDto<>(null, new ResponseInfo(1, "儲存成功"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataDto<Void> transPort(GeneralPayload<Aca1001TransPortPayload> payload) {
        var payloadData = payload.getData();
        var entityList = supAfterCareRepository.findAllById(payloadData.getItemIdList());

        for (var v : entityList) {
            v.setSignProtName(payloadData.getSignProtName());
            v.setSignProtNo(payloadData.getSignProtNo());
            v.setSignDate(null);
            v.setSignState("0");

            v.setAcaReceiptDate(null);
            v.setAcaUser(null);
            v.setAcaState("0");

        };

        supAfterCareRepository.saveAll(entityList);

        return new DataDto<>(null, new ResponseInfo(1, "儲存成功"));
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataDto<Void> assign(GeneralPayload<Aca1001AssignPayload> payload) {
        Aca1001AssignPayload payloadData = payload.getData();
        List<SupAfterCareEntity> entityList = supAfterCareRepository.findAllById(payloadData.getItemIdList());

        for (var v : entityList) {
            v.setSignDate(payloadData.getSignDate());
            v.setSignUser(payloadData.getSignUser());
            v.setSignState("1");

            v.setAcaReceiptDate(null);
            v.setAcaUser(payloadData.getAcaUser());
            v.setAcaState("0");
        };

        supAfterCareRepository.saveAll(entityList);

        return new DataDto<>(null, new ResponseInfo(1, "儲存成功"));
    }
}
