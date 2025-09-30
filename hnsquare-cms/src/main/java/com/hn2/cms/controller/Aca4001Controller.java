package com.hn2.cms.controller;

import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto;
import com.hn2.cms.payload.aca4001.Aca4001EraseQueryPayload;
import com.hn2.cms.service.aca4001.Aca4001Service;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/aca/aca4001")
@RequiredArgsConstructor
@Validated
public class Aca4001Controller {

    private final Aca4001Service service;

    /**
     * 依條件查滿18歲前可清除之 ProRec/CrmRec 清單
     */
    @PostMapping("/eraseQuery")
    public ResponseEntity<DataDto<Aca4001EraseQueryDto>> eraseQuery(@Valid @RequestBody GeneralPayload<Aca4001EraseQueryPayload> payload) {
        DataDto<Aca4001EraseQueryDto> result = service.eraseQuery(payload);
        return ResponseEntity.ok(result);
    }
}