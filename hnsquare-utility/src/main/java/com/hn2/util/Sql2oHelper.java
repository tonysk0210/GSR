package com.hn2.util;

import org.simpleflatmapper.sql2o.SfmResultSetHandlerFactoryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import java.util.List;
import java.util.Map;

/**
 * Sql2o Helper 提供 Sql2o 擴充功能
 *
 * @author miyuki
 */
@Component
public class Sql2oHelper {

  /** Sql2o */
  @Autowired Sql2o sql2o;

  private Connection connection;

    /**
   * 取資料
   *
   * @param sql sql語法
   * @param params 參數
   * @param clasz 類別
   * @param <T> 泛型
   * @return 泛型List
   */
  public <T> List<T> queryList(String sql, Map<String, Object> params, Class<T> clasz) {
    try (Connection con = sql2o.open()) {

      try (Query query = con.createQuery(sql)) {
        if (null != params) {
          for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.addParameter(entry.getKey(), entry.getValue());
          }
        }

        // SimpleFlatMapper
        query.setAutoDeriveColumnNames(true);
        query.setResultSetHandlerFactoryBuilder(new SfmResultSetHandlerFactoryBuilder());
        return (List<T>) query.executeAndFetch(clasz);
      }
    }
  }

  /**
   * 取資料
   *
   * @param sql sql語法
   * @param clasz 類別
   * @param <T> 泛型
   * @return 泛型List
   */
  public <T> List<T> queryList(String sql, Class<T> clasz) {
    return queryList(sql, null, clasz);
  }

  /**
   * 取資料筆數
   *
   * @param sql sql語法
   * @param params 參數
   * @return Object
   */
  public Object executeScalar(String sql, Map<String, Object> params) {
    try (Connection con = sql2o.open()) {

      try (Query query = con.createQuery(sql)) {
        if (null != params) {
          for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.addParameter(entry.getKey(), entry.getValue());
          }
        }
        return query.executeScalar();
      }
    }
  }

  /**
   * 取資料筆數
   *
   * @param sql
   * @return Object
   */
  public Object executeScalar(String sql) {
    return executeScalar(sql, null);
  }

  /**
   * 執行
   *
   * @param sql sql語法
   * @param params 參數
   * @return Object
   */
  public void executeUpdate(String sql, Map<String, Object> params) {
    try (Connection con = sql2o.open()) {

      try (Query query = con.createQuery(sql)) {
        if (null != params) {
          for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.addParameter(entry.getKey(), entry.getValue());
          }
        }
        query.executeUpdate();
      }
    }
  }

  public void exec(String sql, Map<String, Object> params) {
      try (Connection con = sql2o.beginTransaction();
           Query query = con.createQuery(sql)) {
          if (null != params) {
              for (Map.Entry<String, Object> entry : params.entrySet()) {
                  query.addParameter(entry.getKey(), entry.getValue());
              }
          }
          connection = con;
          query.executeUpdate();
          con.commit();
      } catch (Exception e) {
          //e.printStackTrace();

          connection.rollback();
          throw new BusinessException(ErrorType.UNKNOWN, e.getMessage());
      }
  }
}
