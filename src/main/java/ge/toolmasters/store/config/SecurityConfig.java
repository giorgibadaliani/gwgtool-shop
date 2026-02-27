package ge.toolmasters.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ვთიშავთ CSRF-ს, რომ POST მოთხოვნებმა (კალათაში დამატება, შეკვეთა) იმუშავოს
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests((requests) -> requests
                        // დავამატე /category/** აქ
                        .requestMatchers("/", "/product/**", "/category/**", "/cart/**", "/checkout/**",
                                "/order-success", "/images/**", "/css/**",
                                "/uploads/**", "/sitemap.xml", "/robots.txt").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin((form) -> form
                        .permitAll()
                        .defaultSuccessUrl("/", true)
                )
                .logout((logout) -> logout.permitAll());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.withDefaultPasswordEncoder()
                .username("admin")
                .password("IhUEHi53ESdjAe45vt267KinE875tiIRN323GSBqKymGkada6433")
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }
}
