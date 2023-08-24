package com.hn2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 系統相關設定
 *
 * @author hsien
 */
@Configuration
public class ApplicationConfig {

  /**
   * 建立 Sql2o Bean
   *
   * @param ds DataSource
   * @return Sql2o
   */
  @Bean
  org.sql2o.Sql2o sql2o(javax.sql.DataSource ds) {
    return new org.sql2o.Sql2o(ds);
  }

  /**
   * 建立 ModelMapper Bean
   *
   * @return ModelMapper
   */
  @Bean
  public org.modelmapper.ModelMapper modelMapper() {
    org.modelmapper.ModelMapper modelMapper = new org.modelmapper.ModelMapper();
    org.modelmapper.config.Configuration modelMapperConfiguration = modelMapper.getConfiguration();
    modelMapperConfiguration.setMatchingStrategy(org.modelmapper.convention.MatchingStrategies.STRICT);
    modelMapperConfiguration.setPropertyCondition(org.modelmapper.Conditions.isNotNull());
    return modelMapper;
  }
}