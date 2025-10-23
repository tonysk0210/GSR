package com.hn2.cms.repository.aca4001;

import com.hn2.cms.dto.aca4001.Aca4001AuditQueryDto;
import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto;
import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto.PersonBirth;

import java.time.LocalDateTime;
import java.util.List;

public interface Aca4001Repository {

    /*eraseQuery API*/
    PersonBirth findPersonBirth(String acaCardNo);

    List<String> findProRecIdsBefore18(String acaCardNo, LocalDateTime eighteenthStart, LocalDateTime startTs, LocalDateTime endInclusive);

    List<String> findCrmRecIdsBefore18(String acaCardNo, LocalDateTime eighteenthStart, LocalDateTime startTs, LocalDateTime endInclusive);

    List<String> findAllAcaDrugUseIdsByAcaCardNo(String acaCardNo);

    List<Aca4001EraseQueryDto.CrmRec> findCrmRecsByIds(List<String> ids);

    List<Aca4001EraseQueryDto.ProRec> findProRecsByIds(List<String> ids);

    List<Aca4001EraseQueryDto.ACADrugUse> findAcaDrugUsesByIds(List<String> drgIds);

    Boolean findLatestProRecClosed(String acaCardNo);

    /*eraseQuery API & restoreQuery API*/
    Boolean findPersonErased(String acaCardNo);

    /*erase API*/
    List<String> findAllCrmRecIdsByAcaCardNo(String acaCardNo);

    List<String> findAllProRecIdsByAcaCardNo(String acaCardNo);

    /*auditQuery API*/
    List<Aca4001AuditQueryDto.Row> findAuditRows();


}
