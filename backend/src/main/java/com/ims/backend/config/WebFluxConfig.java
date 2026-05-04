package com.ims.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * WebFlux configuration.
 * Increases in-memory buffer limit for large batch payloads (up to 10 MB).
 */
@Configuration
@EnableWebFlux
public class WebFluxConfig implements WebFluxConfigurer {

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        // Allow up to 10 MB per request body (burst signal payloads)
        configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024);
    }
}
