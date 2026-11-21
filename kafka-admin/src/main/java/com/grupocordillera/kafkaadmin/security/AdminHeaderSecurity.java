package com.grupocordillera.kafkaadmin.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AdminHeaderSecurity implements WebMvcConfigurer {

    @Value("${ep5.admin.token}")
    private String expected;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Object handler) throws Exception {
                String supplied = request.getHeader("X-EP5-Admin-Key");

                boolean valid = expected != null
                        && expected.length() >= 20
                        && supplied != null
                        && MessageDigest.isEqual(
                                expected.getBytes(StandardCharsets.UTF_8),
                                supplied.getBytes(StandardCharsets.UTF_8));

                if (!valid) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    response.getWriter().write(
                            "{\"error\":\"Acceso administrativo denegado\"}");
                    return false;
                }

                return true;
            }
        }).addPathPatterns("/admin/**");
    }
}
