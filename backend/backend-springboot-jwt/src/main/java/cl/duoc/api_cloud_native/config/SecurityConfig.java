package cl.duoc.api_cloud_native.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// Agregados post roles
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.ArrayList;
import java.util.Collection;


import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {

        http

                /*
                 * Habilitar CORS
                 */
                .cors(
                        Customizer.withDefaults())

                /*
                 * API REST
                 */
                .csrf(
                        csrf -> csrf.disable())

                /*
                 * No utilizaremos
                 * sesiones HTTP.
                 */
                .sessionManagement(
                        session -> session
                                .sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS))

                /*
                 * Seguridad de URIs
                 */
                .authorizeHttpRequests(

                        auth -> auth

                                /*
                                 * URI pública
                                 */
                                .requestMatchers(
                                        "/public/**")
                                .permitAll()

                                /*
                                 * URI protegida
                                 */
                                .requestMatchers(
                                        "/api/**")
                                .hasAuthority(
                                        "SCOPE_acceso_as_user")

                                /*
                                 * Todo lo demás
                                 */
                                .anyRequest()
                                .authenticated()

                )

                /*
                 * Validación JWT
                 */
                .oauth2ResourceServer(

                        oauth2 ->

                        oauth2.jwt(
                                jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))

                );

        return http.build();

    }

    /*
     * Configuracion JwtAuthentication
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter conversorScopes = new JwtGrantedAuthoritiesConverter();

        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();

        conversor.setJwtGrantedAuthoritiesConverter(jwt -> {

            Collection<GrantedAuthority> scopes = conversorScopes.convert(jwt);

            ArrayList<GrantedAuthority> autoridades = new ArrayList<>();

            if (scopes != null) {
                autoridades.addAll(scopes);
            }

            List<String> roles = jwt.getClaimAsStringList("roles");

            if (roles != null) {
                roles.forEach(
                        rol -> autoridades.add(
                                new SimpleGrantedAuthority(rol)));
            }

            return autoridades;
        });

        return conversor;
    }

    //Configuración CORS
     
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${mesatech.cors.origenes:http://localhost:3000}") List<String> origenesPermitidos) {

        CorsConfiguration config = new CorsConfiguration();

        //React
         
        config.setAllowedOrigins(origenesPermitidos);

        //Métodos HTTP
         
        config.setAllowedMethods(

                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS")

        );

        //Headers permitidos
         
        config.setAllowedHeaders(

                List.of(
                        "Authorization",
                        "Content-Type")

        );

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                config);

        return source;

    }

}
