package com.example.inventory_management.config;

import com.example.inventory_management.entity.Role;
import com.example.inventory_management.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsernameIgnoreCase(username)
                .map(u -> org.springframework.security.core.userdetails.User
                        .withUsername(u.getUsername())
                        .password(u.getPasswordHash())
                        .disabled(!u.isEnabled())
                        .authorities(u.getRoles().stream()
                                .map(Role::name)
                                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                                .toList())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/auth/**", "/products/**", "/orders/**", "/cart/**", "/admin/**", "/seller/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/auth/register",
                                "/css/**", "/js/**", "/images/**", "/webjars/**", "/uploads/**").permitAll()

                        // Public product browsing (REST + UI)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/products/**", "/ui/products").permitAll()

                        // Buyer cart; orders visible to buyers and admins
                        .requestMatchers("/ui/cart/**").hasRole("BUYER")
                        .requestMatchers("/ui/orders", "/ui/orders/**").hasAnyRole("BUYER", "ADMIN")

                        // Admin UI & reports
                        .requestMatchers("/ui/admin/**", "/ui/reports").hasRole("ADMIN")

                        // Seller dashboard
                        .requestMatchers("/ui/seller/**").hasRole("SELLER")

                        // Authenticated UI (create/update products from catalog)
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/ui/products", "/ui/products/**").authenticated()

                        // Role-based REST
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/seller/**").hasRole("SELLER")
                        .requestMatchers("/buyer/**").hasRole("BUYER")
                        .requestMatchers("/cart/**", "/orders/**").hasRole("BUYER")

                        // everything else must authenticate
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                )
                .logout(logout -> logout.logoutUrl("/auth/logout"));

        return http.build();
    }
}
