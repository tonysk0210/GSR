package com.hn2.cms.controller;

import com.hn2.cms.dto.aca3001.Aca3001QueryDto;
import com.hn2.cms.payload.aca3001.Aca3001QueryPayload;
import com.hn2.cms.service.aca3001.Aca3001Service;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/aca/aca3001")
public class Aca3001Controller {

    private final Aca3001Service service;

    @Autowired
    public Aca3001Controller(Aca3001Service service) {
        this.service = service;
    }

    @PostMapping("/query")
    public ResponseEntity<DataDto<Aca3001QueryDto>> query(@Valid @RequestBody GeneralPayload<Aca3001QueryPayload> payload) {
        DataDto<Aca3001QueryDto> result = service.query(payload);
        return ResponseEntity.ok(result);
    }
}