package com.hn2.cms.controller;


import com.hn2.cms.dto.Aca2002CrmRecQueryDto;
import com.hn2.cms.payload.aca2002.Aca2002QueryListPayload;
import com.hn2.cms.payload.aca2002.Aca2002QueryPayload;
import com.hn2.cms.payload.aca2002.Aca2002SavePayload;
import com.hn2.cms.service.Aca2002Service;
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
@RequestMapping("/aca/aca2002")
public class Aca2002Controller {

    @Autowired
    Aca2002Service aca2002Service;


    @PostMapping("/query")
    public ResponseEntity<DataDto<Aca2002CrmRecQueryDto>> query(
            @Valid @RequestBody GeneralPayload<Aca2002QueryPayload> payload) {
        return ResponseEntity.ok(aca2002Service.query(payload));
    }

    @PostMapping("/queryList")
    public ResponseEntity<DataDto<List<Aca2002CrmRecQueryDto>>> queryList(
            @Valid @RequestBody GeneralPayload<Aca2002QueryListPayload> payload) {
        return ResponseEntity.ok(aca2002Service.queryList(payload));
    }


    @PostMapping("/save")
    public ResponseEntity<DataDto<Object>> save(
            @Valid @RequestBody GeneralPayload<Aca2002SavePayload> payload) {
        return ResponseEntity.ok(aca2002Service.save(payload));
    }



}
