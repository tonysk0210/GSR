package com.hn2.cms.repository.report02;

import com.hn2.cms.dto.report02.Report02Dto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class Report02RepositoryImpl implements Report02Repository {

    private final JdbcTemplate jdbc;

    public Report02RepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 取回「分會 × 機關」的聚合計數（扁平結構 FlatRow）。
     * - 分會名稱 / 排序：Lists(ParentID=26) 依 SIGN_PROT_NO 對應 Value -> Text/SortOrder
     * - 機關名稱：Org_Lists 依 ORG_CODE 對應 ORG_NAME
     * - RS_DT 為 date 型別：直接用 >= from AND <= to（含頭含尾）
     */
    public List<Report02Dto.FlatRow> findAggregates(LocalDate from, LocalDate to) {

        // 核心查詢：
        // 1) agg：在 SUP_AfterCare 依「分會 × 機關」分組，計算三個狀態的數量
        // 2) lists_clean：從 Lists(ParentID=26) 取每個 Value 最新的一筆（避免重覆/歷史值）
        // 3) org_clean：從 Org_Lists 取每個 ORG_CODE 一筆（避免重覆）
        // 4) 將分會名稱/排序與機關名稱 JOIN 回 agg，並依分會排序 + 機關代碼排序輸出
        String sql =
                "WITH agg AS ( \n" +
                        "  SELECT \n" +
                        "    a.SIGN_PROT_NO AS branchCode, \n" + // 分會代碼
                        "    a.ORG_CODE     AS orgCode, \n" + // 機關代碼
                        "    SUM(CASE WHEN a.SIGN_STATE = 0 THEN 1 ELSE 0 END) AS pendingCount, \n" +
                        "    SUM(CASE WHEN a.SIGN_STATE = 1 THEN 1 ELSE 0 END) AS signedCount, \n" +
                        "    SUM(CASE WHEN a.SIGN_STATE = 3 THEN 1 ELSE 0 END) AS caseCount \n" +
                        "  FROM dbo.SUP_AfterCare a WITH (NOLOCK) \n" + // 視規範決定是否保留 NOLOCK
                        "  WHERE a.RS_DT >= ? AND a.RS_DT <= ? \n" +    // date 型別直接比較（含頭含尾）
                        "  GROUP BY a.SIGN_PROT_NO, a.ORG_CODE \n" +
                        "), lists_clean AS ( \n" +
                        "  SELECT \n" +
                        "    Value, Text, SortOrder, \n" +
                        "    ROW_NUMBER() OVER (PARTITION BY Value \n" +
                        "      ORDER BY ISNULL(ModifiedOnDate, CreatedOnDate) DESC, EntryID DESC) AS rn \n" + // 每個 Value 取最新一筆
                        "  FROM dbo.Lists \n" +
                        "  WHERE ParentID = 26 \n" + // 分會清單
                        "), org_clean AS ( \n" +
                        "  SELECT \n" +
                        "    ORG_CODE, ORG_NAME, \n" +
                        "    ROW_NUMBER() OVER (PARTITION BY UPPER(LTRIM(RTRIM(ORG_CODE))) \n" +
                        "      ORDER BY ORG_CODE) AS rn \n" + // ORG_CODE 相同只取一筆
                        "  FROM dbo.Org_Lists \n" +
                        ") \n" +
                        "SELECT \n" +
                        "  agg.branchCode, \n" +
                        "  ls.Text      AS branchName, \n" + // 分會名稱
                        "  ls.SortOrder AS sortOrder, \n" + // 分會排序
                        "  agg.orgCode, \n" +
                        "  oc.ORG_NAME  AS orgName, \n" +   // 機關名稱（Org_Lists）
                        "  agg.pendingCount, \n" +
                        "  agg.signedCount, \n" +
                        "  agg.caseCount \n" +
                        "FROM agg \n" +
                        "LEFT JOIN lists_clean ls \n" +
                        "  ON ls.rn = 1 \n" +
                        " AND UPPER(LTRIM(RTRIM(ls.Value))) = UPPER(LTRIM(RTRIM(agg.branchCode))) \n" + // 分會代碼比對（去空白/大小寫忽略）
                        "LEFT JOIN org_clean oc \n" +
                        "  ON oc.rn = 1 \n" +
                        " AND UPPER(LTRIM(RTRIM(oc.ORG_CODE))) = UPPER(LTRIM(RTRIM(agg.orgCode))) \n" + // 機關代碼比對（去空白/大小寫忽略）
                        "ORDER BY \n" +
                        "  ISNULL(ls.SortOrder, 2147483647) ASC, \n" + // 先依分會排序
                        "  TRY_CONVERT(int, agg.orgCode) ASC, agg.orgCode ASC;"; // 再依機關代碼（可數值化則先）


        // 綁定參數與 RowMapper：將每一列結果映射成 FlatRow
        return jdbc.query(sql, ps -> {
                    // JDBC 4.2 起可直接綁 LocalDate；舊 driver 可改成 java.sql.Date.valueOf(from/to)
                    ps.setObject(1, from);
                    ps.setObject(2, to);
                }, (rs, i) -> Report02Dto.FlatRow.builder()
                        .branchCode(rs.getString("branchCode"))
                        .branchName(rs.getString("branchName"))
                        .sortOrder(rs.getObject("sortOrder") == null ? null : rs.getInt("sortOrder"))
                        .orgCode(rs.getString("orgCode"))
                        .orgName(rs.getString("orgName"))
                        .pendingCount(rs.getInt("pendingCount"))
                        .signedCount(rs.getInt("signedCount"))
                        .caseCount(rs.getInt("caseCount"))
                        .build()
        );
    }
}
