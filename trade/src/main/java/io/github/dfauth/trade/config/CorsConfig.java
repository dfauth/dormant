package io.github.dfauth.trade.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.origins}")
    private String origins;

    @Value("${cors.methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String methods;

    @Value("${cors.headers:*}")
    private String headers;

    @Value("${cors.allow.credentials:true}")
    private boolean allowCredentials;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(origins.split(","))
                .allowedMethods(methods.split(","))
                .allowedHeaders(headers)
                .allowCredentials(allowCredentials);
    }

}
