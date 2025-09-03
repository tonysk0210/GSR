package com.hn2.cms.model.aca3001;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "DirectAdoptCriteria")
public class DirectAdoptCriteriaEntity {

    @EmbeddedId
    private Id id;

    @MapsId("proAdoptId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ProAdoptID", nullable = false)
    private ProAdoptEntity proAdopt;

    @Column(name = "EntryText", nullable = false, length = 200)
    private String entryText;

    @Column(name = "IsSelected", nullable = false)
    private boolean selected;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class Id implements Serializable {
        @Column(name = "ProAdoptID", nullable = false)
        private Integer proAdoptId;

        @Column(name = "ListsEntryID", nullable = false)
        private Integer listsEntryId;
    }
}