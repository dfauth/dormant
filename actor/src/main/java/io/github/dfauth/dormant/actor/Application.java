package io.github.dfauth.dormant.actor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"io.github.dfauth.dormant.model", "io.github.dfauth.dormant.actor"})
@EnableJpaRepositories(basePackages = {"io.github.dfauth.dormant.model.repository", "io.github.dfauth.dormant.actor.repository"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
