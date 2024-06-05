package com.hn2.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CoreController {

    @GetMapping("/heartCheck")
    public ResponseEntity<String> heartCheck() {
        return ResponseEntity.ok("OK");
    }

}
