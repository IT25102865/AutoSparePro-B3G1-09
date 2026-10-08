package com.lankaautoparts.autosparepro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthenticationSuccessHandler loginSuccessHandler;

    public SecurityConfig(AuthenticationSuccessHandler loginSuccessHandler) {
        this.loginSuccessHandler = loginSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/signup", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/catalog", "/catalog/**").hasAnyRole("ADMIN", "USER")
                // NOTE: "/payments/**" here covers /payments/pay/** (pay form) and
                // /payments/{id} (receipt view) for the owning customer. It does NOT
                // match "/admin/payments/**" (different prefix), which stays
                // admin-only via the anyRequest() rule below.
                .requestMatchers("/account", "/account/**", "/my-requests", "/my-purchases", "/my-purchases/**", "/payments/**", "/quote", "/quote/**", "/cart", "/cart/**").hasAnyRole("ADMIN", "USER")
                .anyRequest().hasRole("ADMIN")
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(loginSuccessHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }
}
