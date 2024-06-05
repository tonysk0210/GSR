package com.hn2.cms.controller;

import com.hn2.cms.dto.AcaJailQueryDto;
import com.hn2.cms.payload.AcaJailQueryPayload;
import com.hn2.cms.payload.AcaJailSignPayload;
import com.hn2.cms.payload.AcaJailTransPortPayload;
import com.hn2.cms.service.AcaJailService;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/aca/jail")
public class AcaJailController {

    @Autowired
    AcaJailService acaJailService;


    @GetMapping("/heartCheck")
    public ResponseEntity<String> heartCheck() {
        return ResponseEntity.ok("OK");
    }

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

}
