package com.hn2.cms.service.impl;

import com.hn2.cms.dto.SysUserQueryDto;
import com.hn2.cms.model.AcaBrdEntity;
import com.hn2.cms.model.SupAfterCareEntity;
import com.hn2.cms.payload.Aca2001.Aca2001SavePayload;
import com.hn2.cms.repository.AcaBrdRepository;
import com.hn2.cms.repository.SupAfterCareRepository;
import com.hn2.cms.repository.SysUserRepository;
import com.hn2.cms.service.Aca2001Service;
import com.hn2.core.dto.DataDto;
import com.hn2.core.dto.ResponseInfo;
import com.hn2.core.payload.GeneralPayload;
import com.hn2.util.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

@Service
public class Aca2001ServiceImpl implements Aca2001Service {

    @Autowired
    SupAfterCareRepository supAfterCareRepository;
    @Autowired
    AcaBrdRepository acaBrdRepository;
    @Autowired
    SysUserRepository sysUserRepository;

    @Override
    public DataDto<Void> save(GeneralPayload<Aca2001SavePayload> payload) {
        Aca2001SavePayload dataPayload = payload.getData();
        String itemId = dataPayload.getNam().getItemId();

        //1.查詢出A:矯正署資料 透過 itemId 查矯正署資料
        SupAfterCareEntity namData = supAfterCareRepository.findById(itemId).orElseThrow( () -> new BusinessException(("查不到資料")));
        //2.查詢出B:個案資料 鈄過查矯正署資料 身分證及簽收機關查詢個案
        AcaBrdEntity acaData = doSaveOrUpdateAcaBrdData(namData, payload.getData().getAca());

        namData.setAcaState("3");
        namData.setSignState("3");
        namData.setUpUser(acaData.getModifiedByUserId());
        namData.setUpDateTime(LocalDate.now());

        supAfterCareRepository.save(namData);

        return new DataDto<>(null, new ResponseInfo(1, "儲存成功"));
    }

    private AcaBrdEntity doSaveOrUpdateAcaBrdData(SupAfterCareEntity namData, AcaBrdEntity payloadAca) {

        Optional<AcaBrdEntity> acaDataOptional = acaBrdRepository.findByAcaIdNo( namData.getNamIdno());
        //  依查詢結果分為
        //    有資料：進行資料異動；注意建立資訊不可變更
        //    無資料：以payload建立個案資料(AcaBrd)，系統額外做個案代碼(ID)、建檔編號(ACACardNo)及isErase=0、isDelete=0動作。
        //    通用行為：payload的帳號欄位均要轉成帳號內碼。
        if (acaDataOptional.isEmpty()){
            //新增資料
            payloadAca.setId(genNewAcaBrdId(payloadAca.getCreatedByBranchId()));
            payloadAca.setAcaCardNo(genNewAcaCardNo(payloadAca.getCreatedByBranchId()));
            payloadAca.setCreatedByUserId(convertUsernameToUserId(payloadAca.getCreatedByUserId()));
            payloadAca.setModifiedByUserId(convertUsernameToUserId(payloadAca.getModifiedByUserId()));
            payloadAca.setIsErase(0);
            payloadAca.setIsDeleted(0);
        } else {
            //異動資料
            AcaBrdEntity oldAcaData = acaDataOptional.get();
            payloadAca.setId(oldAcaData.getId());
            payloadAca.setAcaCardNo(oldAcaData.getAcaCardNo());
            payloadAca.setCreatedByUserId(oldAcaData.getCreatedByUserId()); //建立資料不可異動
            payloadAca.setModifiedByUserId(convertUsernameToUserId(payloadAca.getModifiedByUserId()));
            //payloadAca.setIsErase(0);
            //payloadAca.setIsDeleted(0);
        }
        return acaBrdRepository.save(payloadAca);
    }

    /**
     * 前端傳入為員工編號(對應資料庫為username) ，轉換成資料內碼
     * @param username
     * @return
     */
    private String convertUsernameToUserId(String username) {
        if (StringUtils.isEmpty( username)){
            return "-1";
        }
        SysUserQueryDto user = sysUserRepository.queryByUsername(username);
        if (user == null ){
            return "-1";
        }
        return user.getUserId();
    }

    /**
     * 個案代碼
     * 編碼方式： 分會代碼(1碼) + 西元年月(6碼) + 流水號(5碼) 例如：A20131200001
     * @param createdByBranchId
     * @return
     */
    private String genNewAcaBrdId(String createdByBranchId) {
        Date date = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMM");
        String datestr = sf.format(date);
        String key = createdByBranchId + datestr ;
        String no = acaBrdRepository.genNewId(key+ "%'");
        return key + no;
    }

    /**
     * 建檔編號
     * 編碼方式： 分會代碼(1碼) + 西元年後兩碼(2碼) + 流水號(4碼) 例如：A130001
     * @param createdByBranchId
     * @return
     */
    private String genNewAcaCardNo(String createdByBranchId) {
        Date date = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yy");
        String datestr = sf.format(date);
        String key = createdByBranchId + datestr ;
        String no = acaBrdRepository.genNewAcaCardNo(key+ "%'");
        return key + no;
    }
}
