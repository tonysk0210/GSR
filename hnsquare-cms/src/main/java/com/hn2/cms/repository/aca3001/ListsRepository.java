package com.hn2.cms.repository.aca3001;

import com.hn2.cms.model.aca3001.ListsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListsRepository extends JpaRepository<ListsEntity, Integer> {
    List<ListsEntity> findByListNameAndIsDisabledFalseOrderBySortOrderAscEntryIdAsc(String listName);
}