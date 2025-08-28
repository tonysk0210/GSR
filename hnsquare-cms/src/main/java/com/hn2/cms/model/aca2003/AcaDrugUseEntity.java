package com.hn2.cms.model.aca2003;

import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "AcaDrugUse")
@Data
public class AcaDrugUseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name="ACACardNo", nullable=false, length=50)
    private String acaCardNo;

    @Column(name="ProRecId", nullable=false, length=20)
    private String proRecId;

    @Column(name="DrgUserText")       private String drgUserText;
    @Column(name="OprFamilyText")     private String oprFamilyText;
    @Column(name="OprFamilyCareText") private String oprFamilyCareText;
    @Column(name="OprSupportText")    private String oprSupportText;
    @Column(name="OprContactText")    private String oprContactText;
    @Column(name="OprReferText")      private String oprReferText;
    @Column(name="Addr")              private String addr;
    @Column(name="OprAddr")           private String oprAddr;

    @Column(name="CreatedByBranchID") private String createdByBranchId;
    @Column(name="CreatedByUserID")   private Integer createdByUserId;
    @Column(name="CreatedOnDate")     private Timestamp createdOnDate;
    @Column(name="ModifiedByUserID")  private Integer modifiedByUserId;
    @Column(name="ModifiedOnDate")    private Timestamp modifiedOnDate;
    @Column(name="IsDeleted")         private Boolean isDeleted;
}
