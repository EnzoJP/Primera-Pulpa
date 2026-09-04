package com.primeraPulpa.configs;

import com.primeraPulpa.Services.UsuarioService;
import com.primeraPulpa.repositories.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
  private final UsuarioRepository usuarioRepository;

  public SecurityConfig(UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  @Bean
  public UserDetailsService userDetailsService() {

    return email -> usuarioRepository.findByEmail(email)
        .filter(u -> !Boolean.TRUE.equals(u.getEliminado()))
        .map(usuario -> User
            .withUsername(usuario.getEmail())
            .password(usuario.getPasswordHash())
            .roles(usuario.getRol().getDescripcion())
            .build())
        .orElseThrow(() -> new UsernameNotFoundException(
            "Usuario no encontrado"));
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/error/**").permitAll()
            .requestMatchers("/usuarios/**", "/backups/**", "/formulas/nuevo", "/formulas/*/editar" ,"/materias-primas/*","/materias-primas/*/editar","/materias-primas/nuevo","/materias-primas/*/eliminar", "/unidades-medida/**",
                    "/mixes/nuevo","/mixes/*/editar","/elaboracion/*/editar", "/pedidos/*/editar","/pedidos/nuevo","/estadisticas/**" ,"/clientes/**","/costos-adicionales/**").hasRole("ADMIN")
            .anyRequest().hasAnyRole("ADMIN", "EMPLEADO"))
        .formLogin(form -> form
            .loginPage("/login")
            .usernameParameter("username")
            .passwordParameter("password")
            .defaultSuccessUrl("/dashboard", true)
            .permitAll())
        .logout(logout -> logout
            .logoutSuccessUrl("/login?logout")
            .permitAll());

    return http.build();
  }

}