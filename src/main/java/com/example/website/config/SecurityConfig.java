package com.example.website.config;

import com.example.website.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; // 💡 Recommended to include
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // 💡 Recommended annotation
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (Login, Register, Static Files)
                .requestMatchers("/login", "/register", "/css/**", "/js/**", "/uploads/**", "/images/**").permitAll()

                // 🟢 FIX: Allow access to all standard dashboard/menu routes for authenticated users
                // This covers: /dashboard, /most-viewed, /most-liked, /celebrity-videos, /about
                // It also covers all interaction routes: /content/like/{id}, /content/comment/{id}, /content/view/{id}
                .requestMatchers(
                    "/dashboard", 
                    "/most-viewed", 
                    "/most-liked", 
                    "/celebrity-videos", 
                    "/about",
                    "/content/**" // General content interactions (like, view, comment)
                ).authenticated()

                // Only ADMIN can upload or delete content
                .requestMatchers("/upload", "/content/delete/**").hasRole("ADMIN")

                // 🟢 FIX: Any request that hasn't been explicitly allowed or protected is caught here.
                // We use .anyRequest().authenticated() to ensure a user must log in for anything else.
                .anyRequest().authenticated() // Ensures all unlisted pages are protected
            )

            // ✅ Custom Login Page
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )

            // ✅ Logout Configuration
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // ✅ Optional: Disable CSRF for local testing (Revert/Configure for production)
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
        provider.setUserDetailsService(userDetailsService); // Connects to your custom user fetch logic
        provider.setPasswordEncoder(passwordEncoder()); // Uses the secure password encoder
        return provider;
    }
}