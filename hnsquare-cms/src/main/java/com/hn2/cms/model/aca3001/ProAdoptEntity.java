package com.hn2.cms.model.aca3001;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

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

    @Column(name = "ScoreEconomy", nullable = false)
    private short scoreEconomy;
    @Column(name = "ScoreEmployment", nullable = false)
    private short scoreEmployment;
    @Column(name = "ScoreFamily", nullable = false)
    private short scoreFamily;
    @Column(name = "ScoreSocial", nullable = false)
    private short scoreSocial;
    @Column(name = "ScorePhysical", nullable = false)
    private short scorePhysical;
    @Column(name = "ScorePsych", nullable = false)
    private short scorePsych;
    @Column(name = "ScoreParenting", nullable = false)
    private short scoreParenting;
    @Column(name = "ScoreLegal", nullable = false)
    private short scoreLegal;
    @Column(name = "ScoreResidence", nullable = false)
    private short scoreResidence;

    @Column(name = "ScoreTotal", insertable = false, updatable = false)
    private Short scoreTotal;

    @Column(name = "[Comment]")
    private String comment;

    @Column(name = "CaseReject", nullable = false)
    private boolean caseReject;
    @Column(name = "ReasonReject")
    private String reasonReject;
    @Column(name = "CaseAccept", nullable = false)
    private boolean caseAccept;
    @Column(name = "ReasonAccept")
    private String reasonAccept;
    @Column(name = "CaseEnd", nullable = false)
    private boolean caseEnd;
    @Column(name = "ReasonEnd")
    private String reasonEnd;

    // ==== 審計欄位 ====
    @Column(name = "CreatedByUserID", updatable = false)
    private Integer createdByUserId;
    @Column(name = "ModifiedByUserID")
    private Integer modifiedByUserId;
    @Column(name = "CreatedOnDate", updatable = false)
    private LocalDateTime createdOnDate;
    @Column(name = "ModifiedOnDate")
    private LocalDateTime modifiedOnDate;

    // === 重要：與子表的關聯（級聯 + 需 orphanRemoval） ===
    @OneToMany(mappedBy = "proAdopt",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Set<DirectAdoptCriteriaEntity> directCriteria = new LinkedHashSet<>();

    @OneToMany(mappedBy = "proAdopt",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Set<EvalAdoptCriteriaEntity> evalCriteria = new LinkedHashSet<>();

    // === 輔助方法：維護雙向關聯一致性 ===
    public void addDirect(DirectAdoptCriteriaEntity e) {
        directCriteria.add(e);
        e.setProAdopt(this);
    }

    public void removeDirect(DirectAdoptCriteriaEntity e) {
        directCriteria.remove(e);
        e.setProAdopt(null);
    }

    public void addEval(EvalAdoptCriteriaEntity e) {
        evalCriteria.add(e);
        e.setProAdopt(this);
    }

    public void removeEval(EvalAdoptCriteriaEntity e) {
        evalCriteria.remove(e);
        e.setProAdopt(null);
    }

    // ==== 自動審計（不依賴 Spring Security）====
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdOnDate = (this.createdOnDate == null) ? now : this.createdOnDate;
        this.modifiedOnDate = now;
        if (this.modifiedByUserId == null) {
            this.modifiedByUserId = this.createdByUserId; // 新增時同步
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.modifiedOnDate = LocalDateTime.now();
    }
}