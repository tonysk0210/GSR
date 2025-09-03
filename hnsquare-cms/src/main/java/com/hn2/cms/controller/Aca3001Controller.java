package com.hn2.cms.controller;

import com.hn2.cms.dto.aca3001.Aca3001QueryDto;
import com.hn2.cms.dto.aca3001.Aca3001SaveResponse;
import com.hn2.cms.payload.aca3001.Aca3001DeletePayload;
import com.hn2.cms.payload.aca3001.Aca3001QueryPayload;
import com.hn2.cms.payload.aca3001.Aca3001SavePayload;
import com.hn2.cms.service.aca3001.Aca3001Service;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

// 認輔資料表建檔
@RestController
@RequestMapping("/aca/aca3001")
public class Aca3001Controller {

    private final Aca3001Service service;

    @Autowired
    public Aca3001Controller(Aca3001Service service) {
        this.service = service;
    }

    //Query API
    @PostMapping("/query")
    public ResponseEntity<DataDto<Aca3001QueryDto>> query(@Valid @RequestBody GeneralPayload<Aca3001QueryPayload> payload) {
        DataDto<Aca3001QueryDto> result = service.query(payload);
        return ResponseEntity.ok(result);
    }

    //Save API
    @PostMapping("/save")
    public ResponseEntity<DataDto<Aca3001SaveResponse>> save(
            @Valid @RequestBody GeneralPayload<Aca3001SavePayload> payload) {
        DataDto<Aca3001SaveResponse> result = service.savejpa(payload);
        return ResponseEntity.ok(result);
    }

    //Delete API
    @DeleteMapping("/delete")
    public ResponseEntity<DataDto<Void>> delete(
            @Valid @RequestBody GeneralPayload<Aca3001DeletePayload> payload) {
        DataDto<Void> result = service.delete(payload);
        return ResponseEntity.ok(result);
    }
}