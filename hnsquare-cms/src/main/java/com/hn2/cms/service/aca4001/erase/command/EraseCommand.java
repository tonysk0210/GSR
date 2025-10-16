package com.hn2.cms.service.aca4001.erase.command;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Getter
@ToString
public final class EraseCommand {
    private final String acaCardNo;
    private final Map<String, List<String>> tableToIds;
    private final Integer operatorUserId;
    private final String operatorIp;
    private final Integer docNum;       // 可為 null
    private final String eraseReason;   // 可為 null

    @Builder
    public EraseCommand(
            String acaCardNo,
            @Singular("tableIds") Map<String, List<String>> tableToIds,
            Integer operatorUserId,
            String operatorIp,
            Integer docNum,
            String eraseReason
    ) {
        this.acaCardNo = acaCardNo;
        this.tableToIds = sanitizeTableToIds(tableToIds);
        this.operatorUserId = operatorUserId;
        this.operatorIp = operatorIp;
        this.docNum = docNum;
        this.eraseReason = eraseReason;
    }

    public List<String> idsOf(String table) {
        List<String> v = tableToIds.get(table);
        return v == null ? Collections.emptyList() : v;
    }

    public boolean isEmpty() {
        return tableToIds.isEmpty();
    }

    /** 將呼叫端提供的表格/主鍵清單整理成不可變且無空值的結構，避免後續重複檢核。 */
    private static Map<String, List<String>> sanitizeTableToIds(Map<String, List<String>> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> cleaned = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            String table = entry.getKey();
            if (table == null || table.isBlank()) {
                continue;
            }
            List<String> ids = cleanIds(entry.getValue());
            if (ids.isEmpty()) {
                continue;
            }
            cleaned.merge(
                table,
                ids,
                EraseCommand::mergeIds
            );
        }
        return cleaned.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(cleaned);
    }

    /** 去除空白、維持順序並避免重複的主鍵清單。 */
    private static List<String> cleanIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        var ordered = new LinkedHashSet<String>();
        for (String raw : ids) {
            if (raw == null) continue;
            String trimmed = raw.trim();
            if (!trimmed.isEmpty()) {
                ordered.add(trimmed);
            }
        }
        return ordered.isEmpty() ? Collections.emptyList() : List.copyOf(ordered);
    }

    /** 以主鍵出現順序為準合併兩個清單，確保後續遍歷可預期。 */
    private static List<String> mergeIds(List<String> left, List<String> right) {
        var ordered = new LinkedHashSet<String>(left);
        ordered.addAll(right);
        return List.copyOf(ordered);
    }
}
