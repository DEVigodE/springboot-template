package com.template.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class AuthTemplateApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AuthTemplateApplication.class, args);

        // The `seed` profile is a one-shot command (infrastructure/seed): once the
        // DatabaseSeeder runner has finished, exit instead of serving traffic.
        if (context.getEnvironment().acceptsProfiles(Profiles.of("seed"))) {
            System.exit(SpringApplication.exit(context));
        }
    }
}
