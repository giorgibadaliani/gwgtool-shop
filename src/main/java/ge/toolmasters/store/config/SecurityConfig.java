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
                .authorizeHttpRequests((requests) -> requests
                        // აი აქ ჩაამატე "/order-success"
                        .requestMatchers("/", "/product/**", "/cart/**", "/checkout/**", "/order-success", "/images/**", "/css/**", "/uploads/**").permitAll()

                        .anyRequest().authenticated()
                )

                .formLogin((form) -> form
                        .permitAll() // Login გვერდი ყველასთვის ღიაა
                        .defaultSuccessUrl("/", true) // შესვლის მერე გადადი მთავარზე
                )
                .logout((logout) -> logout.permitAll());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // დროებითი ადმინი
        UserDetails admin = User.withDefaultPasswordEncoder()
                .username("admin")
                .password("admin123") // პაროლი: admin123
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }
}
