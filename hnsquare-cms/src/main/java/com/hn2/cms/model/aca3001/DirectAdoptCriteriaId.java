package com.hn2.cms.model.aca3001;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DirectAdoptCriteriaId implements Serializable {
    @Column(name = "ProAdoptID")
    private Integer proAdoptId;

    @Column(name = "ListsEntryID")
    private Integer listsEntryId;
}