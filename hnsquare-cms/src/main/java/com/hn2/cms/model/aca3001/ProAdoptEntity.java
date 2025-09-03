package com.hn2.cms.model.aca3001;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

// package: com.hn2.cms.model.aca3001
@Entity
@Table(name = "ProAdopt",
        uniqueConstraints = @UniqueConstraint(name = "UQ_ProAdopt_ProRecID", columnNames = "ProRecID"))
@Getter
@Setter
public class ProAdoptEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "ProRecID", nullable = false, length = 12)
    private String proRecId;

    // 九項分數
    @Column(name = "ScoreEconomy",    nullable = false) private short scoreEconomy;
    @Column(name = "ScoreEmployment", nullable = false) private short scoreEmployment;
    @Column(name = "ScoreFamily",     nullable = false) private short scoreFamily;
    @Column(name = "ScoreSocial",     nullable = false) private short scoreSocial;
    @Column(name = "ScorePhysical",   nullable = false) private short scorePhysical;
    @Column(name = "ScorePsych",      nullable = false) private short scorePsych;
    @Column(name = "ScoreParenting",  nullable = false) private short scoreParenting;
    @Column(name = "ScoreLegal",      nullable = false) private short scoreLegal;
    @Column(name = "ScoreResidence",  nullable = false) private short scoreResidence;

    // DB 計算欄位
    @Column(name = "ScoreTotal", insertable = false, updatable = false)
    private Short scoreTotal;

    @Column(name = "[Comment]") private String comment;

    @Column(name = "CaseReject", nullable = false) private boolean caseReject;
    @Column(name = "ReasonReject")               private String reasonReject;
    @Column(name = "CaseAccept", nullable = false) private boolean caseAccept;
    @Column(name = "ReasonAccept")                 private String reasonAccept;
    @Column(name = "CaseEnd",    nullable = false) private boolean caseEnd;
    @Column(name = "ReasonEnd")                    private String reasonEnd;

    @Column(name = "CreatedByUserID")  private Integer createdByUserId;
    @Column(name = "ModifiedByUserID") private Integer modifiedByUserId;
}