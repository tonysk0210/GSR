package com.hn2.cms.controller;

import com.hn2.cms.dto.SysUserQueryDto;
import com.hn2.cms.service.SysService;
import com.hn2.core.dto.DataDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/aca/sys")
public class SysController {

    @Autowired
    SysService sysService;

    /**
     * 承辦人簽收資料查詢
     *
     * @param unit 分會別
     * @return 結果列表
     */
    @GetMapping("/user/{unit}")
    public ResponseEntity<DataDto<List<SysUserQueryDto>>> queryList(
            @PathVariable String unit) {
        SysService sysService = new SysService() {
            @Override
            public DataDto<List<SysUserQueryDto>> queryList(String unit) {
                return null;
            }
        };
        DataDto<List<SysUserQueryDto>> result = sysService.queryList(unit);
        return ResponseEntity.ok(result);
    }



}
