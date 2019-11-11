package com.microservice.authentication.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.authentication.common.service.SharedAuthenticationService;
import com.microservice.authentication.dto.JwtTokenDto;
import com.microservice.authentication.web.util.CustomDefaultErrorAttributes;
import com.microservice.jwt.common.TokenProvider;
import lombok.AllArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Spring Security Configuration for form
 */
@Configuration
@EnableWebSecurity
@AllArgsConstructor
@Order(2)
public class SpringSecurityFormConfiguration extends WebSecurityConfigurerAdapter {
    private final SharedAuthenticationService sharedAuthenticationService;

    private final TokenProvider tokenProvider;

    private final ObjectMapper objectMapper;

    private final CustomDefaultErrorAttributes customDefaultErrorAttributes;

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected UserDetailsService userDetailsService() {
        return sharedAuthenticationService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.requestMatchers()
            .antMatchers("/**")
            .and()
            .authorizeRequests()
            .anyRequest().authenticated()
            .and()
            .formLogin()
            .loginProcessingUrl("/api/authenticate").permitAll()
            .successHandler(successHandler())
            .failureHandler(authenticationFailureHandler())
            .and()
            .logout()
            .logoutUrl("/api/logout")
            .deleteCookies("SESSIONID")
            .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
            .logoutRequestMatcher(new AntPathRequestMatcher("/api/logout", HttpMethod.GET.name()))
            .invalidateHttpSession(true)
            .and()
            .csrf()
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .and()
            .exceptionHandling()
            .authenticationEntryPoint(new Http403ForbiddenEntryPoint());
    }

    private AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            if (validateApiPath(request)) {
                Map<String, Object> errorAttributes = customDefaultErrorAttributes.getErrorAttributes(request, exception, true);
                response.setStatus(Integer.parseInt(errorAttributes.get("status").toString()));
                response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().append(objectMapper.writeValueAsString(errorAttributes));
            } else {
                new SimpleUrlAuthenticationFailureHandler().onAuthenticationFailure(request, response, exception);
            }
        };
    }

    private boolean validateApiPath(HttpServletRequest request) {
        return StringUtils.isNotBlank(request.getPathInfo()) && request.getPathInfo().startsWith("/api/") ||
            StringUtils.isNotBlank(request.getServletPath()) && request.getServletPath().startsWith("/api/");
    }

    private AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            if (validateApiPath(request)) {
                String authorization = "Bearer " + tokenProvider.createToken(authentication, authentication.getName(), "true" .equals(request.getParameter("rememberMe")));
                JwtTokenDto jwtToken = new JwtTokenDto(authorization);
                response.addHeader(HttpHeaders.AUTHORIZATION, authorization);
                response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                response.addHeader("sessionId", request.getSession().getId());
                response.setStatus(HttpStatus.OK.value());
                response.getWriter().append(objectMapper.writeValueAsString(jwtToken));
            } else {
                new SavedRequestAwareAuthenticationSuccessHandler().onAuthenticationSuccess(request, response, authentication);
            }
        };
    }

}