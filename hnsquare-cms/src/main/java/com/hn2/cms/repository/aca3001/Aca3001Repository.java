package com.hn2.cms.repository.aca3001;

import com.hn2.cms.dto.aca3001.Aca3001QueryDto;
import com.hn2.cms.payload.aca3001.Aca3001SavePayload;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;


public interface Aca3001Repository {
    //Query API
    Integer findProAdoptIdByProRecId(String proRecId);

    LocalDate loadTimeLockDate();

    boolean isEditable(String proRecId, LocalDate timeLockDate);

    Aca3001QueryDto.Meta computeMeta(String proRecId, Integer proAdoptId);

    Aca3001QueryDto.Header computeHeader(String proRecId);

    Aca3001QueryDto.Profile computeProfile(String proRecId);

    Aca3001QueryDto.DirectAdoptCriteria computeDirectAdoptCriteria(Integer proAdoptId);

    Aca3001QueryDto.EvalAdoptCriteria computeEvalAdoptCriteria(Integer proAdoptId);

    Aca3001QueryDto.Summary computeSummary(String proRecId, Integer proAdoptId);

    //Save API
    Integer insertProAdopt(@NotBlank String proRecId, Aca3001SavePayload.@NotNull @Valid Scores scores, boolean caseReject, String reasonReject, boolean caseAccept, String reasonAccept, boolean caseEnd, String reasonEnd, Integer integer);

    void updateProAdopt(Integer proAdoptId, Aca3001SavePayload.@NotNull @Valid Scores scores, boolean caseReject, String reasonReject, boolean caseAccept, String reasonAccept, boolean caseEnd, String reasonEnd, Integer integer);

    void replaceDirectAdoptCriteria(Integer proAdoptId, @NotNull List<Integer> directSelectedEntryIds);

    void replaceEvalAdoptCriteria(Integer proAdoptId, @NotNull List<Integer> evalSelectedEntryIds);

    void upsertDirectAdoptCriteria(int proAdoptId, List<Integer> selectedEntryIds, boolean refreshSnapshot, boolean isNew);

    void upsertEvalAdoptCriteria(int proAdoptId, List<Integer> selectedEntryIds, boolean refreshSnapshot, boolean isNew);

    //Delete API
    void deleteProAdoptCascade(Integer proAdoptId);
}
