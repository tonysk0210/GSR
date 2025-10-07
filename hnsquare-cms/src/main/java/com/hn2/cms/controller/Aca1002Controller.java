package com.hn2.cms.controller;

import com.hn2.cms.dto.aca1002.Aca1002ComparyAcaDto;
import com.hn2.cms.dto.aca1002.Aca1002QueryDto;
import com.hn2.cms.payload.aca1002.*;
import com.hn2.cms.service.aca1002.Aca1002Service;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/aca/aca1002")
public class Aca1002Controller {

    @Autowired
    Aca1002Service aca1002Service;

    /**
     * 承辦人簽收資料查詢
     *
     * @param payload payload
     * @return 結果列表
     */
    @PostMapping("/queryList")
    public ResponseEntity<DataDto<List<Aca1002QueryDto>>> queryList(
            @Valid @RequestBody GeneralPayload<Aca1002QueryPayload> payload) {
        return ResponseEntity.ok(aca1002Service.queryList(payload));
    }

    /**
     * 承辦人簽收
     *
     * @param payload payload
     * @return 結果列表
     */
    @PostMapping("/signList")
    public ResponseEntity<DataDto<Void>> signList(
            @Valid @RequestBody GeneralPayload<Aca1002SignPayload> payload) {
        return ResponseEntity.ok(aca1002Service.signList(payload));
    }

    /**
     * 發文資料-轉簽收分會
     *
     * @param payload payload
     * @return 結果列表
     */
    @PostMapping("/transPort")
    public ResponseEntity<DataDto<Void>> transPort(
            @Valid @RequestBody GeneralPayload<Aca1002TransPortPayload> payload) {
        return ResponseEntity.ok(aca1002Service.transPort(payload));
    }


    /**
     * 退回
     *
     * @param payload payload
     * @return 結果列表
     */
    @PostMapping("/goBack")
    public ResponseEntity<DataDto<Void>> goBack(
            @Valid @RequestBody GeneralPayload<Aca1002GoBackPayload> payload) {
        return ResponseEntity.ok(aca1002Service.goBack(payload));
    }

    /**
     * 轉分派
     *
     * @param payload payload
     * @return 結果列表
     */
    @PostMapping("/reassign")
    public ResponseEntity<DataDto<Void>> reassign(
            @Valid @RequestBody GeneralPayload<Aca1002ReassignPayload> payload) {
        return ResponseEntity.ok(aca1002Service.reassign(payload));
    }

    /**
     * 轉分派
     *
     * @param payload payload
     * @return 結果列表
     */
    @PostMapping("/compareAca")
    public ResponseEntity<DataDto<Aca1002ComparyAcaDto>> compareAca(
            @Valid @RequestBody GeneralPayload<Aca1002CompareAcaPayload> payload) {
        return ResponseEntity.ok(aca1002Service.compareAca(payload));
    }



}
