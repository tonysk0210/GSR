package com.hn2.cms.controller;

import com.hn2.cms.dto.aca4001.Aca4001AuditQueryDto;
import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto;
import com.hn2.cms.dto.aca4001.Aca4001RestoreQueryDto;
import com.hn2.cms.payload.aca4001.Aca4001ErasePayload;
import com.hn2.cms.payload.aca4001.Aca4001EraseQueryPayload;
import com.hn2.cms.payload.aca4001.Aca4001RestorePayload;
import com.hn2.cms.payload.aca4001.Aca4001RestoreQueryPayload;
import com.hn2.cms.service.aca4001.Aca4001Service;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/aca/aca4001")
@RequiredArgsConstructor
@Validated
public class Aca4001Controller {

    private final Aca4001Service service;

    @PostMapping("/eraseQuery")
    public ResponseEntity<DataDto<Aca4001EraseQueryDto>> eraseQuery(@Valid @RequestBody GeneralPayload<Aca4001EraseQueryPayload> payload) {
        DataDto<Aca4001EraseQueryDto> result = service.eraseQuery(payload);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/erase")
    public ResponseEntity<DataDto<Void>> erase(@Valid @RequestBody GeneralPayload<Aca4001ErasePayload> payload, HttpServletRequest request) {

        Aca4001ErasePayload req = payload.getData();

        // 從 HttpServletRequest 取得用戶端的 IP 位址
        String userIp = request.getRemoteAddr();

        // 呼叫 service 的 erase 方法，傳入所需參數：
        // - payload (原始的 request payload，可能包含 meta 或其他資訊)
        // - operatorUserId (操作人員的 ID)
        // - operatorUserName (操作人員的名稱)
        // - userIp (操作者的 IP)
        // - operatorBranchId (操作人員所屬分會/部門代號)
        DataDto<Void> result = service.erase(payload, req.getOperatorUserId(), userIp);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/restoreQuery")
    public ResponseEntity<DataDto<Aca4001RestoreQueryDto>> restoreQuery(@Valid @RequestBody GeneralPayload<Aca4001RestoreQueryPayload> payload) {
        DataDto<Aca4001RestoreQueryDto> result = service.restoreQuery(payload);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/restore")
    public ResponseEntity<DataDto<Void>> restore(@Valid @RequestBody GeneralPayload<Aca4001RestorePayload> payload, HttpServletRequest request) {
        var req = payload.getData();
        String userIp = request.getRemoteAddr();

        DataDto<Void> result = service.restore(payload, req.getOperatorUserId(), userIp);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/auditQuery")
    public ResponseEntity<DataDto<Aca4001AuditQueryDto>> auditQuery() {
        return ResponseEntity.ok(service.auditQuery());
    }
}