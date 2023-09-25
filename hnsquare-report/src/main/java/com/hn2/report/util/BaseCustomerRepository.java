package com.hn2.report.util;


import org.apache.commons.lang3.ArrayUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.security.InvalidParameterException;
import java.time.LocalDate;
import java.util.*;

public abstract class BaseCustomerRepository {


    @Autowired
    ModelMapper mapper;

    protected StringBuilder sql;
    protected Map<String, Object> params;
    protected boolean inspectChildOrgan = false;

    protected void queryInit() {
        sql = new StringBuilder();
        params = new HashMap<>();
        inspectChildOrgan = false;
    }

    protected void eq(String field, String value) {
        buildCondition(field, value, "=");
    }

    protected void eq(String field, LocalDate value) {
        if (hasValue(value)) {
            buildCondition(field, value.toString(), "=");
        }
    }

    protected void eq(String prefix, String field, String value) {
        eq(prefix + "." + field, value);
    }

    protected void gte(String field, String value) {
        buildCondition(field, value, ">=");
    }

    protected void gt(String field, String value) {
        buildCondition(field, value, ">");
    }

    protected void lte(String field, String value) {
        buildCondition(field, value, "<=");
    }

    protected void lt(String field, String value) {
        buildCondition(field, value, "<");
    }

    protected void in(String field, List<String> valueList) {
        if (CollectionUtils.isEmpty(valueList)) {
            return;
        }
        buildInCondition(field, valueList.toArray(String[]::new));
    }

    protected void or(String field, String[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return;
        }
        buildOrCondition(field, values);
    }

    protected void in(String field, String[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return;
        }
        buildInCondition(field, values);
    }

    protected void notIn(String field, String[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return;
        }

        buildNotInCondition(field, values);
    }

    protected void between(String field, String start, String end) {
        if (StringUtils.hasLength(start)) {
            gte(field, start);
        }

        if (StringUtils.hasLength(end)) {
            lte(field, end);
        }
    }

    protected void between(String field, LocalDate start, LocalDate end) {
        between(field, String.valueOf(start), String.valueOf(end));
    }

    protected void buildCondition(String field, String value, String operator) {
        String placeHolder = placeHolder(field, operator);
        String paramKey = placeHolder.replace(":", "");
        operator = explainOperator(operator);

        if (hasValue(value)) {
            sql.append(" and ").append(field).append(operator).append(placeHolder);
            params.put(paramKey, value);
        }
    }

    protected void buildCondition2(String field1,String field2, String value, String operator) {
        String placeHolder1 = placeHolder(field1, operator);
        String placeHolder2 = placeHolder(field2, operator);
        String paramKey1 = placeHolder1.replace(":", "");
        String paramKey2 = placeHolder2.replace(":", "");
        operator = explainOperator(operator);

        if (hasValue(value)) {
            sql.append(" and (").append(field1).append(operator).append(placeHolder1).append(" or ").append(field2).append(operator).append(placeHolder2).append(")");
            params.put(paramKey1, value);
            params.put(paramKey2, value);
        }
    }

    protected void buildCondition3(String field, String[] value, String operator) {
        if (value == null || value.length == 0) {
            return;  // Return early if no values are provided
        }
        operator = explainOperator(operator);

        sql.append(" AND (");
        for (int i = 0; i < value.length; i++) {
            if (i > 0) {
                sql.append(" OR ");  // Add "OR" for all conditions after the first one
            }
            String currentPlaceholder = ":value"+(i)+"" ;
            sql.append(field).append(" ").append(operator).append(" ").append(currentPlaceholder);

            // Adding each value from the array into the params map
            params.put("value" + (i) , "%"+value[i]+"%");
        }
        sql.append(")");
    }


    private String explainOperator(String operator) {
        switch (operator) {
            case ">=":
            case "gte":
                return " >= ";

            case "<=":
            case "lte":
                return " <= ";

            case "like":
            case "leftLike":
            case "rightLike":
                return " like ";

            case "=":
            case "eq":
                return " = ";

            default:
                return operator;
        }
    }

    protected void buildInCondition(String field, String[] values) {

        String placeHolder = placeHolder(field, "in");
        String paramKey = placeHolder.replace(":", "");

        if (values.length > 0) {
            sql.append(" and ").append(field).append(" in (").append(placeHolder).append(") ");
            params.put(paramKey, values);
        }
    }

    protected void buildNotInCondition(String field, String[] values) {
        String placeHolder = placeHolder(field, "in");
        String paramKey = placeHolder.replace(":", "");

        if (values.length > 0) {
            sql.append(" and ").append(field).append(" not in (").append(placeHolder).append(") ");
            params.put(paramKey, values);
        }
    }

    protected void buildOrCondition(String field, String[] values) {
        String placeHolder = placeHolder(field, "or");
        String paramKey = placeHolder.replace(":", "");

        if (values.length > 0) {
            sql.append(" and (").append(field).append("=").append("'"+values[0]+"'");
            for(int i=1;i<values.length;i++){
                sql.append(" or ").append(field).append("=").append("'"+values[i]+"'");
            }
            sql.append(")");
        }
    }

    protected void startWith(String field, String value) {
        if (hasValue(value)) {
            value = concat(value, "%");
        }
        buildCondition(field, value, "rightLike");
    }

    protected void endWith(String field, String value) {
        if (hasValue(value)) {
            value = concat("%", value);
        }
        buildCondition(field, value, "leftLike");
    }

    protected void like(String field, String value) {
        if (hasValue(value)) {
            value = concat("%", value, "%");
        }
        buildCondition(field, value, "like");
    }

    protected void notLike(String field, String value) {
        if (hasValue(value)) {
            value = concat("%", value, "%");
        }
        buildCondition(field, value, "not like");
    }


    protected void likeArray(String field, String[] value) {
        buildCondition3(field, value, "like");
    }

    protected void notLikeArray(String field, String[] value) {
        buildCondition3(field, value, "not like");
    }

    protected void like2(String field1,String field2,String value) {
        if (hasValue(value)) {
            value = concat("%", value, "%");
        }

        buildCondition2(field1,field2, value, "like");
    }


    protected void groupBy(String... fields) {
        String s = Arrays.stream(fields).reduce((first, second) -> first + ", " + second)
                .orElseThrow(() -> new InvalidParameterException("fields參數輸入錯誤： " + Arrays.toString(fields)));

        groupBy(s);
    }

    protected void groupBy(String fieldString) {
        sql.append(" group by ").append(fieldString);
    }

    protected void having(String condition) {
        sql.append(" having ").append(condition);
    }

    protected void orderBy(String field) {
        sql.append(" order by ").append(field);
    }

    /**
     * order by input
     *
     * @param fields input strings ex: {"name asc", "age desc", ... }
     */
    protected void orderBy(String... fields) {
        sql.append(" order by ");
        int count = 0;
        for (String field : fields) {
            if (!hasValue(field)) {
                continue;
            }

            if (count++ > 0) {
                sql.append(", ");
            }

            sql.append(field);
        }
    }

    /**
     * order by input
     *
     * @param fields input strings ex: {"name asc", "age desc", ... }
     */
    protected void orderBy(Collection<String> fields) {
        sql.append(" order by ");
        int count = 0;
        for (String field : fields) {
            if (!hasValue(field)) {
                continue;
            }

            if (count++ > 0) {
                sql.append(", ");
            }

            sql.append(field);
        }
    }

    protected void orderByDesc(String field) {
        sql.append(" order by ").append(field).append(" desc ");
    }

    private void relate(String relate, String tableName, String resource, String self, List<String> extraCondition) {
        String relation;
        switch (relate) {
            case "LF":
            case "LEFT":
                relation = " left join ";
                break;
            case "RT":
            case "RIGHT":
                relation = " right join ";
            case "CROSS":
            case "X":
                relation = " cross join ";
            default:
                relation = " join ";
        }

        sql.append(relation).append(tableName)
                .append(" on ")
                .append(resource).append(" = ").append(self);

        if (CollectionUtils.isEmpty(extraCondition)) {
            return;
        }

        for (String cond : extraCondition) {
            sql.append(cond);
        }
    }

    protected void join(String tableName, String resource, String self) {
        relate("", tableName, resource, self, null);
    }

    protected void join(String tableName, String resource, String self, List<String> extraCondition) {
        relate("", tableName, resource, self, extraCondition);
    }

    protected void leftJoin(String tableName, String resource, String self) {
        relate("LF", tableName, resource, self, null);
    }

    protected void leftJoin(String tableName, String resource, String self, List<String> extraCondition) {
        relate("LF", tableName, resource, self, extraCondition);
    }

    protected void rightJoin(String tableName, String resource, String self) {
        relate("RT", tableName, resource, self, null);
    }

    protected void rightJoin(String tableName, String resource, String self, List<String> extraCondition) {
        relate("RT", tableName, resource, self, extraCondition);
    }




    private String placeHolder(String field, String operator) {
        String param = Arrays.stream(field.split("\\."))
                .reduce((prefix, column) -> column)
                .orElseThrow(() -> new InvalidParameterException("field參數輸入錯誤： " + field));

        switch (operator) {
            case ">=":
            case "gte":
                return ":" + param + "S";

            case "<=":
            case "lte":
                return ":" + param + "E";

            default:
                return ":" + param;
        }
    }

    protected String concat(String... strings) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : strings) {
            stringBuilder.append(s);
        }
        return stringBuilder.toString();
    }

    protected String getFieldName(String prefix, String field) {
        return StringUtils.hasLength(prefix) ? concat(prefix, ".", field) : field;
    }

    protected Boolean hasValue(String text) {
        return StringUtils.hasLength(text) && !"null".equals(text);
    }

    protected Boolean hasValue(Collection<String> values) {
        return !CollectionUtils.isEmpty(values);
    }

    protected Boolean hasValue(LocalDate date) {
        return Objects.nonNull(date);
    }

    protected String joinFields(String[] fields) {
        return String.join(",", fields);
    }

    protected String joinFields(Collection<String> fields) {
        return String.join(",", fields);
    }


    protected <T> String[] getDeclaredFields(String prefix, Class<T> clazz, Map<String, String> extraPrefixMapping) {
        Field[] fields = clazz.getDeclaredFields();
        String[] select = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            field.setAccessible(true);
            if (hasValue(prefix) || !CollectionUtils.isEmpty(extraPrefixMapping)) {
                String customerPrefix = extraPrefixMapping.get(field.getName());
                if (hasValue(customerPrefix)) {
                    select[i] = concat(customerPrefix, ".", field.getName());
                } else {
                    select[i] = concat(prefix, ".", field.getName());
                }
            } else {
                select[i] = field.getName();
            }
        }
        return select;
    }

    protected <T> String[] getDeclaredFields(String prefix, Class<T> clazz) {
        return getDeclaredFields(prefix, clazz, Collections.EMPTY_MAP);
    }

    protected <T> String[] getDeclaredFields(Class<T> clazz) {
        return getDeclaredFields("", clazz, Collections.EMPTY_MAP);
    }

    protected <T> String[] getDeclaredFields(Class<T> clazz, Map<String, String> extraPrefixMapping) {
        return getDeclaredFields("", clazz, extraPrefixMapping);
    }

    protected <T> String[] getAllDeclaredFields(Class<T> clazz) {
        return getAllDeclaredFields("", clazz, Collections.EMPTY_MAP);
    }

    protected <T> String[] getAllDeclaredFields(String prefix, Class<T> clazz) {
        return getAllDeclaredFields(prefix, clazz, Collections.EMPTY_MAP);
    }

    private <T> String[] getAllDeclaredFields(String prefix, Class<T> clazz, Map<String, String> extraPrefixMapping) {
        Field[] fields = new Field[0];
        Class<? super T> clz = clazz;
        do {
            Field[] declaredFields = clz.getDeclaredFields();
            if (clz.getName().endsWith("BaseEntity")) {
                for (int i = 0; i < declaredFields.length; i++) {
                    if (declaredFields[i].getName().endsWith("serialVersionUID")) {
                        declaredFields = ArrayUtils.remove(declaredFields, i);
                        break;
                    }
                }
            }
            fields = ArrayUtils.addAll(fields, declaredFields);
            clz = clz.getSuperclass();
        } while (clz != Object.class);

        String[] select = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            field.setAccessible(true);
            if (hasValue(prefix) || !CollectionUtils.isEmpty(extraPrefixMapping)) {
                String customerPrefix = extraPrefixMapping.get(field.getName());
                if (hasValue(customerPrefix)) {
                    select[i] = concat(customerPrefix, ".", field.getName());
                } else {
                    select[i] = concat(prefix, ".", field.getName());
                }
            } else {
                select[i] = field.getName();
            }
        }
        return select;
    }

}
