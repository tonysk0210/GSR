package com.hn2.report.controller;

import com.hn2.report.payload.Reprot01Payload;
import com.hn2.report.service.Report01Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/aca/jail")
@Slf4j
public class Report01Controller {
    @Autowired
    Report01Service service;

    @PostMapping(value = "/report01")
    public ResponseEntity<Object> getListByCustom(@RequestBody Reprot01Payload payload) {

        //payload.setPrintUser(principalAccessor.getPrincipal().getCname());
        log.info(payload.toString());
        byte[] bytes = service.getReport(payload);

        return ResponseEntity.ok()
                .header("Content-Type", "application/octet-stream; charset=UTF-8")
                .body(bytes);
    }
}
