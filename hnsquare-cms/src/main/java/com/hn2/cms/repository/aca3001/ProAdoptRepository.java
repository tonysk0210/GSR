package com.hn2.cms.repository.aca3001;

import com.hn2.cms.model.aca3001.ProAdoptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// package: com.hn2.cms.repository.aca3001
public interface ProAdoptRepository extends JpaRepository<ProAdoptEntity, Integer> {
    Optional<ProAdoptEntity> findByProRecId(String proRecId);

    @Query("select p.id from ProAdoptEntity p where p.proRecId = :proRecId")
    Optional<Integer> findIdByProRecId(@Param("proRecId") String proRecId);
}