package com.hn2.cms.controller;

import com.hn2.cms.dto.AcaJailQueryDto;
import com.hn2.cms.model.SupAfterCareEntity;
import com.hn2.cms.payload.AcaJailQueryPayload;
import com.hn2.cms.payload.AcaJailSignPayload;
import com.hn2.cms.payload.AcaJailTransPortPayload;
import com.hn2.cms.repository.SupAfterCareRepository;
import com.hn2.cms.service.AcaJailService;
import com.hn2.core.dto.DataDto;
import com.hn2.core.dto.ResponseInfo;
import com.hn2.core.payload.GeneralPayload;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/aca/jail")
@Data
public class AcaJailController {

    @Autowired
    AcaJailService acaJailService;

    /**
     * 發文資料查詢
     *
     * @param payload payload
     * @return 結果列表
     */
    @PostMapping("/queryList")
    public ResponseEntity<DataDto<List<AcaJailQueryDto>>> queryList(
            @Valid @RequestBody GeneralPayload<AcaJailQueryPayload> payload) {
        return ResponseEntity.ok(acaJailService.queryList(payload));
    }

    /**
     * 發文資料簽收
     *
     * @param payload payload
     * @return 結果列表
     */
    @PostMapping("/signList")
    public ResponseEntity<DataDto<Void>> signList(
            @Valid @RequestBody GeneralPayload<AcaJailSignPayload> payload) {
        return ResponseEntity.ok(acaJailService.signList(payload));
    }

    /**
     * 發文資料-轉簽收分會
     *
     * @param payload payload
     * @return 結果列表
     */
    @PostMapping("/transPort")
    public ResponseEntity<DataDto<Void>> transPort(
            @Valid @RequestBody GeneralPayload<AcaJailTransPortPayload> payload) {
        return ResponseEntity.ok(acaJailService.transPort(payload));
    }



    @Autowired
    SupAfterCareRepository supAfterCareRepository;


    @GetMapping("/test")
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<DataDto<Void>> test() {

        var e = new SupAfterCareEntity();
        e.setOrgCode("");
        e.setVirNo(0L);
        e.setRsDt(LocalDate.now());
        e.setSignState("0");
        e.setSignProtName("員林分會");
        e.setCallNo("A074");
        e.setCrDateTime(LocalDate.now());
        e.setNamCname("鎮許方");
        e.setNamSex("M");
        e.setNamHaddrText("新竹縣新豐鄉明德巷13號");
        supAfterCareRepository.save(e);

        DataDto<Void> dto = new DataDto<>(null, new ResponseInfo(1, "儲存成功"));
        return ResponseEntity.ok(dto);
    }

}
