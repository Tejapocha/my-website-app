package com.example.website.config;

import com.example.website.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                // 1. PUBLIC ACCESS (Viewers can see all content pages without login)
                // This is correct based on your requirement.
                .requestMatchers(
                    "/", // Root access
                    "/dashboard",
                    "/most-viewed",
                    "/most-liked",
                    "/celebrity-videos",
                    "/about",
                    "/view/**" // Allows access to the /view/{id} redirect/AJAX endpoint
                ).permitAll()
                
                // 2. AUTHENTICATED ACCESS (Logged-in user required for interaction)
                // Liking and commenting requires a user ID, so only logged-in users can do this.
                .requestMatchers(
                    "/like/**",      // /like/{id}
                    "/comment/**"    // /comment/{id}
                ).authenticated()

                // 3. ADMIN ACCESS (Upload and Delete)
                // Only users with the ADMIN role can access these critical endpoints.
                .requestMatchers(
                    "/upload", 
                    "/delete/**"     // /delete/{id}
                ).hasRole("ADMIN")
                
                // Public Endpoints (Login, Register, Static Files)
                .requestMatchers("/login", "/register", "/css/**", "/js/**", "/uploads/**", "/images/**").permitAll()
                
                // Ensures all unlisted pages are protected by default
                .anyRequest().authenticated()
            )

            // Custom Login Page
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )

            // Logout Configuration
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // Disable CSRF for simpler development. Reconfigure for production!
            .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt is the standard, secure password hashing algorithm
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService); 
        provider.setPasswordEncoder(passwordEncoder()); 
        return provider;
    }
}
