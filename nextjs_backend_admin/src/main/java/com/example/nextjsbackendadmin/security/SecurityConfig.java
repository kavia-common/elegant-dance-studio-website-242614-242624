package com.example.nextjsbackendadmin.security;

import com.example.nextjsbackendadmin.config.AdminAuthProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers filters (simple token protection for admin routes).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public FilterRegistrationBean<AdminAuthFilter> adminAuthFilter(AdminAuthProperties props) {
        FilterRegistrationBean<AdminAuthFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new AdminAuthFilter(props));
        reg.setOrder(1);
        return reg;
    }
}
