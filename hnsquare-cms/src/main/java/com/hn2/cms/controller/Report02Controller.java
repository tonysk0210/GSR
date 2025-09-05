package com.hn2.cms.controller;


import com.hn2.cms.dto.report02.Report02Dto;
import com.hn2.cms.payload.report02.Report02Payload;
import com.hn2.cms.service.report02.Report02Service;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/aca/report02")
public class Report02Controller {

    private final Report02Service service;

    public Report02Controller(Report02Service service) {
        this.service = service;
    }

    /**
     * 取得 Report02 聚合報表資料
     * <p>
     * POST /api/report02/query
     * {
     * "from": "2025-06-01",
     * "to":   "2025-09-01"
     * }
     */
    @PostMapping("/query")
    public ResponseEntity<DataDto<Report02Dto>> query(@Valid @RequestBody GeneralPayload<Report02Payload> payload) {
        DataDto<Report02Dto> result = service.query(payload);
        return ResponseEntity.ok(result);
    }

}