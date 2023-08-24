package com.hn2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Swagger 設定檔
 *
 * @author hsien
 */
@Configuration
@EnableSwagger2
@Profile("!prod")
public class SwaggerConfig {

  /**
   * Swagger 相關設定
   *
   * @return Docket
   */
  @Bean
  public Docket buildDocket() {

    ApiInfo info = new ApiInfoBuilder().title("HN2 API").version("1.0").build();

    return new Docket(DocumentationType.OAS_30)
        .apiInfo(info)
        .select()
        .apis(RequestHandlerSelectors.any())
        // .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
        .paths(PathSelectors.any())
        .build();
  }
}
