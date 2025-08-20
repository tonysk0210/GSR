package com.hn2.cms.controller;

import com.hn2.cms.dto.aca3001.ProAdoptQueryRequest;
import com.hn2.cms.dto.aca3001.ProAdoptViewResponse;
import com.hn2.cms.service.aca3001.ProAdoptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/aca/aca3001")
public class Aca3001Controller {

    private final ProAdoptService service;

    public Aca3001Controller(ProAdoptService service) {
        this.service = service;
    }

    @PostMapping("/query")
    public ResponseEntity<ProAdoptViewResponse> query(@RequestBody ProAdoptQueryRequest req) {
        if (req.getProRecId() == null || req.getProRecId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        ProAdoptViewResponse resp = service.buildProfileOnlyView(req.getProRecId());
        if (resp == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resp);
    }

}
