package com.hn2.cms.controller;

import com.hn2.cms.payload.Aca2001.Aca2001SavePayload;
import com.hn2.cms.service.Aca2001Service;
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
@RequestMapping("/aca/aca2001")
public class Aca2001Controller {

    @Autowired
    Aca2001Service aca2001Service;



    @PostMapping("/save")
    public ResponseEntity<DataDto<Void>> save(
            @Valid @RequestBody GeneralPayload<Aca2001SavePayload> payload) {
        return ResponseEntity.ok(aca2001Service.save(payload));
    }

}
