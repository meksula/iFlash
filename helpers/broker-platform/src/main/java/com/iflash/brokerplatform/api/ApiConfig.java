package com.iflash.brokerplatform.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/** Wires the JWT guard, the {@link CurrentUserId} resolver and dev CORS for the Angular app. */
@Configuration
class ApiConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final CurrentUserIdResolver currentUserIdResolver;
    private final String allowedOrigin;

    ApiConfig(JwtAuthInterceptor jwtAuthInterceptor, CurrentUserIdResolver currentUserIdResolver,
              @Value("${ibp.cors.allowed-origin}") String allowedOrigin) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.currentUserIdResolver = currentUserIdResolver;
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login");
    }

    @Override
    public void addArgumentResolvers(List<org.springframework.web.method.support.HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserIdResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
