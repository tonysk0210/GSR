package com.hn2.cms.service.aca4001.erase.spi;

import com.hn2.cms.service.aca4001.erase.support.RowUtils;

import java.util.List;
import java.util.Map;

public abstract class AbstractSql2oTarget implements EraseTarget {
    protected final org.sql2o.Sql2o sql2o; // 由 Spring 注入的連線工廠

    protected AbstractSql2oTarget(org.sql2o.Sql2o sql2o) {
        this.sql2o = sql2o;
    }

    // 依白名單組 SELECT 欄位，強制 ID AS __PK__（呼應 idColumn() 預設）
    protected String buildSelectCols() {
        var white = whitelistColumns();
        return (white == null || white.isEmpty())
                ? "ID AS __PK__"
                : "ID AS __PK__," + String.join(",", white);
    }

    // 以 IN(:ids) 分片查詢，避免 IN 過長；每片開一個連線抓回來再合併
    protected List<Map<String, Object>> fetchRowsByIdsChunked(List<String> ids, int chunkSize) {
        if (ids == null || ids.isEmpty()) return java.util.Collections.emptyList();
        final String sql = "SELECT " + buildSelectCols() + " FROM " + schema() + "." + table() + " WHERE ID IN (:ids)";
        var all = new java.util.ArrayList<Map<String, Object>>();
        for (int i = 0; i < ids.size(); i += chunkSize) {
            var sub = ids.subList(i, Math.min(i + chunkSize, ids.size()));
            try (var con = sql2o.open()) {
                var t = con.createQuery(sql).addParameter("ids", sub).executeAndFetchTable();
                all.addAll(t.rows().stream().map(org.sql2o.data.Row::asMap)
                        .collect(java.util.stream.Collectors.toList()));
            }
        }
        return all;
    }

    // 動態 UPDATE SET 子句的共用方法（還原時用）
    protected int updateWithDynamicSet(
            String id,
            Map<String, Object> cleaned, // 欲回填的欄位名->值（已正規化）
            String extraSetSuffix, // 例如 ", [isERASE]=0, [ModifiedOnDate]=${NOW}, [ModifiedByUserID]=:uid"
            java.util.function.Function<org.sql2o.Query, org.sql2o.Query> binder // 補充綁參
    ) {
        // ---- 1) 統一決定時間來源（true=UTC / false=本地）----
        final boolean useUtcNow = false; // 你可切換：true=UTC、false=本地
        final String nowExpr = useUtcNow ? "SYSUTCDATETIME()" : "SYSDATETIME()";

        // 將 suffix 內的 ${NOW} 以上面 nowExpr 替換
        String suffix = (extraSetSuffix == null) ? "" : extraSetSuffix.replace("${NOW}", nowExpr);

        // 動態拼 SET 子句：對 cleaned 的每個鍵生成 "[Col]=:param"；param 名用 RowUtils.paramName 防衝突
        StringBuilder set = new StringBuilder();
        int i = 0;
        for (var e : cleaned.entrySet()) {
            if (i++ > 0) set.append(", ");
            set.append("[")
                    .append(e.getKey())
                    .append("] = :")
                    .append(RowUtils.paramName(e.getKey()));
        }

        // 合併 suffix；若 cleaned 為空，會把 suffix 前導逗號/空白去掉後直接用
        if (!suffix.isEmpty()) {
            if (set.length() == 0) {
                // 沒有任何欄位要 SET，只能靠 suffix；把開頭形如 ", " 或 "," 去掉
                String trimmedSuffix = suffix;
                while (trimmedSuffix.startsWith(",") || trimmedSuffix.startsWith(" ")) {
                    trimmedSuffix = trimmedSuffix.substring(1);
                }
                set.append(trimmedSuffix);
            } else {
                set.append(suffix);
            }
        }

        // 沒有任何可更新欄位就直接略過
        if (set.length() == 0) {
            return 0;
        }

        String sql = "UPDATE " + schema() + "." + table() + " SET " + set + " WHERE ID=:id AND ISNULL(isERASE,0)=1";
        try (var con = sql2o.open()) {
            var q = con.createQuery(sql).addParameter("id", id);
            for (var e : cleaned.entrySet()) {
                q.addParameter(RowUtils.paramName(e.getKey()), e.getValue());
            }
            q = binder.apply(q); // 讓呼叫端補上 :uid 等參數
            return q.executeUpdate().getResult();
        }
    }
}
