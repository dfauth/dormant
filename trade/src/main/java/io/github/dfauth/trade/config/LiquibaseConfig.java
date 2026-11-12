package io.github.dfauth.trade.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class LiquibaseConfig {

//    @Value("${spring.liquibase.change-log:classpath:db/changelog/db-changelog-master.xml}")
    @Value("${spring.liquibase.change-log}")
    private String changelog;

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        // Explicitly tells the app where the file lives
        liquibase.setChangeLog(changelog);
        return liquibase;
    }
}
