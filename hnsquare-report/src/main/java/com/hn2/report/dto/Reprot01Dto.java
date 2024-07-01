package com.hn2.report.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(description = "更生保護通知書")
public class Reprot01Dto {
    private String id;
    /**發文機關*/
    private String org_code;
    private String org;
    /**虛擬編號*/
    private String vir_no;
    /**發文日期*/
    private String rs_dt;
    /**承辦人*/
    private String tr_user;
    /**承辦人電話*/
    private String tr_tel;
    /**承辦人email*/
    private String tr_email;
    /**受文者*/
    private String prot_name;
    /**姓名*/
    private String nam_cname;
    /**性別*/
    private String nam_sex;
    /**出生日期**/
    private String nam_brdt;
    /**出生地**/
    private String nam_bonp_text;
    /**國民身分證統一編號*/
    private String nam_idno;
    /**戶籍地址*/
    private String nam_haddr_text;
    /**聯絡電話*/
    private String nam_tel;
    /**以前職業*/
    private String docu_proc_text;
    /**教育程度*/
    private String nam_educ_text;
    /**罪名*/
    private String nam_cnames_text;
    /**刑期*/
    private String nam_pen_text;
    /**入監所或被收容日期*/
    private String nam_mvdt;
    /**預計獲釋日期*/
    private String docu_otdt;
    /**預計獲釋原因*/
    private String docu_otop_text;
    /**專長*/
    private String skill_text;
    /**需要何種更生保護*/
    private String protect_text;
    /**取得證照職種*/
    private String docu_license_text;
    /**宗教信仰*/
    private String relig_text;
    /**是否有暴力傾向*/
    private String docu_violent_text;
    /**經濟狀況**/
    private String economic_text;
    /**婚姻狀況**/
    private String marriage_text;
    /**其他*/
    private String docu_remark;
    /**聯絡人姓名*/
    private String reld_name;
    /**連絡人關係*/
    private String reld_no_text;
    /**連絡人電話*/
    private String reld_tel1;
    /**聯絡人手機*/
    private String reld_tel2;
    /**聯絡人地址*/
    private String reld_addr;

}
