package com.hn2.cms.model.aca3001;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

// 所有子表（DirectAdoptCriteria/EvalAdoptCriteria）會從這裡拿「文字快照」。
@Entity
@Table(name = "Lists")
@Getter
@Setter
public class ListsEntity {
    @Id
    @Column(name = "EntryID")
    private Integer entryId;

    @Column(name = "ListName", nullable = false)
    private String listName;

    @Column(name = "Value")
    private String value;

    @Column(name = "Text")
    private String text;

    @Column(name = "IsDisabled", nullable = false)
    private boolean isDisabled;

    @Column(name = "SortOrder")
    private Integer sortOrder;
}