package com.hn2.cms.repository.report02;

import com.hn2.cms.dto.report02.Report02Dto;

import java.time.LocalDate;
import java.util.List;

public interface Report02Repository {
    List<Report02Dto.FlatRow> findAggregates(LocalDate from, LocalDate to);
}
