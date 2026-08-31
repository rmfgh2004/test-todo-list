package com.timetable.todo.platform;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** SECURITY-01, SECURITY-12: Fails the file profile fast when the database is not safely keyed. */
@Configuration(proxyBeanMethods = false)
@Profile("file")
class FileDatasourceValidator {

  FileDatasourceValidator(
      @Value("${spring.datasource.url:}") String url,
      @Value("${spring.datasource.password:}") String password) {
    FileDatasourceGuard.validate(url, password);
  }
}
