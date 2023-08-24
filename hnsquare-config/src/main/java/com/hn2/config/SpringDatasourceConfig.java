package com.hn2.config;

import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.TransactionManager;

/**
 * 資料庫連線相關設定 JPA 必須指定 Entity 及 Repository 的 Package
 *
 * @author hsien
 */
@Slf4j
@Configuration
@EnableJpaRepositories(
    basePackages = {"com.hn2.*.repository"},
    entityManagerFactoryRef = "primaryEntityManagerFactory",
    transactionManagerRef = "primaryTransactionManager",
    excludeFilters = {
    } )
public class SpringDatasourceConfig {
  /** 資料庫驅動 class */
  @Value("${spring.datasource.driver-class-name}")
  private String driver;
  /** 資料庫連線位址 */
  @Value("${spring.datasource.url}")
  private String url;
  /** 資料庫人員帳號 */
  @Value("${spring.datasource.username}")
  private String user;
  /** 資料庫人員密碼 */
  @Value("${spring.datasource.mima}")
  private String mima;

  /**
   * 建立 DataSource Bean
   *
   * @return DataSource
   */
  @Bean
  @Primary
  public DataSource dataSource() {
    log.info("Configuring spring.datasources");
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName(driver);
    dataSource.setUrl(url);
    dataSource.setUsername(user);
    dataSource.setPassword(mima);
    return dataSource;
  }

  /**
   * 建立 LocalContainerEntityManagerFactoryBean Bean
   *
   * @param builder EntityManagerFactoryBuilder
   * @return LocalContainerEntityManagerFactoryBean
   */
  @Bean
  @Primary
  public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
      EntityManagerFactoryBuilder builder) {
    return builder
        .dataSource(dataSource())
        .packages("com.hn2.*.model")
        .persistenceUnit("primary")
        .build();
  }

  /**
   * 建立 TransactionManager Bean
   *
   * @param builder EntityManagerFactoryBuilder
   * @return TransactionManager
   */
  @Bean
  @Primary
  public TransactionManager primaryTransactionManager(EntityManagerFactoryBuilder builder) {
    JpaTransactionManager manager = new JpaTransactionManager();
    manager.setDataSource(dataSource());
    manager.setEntityManagerFactory(primaryEntityManagerFactory(builder).getObject());
    return manager;
  }
}
