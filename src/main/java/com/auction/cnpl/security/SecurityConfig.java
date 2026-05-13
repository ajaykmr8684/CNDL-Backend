package com.auction.cnpl.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final IpAddressFilter ipAddressFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService, IpAddressFilter ipAddressFilter) {
        this.userDetailsService = userDetailsService;
        this.ipAddressFilter = ipAddressFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Define AuthenticationManager explicitly
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors().and()
                .csrf().disable()
                .authorizeRequests()
                // You can uncomment and adjust these if you need role-based auth in addition to IP filtering
                // .antMatchers("/api/auction/bid").authenticated()
                // .antMatchers("/api/auction/sold", "/api/auction/unsold").hasRole("ADMIN")
                .antMatchers("/api/auction/bid", "/api/auction/sold").permitAll() // We'll control access via IP filter
                .antMatchers("/api/**", "/ws/**").permitAll()
                .anyRequest().permitAll()
                .and()
                .httpBasic()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .addFilterBefore(ipAddressFilter, FilterSecurityInterceptor.class); // Add our custom IP filter

        return http.build();
    }
}