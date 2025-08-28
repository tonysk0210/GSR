package com.hn2.cms.controller;

import com.hn2.cms.dto.aca2003.Aca2003DetailView;
import com.hn2.cms.dto.aca2003.Aca2003QueryDto;
import com.hn2.cms.dto.aca2003.Aca2003SaveResponse;
import com.hn2.cms.payload.aca2003.Aca2003QueryPayload;
import com.hn2.cms.payload.aca2003.Aca2003SavePayload;
import com.hn2.cms.service.aca2003.Aca2003Service;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

// 毒品濫用資料表建檔
@RestController
@RequestMapping("/aca/aca2003")
public class Aca2003Controller {
    private final Aca2003Service service;

    @Autowired
    public Aca2003Controller(Aca2003Service service) {
        this.service = service;
    }

    @PostMapping("/save")
    public DataDto<Aca2003SaveResponse> save(@RequestBody GeneralPayload<Aca2003SavePayload> payload) {
        return service.save(payload);
    }

    // 新增：依 ID 查詢詳情
    @PostMapping("/query")
    public ResponseEntity<DataDto<Aca2003QueryDto>> query(@Valid @RequestBody GeneralPayload<Aca2003QueryPayload> payload) {
        return ResponseEntity.ok(service.queryById(payload));
    }
}
