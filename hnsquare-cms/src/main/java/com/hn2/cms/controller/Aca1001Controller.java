package com.hn2.cms.controller;

import com.hn2.cms.dto.Aca1001QueryDto;
import com.hn2.cms.payload.aca1001.Aca1001QueryPayload;
import com.hn2.cms.payload.aca1001.Aca1001SignPayload;
import com.hn2.cms.payload.aca1001.Aca1001TransPortPayload;
import com.hn2.cms.service.Aca1001Service;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/aca/jail/receipt/")
public class Aca1001Controller {

    @Autowired
    Aca1001Service aca1001Service;


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
    public ResponseEntity<DataDto<List<Aca1001QueryDto>>> queryList(
            @Valid @RequestBody GeneralPayload<Aca1001QueryPayload> payload) {
        return ResponseEntity.ok(aca1001Service.queryList(payload));
    }

    /**
     * 發文資料簽收
     *
     * @param payload payload
     * @return 結果列表
     */
    @PostMapping("/signList")
    public ResponseEntity<DataDto<Void>> signList(
            @Valid @RequestBody GeneralPayload<Aca1001SignPayload> payload) {
        return ResponseEntity.ok(aca1001Service.signList(payload));
    }

    /**
     * 發文資料-轉簽收分會
     *
     * @param payload payload
     * @return 結果列表
     */
    @PostMapping("/transPort")
    public ResponseEntity<DataDto<Void>> transPort(
            @Valid @RequestBody GeneralPayload<Aca1001TransPortPayload> payload) {
        return ResponseEntity.ok(aca1001Service.transPort(payload));
    }

}
