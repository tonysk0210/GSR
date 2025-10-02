package com.hn2.cms.service.aca4001.erase.spi;

import com.hn2.cms.service.aca4001.erase.support.RowUtils;

import java.util.List;
import java.util.Map;

public abstract class AbstractSql2oTarget implements EraseTarget {
    protected final org.sql2o.Sql2o sql2o;

    protected AbstractSql2oTarget(org.sql2o.Sql2o sql2o) {
        this.sql2o = sql2o;
    }

    protected String buildSelectCols() {
        var white = whitelistColumns();
        return (white == null || white.isEmpty())
                ? "ID AS __PK__"
                : "ID AS __PK__," + String.join(",", white);
    }

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

    protected int updateWithDynamicSet(String id,
                                       Map<String, Object> cleaned,
                                       String extraSetSuffix, // e.g. ", [isERASE]=0, [ModifiedOnDate]=SYSUTCDATETIME(), [ModifiedByUserID]=:uid"
                                       java.util.function.Function<org.sql2o.Query, org.sql2o.Query> binder) {
        StringBuilder set = new StringBuilder();
        int i = 0;
        for (var e : cleaned.entrySet()) {
            if (i++ > 0) set.append(", ");
            set.append("[").append(e.getKey()).append("] = :").append(RowUtils.paramName(e.getKey()));
        }
        if (extraSetSuffix != null && !extraSetSuffix.isEmpty()) set.append(extraSetSuffix);

        String sql = "UPDATE " + schema() + "." + table() + " SET " + set + " WHERE ID=:id AND ISNULL(isERASE,0)=1";
        try (var con = sql2o.open()) {
            var q = con.createQuery(sql).addParameter("id", id);
            for (var e : cleaned.entrySet()) {
                q.addParameter(RowUtils.paramName(e.getKey()), e.getValue());
            }
            q = binder.apply(q);
            return q.executeUpdate().getResult();
        }
    }
}
