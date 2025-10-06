package com.hn2.cms.controller;

import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto;
import com.hn2.cms.payload.aca4001.Aca4001ErasePayload;
import com.hn2.cms.payload.aca4001.Aca4001EraseQueryPayload;
import com.hn2.cms.payload.aca4001.Aca4001RestorePayload;
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

    /**
     * 依條件查滿18歲前可清除之 ProRec/CrmRec 清單
     */
    @PostMapping("/eraseQuery")
    public ResponseEntity<DataDto<Aca4001EraseQueryDto>> eraseQuery(@Valid @RequestBody GeneralPayload<Aca4001EraseQueryPayload> payload) {
        DataDto<Aca4001EraseQueryDto> result = service.eraseQuery(payload);
        return ResponseEntity.ok(result);
    }

    /**
     * 整體用途：
     * - 定義 POST /erase 端點，用於觸發「塗銷」流程
     * - 從 HttpServletRequest 取得操作者 IP
     * - 呼叫 service.erase(...) 交給服務層執行實際塗銷、鏡像與稽核
     */
    @PostMapping("/erase")
    public ResponseEntity<DataDto<Void>> erase(@Valid @RequestBody GeneralPayload<Aca4001ErasePayload> payload, HttpServletRequest request) {

        Aca4001ErasePayload req = payload.getData(); // 從 GeneralPayload 取出真正的 data（業務欄位）
        String userIp = request.getRemoteAddr(); // 從 HttpServletRequest 取得用戶端的 IP 位址

        DataDto<Void> result = service.erase(payload, req.getOperatorUserId(), userIp);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/restore")
    public ResponseEntity<DataDto<Void>> restore(@Valid @RequestBody GeneralPayload<Aca4001RestorePayload> payload, HttpServletRequest request) {
        var req = payload.getData();
        String userIp = request.getRemoteAddr();

        DataDto<Void> result = service.restore(payload, req.getOperatorUserId(), userIp);
        return ResponseEntity.ok(result);
    }
}