package fr.kernelis.illico.securite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.util.Map;

@Configuration
public class SecurityConfig {

    /** Paramètres Argon2id recommandés par l'OWASP : 19 Mio de mémoire, 2 itérations, 1 fil d'exécution. */
    private static final int ARGON2_SEL_OCTETS = 16;
    private static final int ARGON2_EMPREINTE_OCTETS = 32;
    private static final int ARGON2_PARALLELISME = 1;
    private static final int ARGON2_MEMOIRE_KIO = 19_456;
    private static final int ARGON2_ITERATIONS = 2;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/connexion").permitAll()
                // La page d'erreur doit rester accessible, sinon une erreur 400 deviendrait une 401
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated())
            // Jeton CSRF dans un cookie lisible par Angular, qui le renvoie dans l'en-tête X-XSRF-TOKEN
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            // Une API consommée par l'application Angular répond 401, sans redirection vers une page de connexion
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    /**
     * Hachage Argon2id. L'encodeur « délégant » préfixe chaque empreinte de son algorithme ({argon2}) :
     * si l'algorithme change un jour, les anciens mots de passe restent vérifiables pendant la migration.
     */
    @Bean
    PasswordEncoder encodeurDeMotsDePasse() {
        Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(
            ARGON2_SEL_OCTETS, ARGON2_EMPREINTE_OCTETS, ARGON2_PARALLELISME, ARGON2_MEMOIRE_KIO, ARGON2_ITERATIONS);
        return new DelegatingPasswordEncoder("argon2", Map.of("argon2", argon2));
    }
}
