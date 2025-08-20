package com.hn2.cms.repository.aca3001;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProAdoptRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProAdoptRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ProfileRow findProfileByProRecId(String proRecId) {
        // 方案一：ProRec 先取到 ACACardNo，再去 ACABrd
        // （若你 DB 結構是這種，打開這段並關掉方案二）
        /*
        String sql = """
            SELECT b.ACAName, b.ACAIDNo, b.ACACardNo
            FROM ProRec r
            JOIN ACABrd b ON b.ACACardNo = r.ACACardNo
            WHERE r.ProRecID = ?
        """;
        */

        // 方案二：ProRec 直接關聯 ACABrd（舉例用 AcaId）
        String sql = """
            SELECT TOP 1
                   b.ACAName,
                   b.ACAIDNo,
                   b.ACACardNo
            FROM dbo.ProRec r
            JOIN dbo.ACABrd b ON b.ACACardNo = r.ACACardNo
            WHERE r.ProCaseID = ?
            ORDER BY b.ModifiedOnDate DESC
        """;

        List<ProfileRow> list = jdbcTemplate.query(sql, (rs, i) -> {
            ProfileRow row = new ProfileRow();
            row.setAcaName(rs.getString("ACAName"));
            row.setAcaIdNo(rs.getString("ACAIDNo"));
            row.setAcaCardNo(rs.getString("ACACardNo"));
            return row;
        }, proRecId);

        return list.isEmpty() ? null : list.get(0);
    }

    // 簡單 Row DTO（內部用）
    public static class ProfileRow {
        private String acaName;
        private String acaIdNo;
        private String acaCardNo;
        // getters/setters
        public String getAcaName() { return acaName; }
        public void setAcaName(String acaName) { this.acaName = acaName; }
        public String getAcaIdNo() { return acaIdNo; }
        public void setAcaIdNo(String acaIdNo) { this.acaIdNo = acaIdNo; }
        public String getAcaCardNo() { return acaCardNo; }
        public void setAcaCardNo(String acaCardNo) { this.acaCardNo = acaCardNo; }
    }
}
