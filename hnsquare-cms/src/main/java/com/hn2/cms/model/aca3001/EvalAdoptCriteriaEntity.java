package com.hn2.cms.model.aca3001;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "EvalAdoptCriteria")
@Getter
@Setter
public class EvalAdoptCriteriaEntity {

    @EmbeddedId
    private EvalAdoptCriteriaId id;

    @MapsId("proAdoptId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProAdoptID", nullable = false)
    private ProAdoptEntity proAdopt;

    @Column(name = "EntryText")
    private String entryText;

    @Column(name = "IsSelected", nullable = false)
    private boolean isSelected;
}