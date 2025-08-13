package com.hn2;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ping-assessment")
public class PingController {
    private final AssessmentHelloService svc;

    public PingController(AssessmentHelloService svc) {
        this.svc = svc;
    }

    @GetMapping
    public String ping() {
        return svc.hello();
    }
}