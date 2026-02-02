package ai.claritywalk.config;

import ai.claritywalk.security.CustomUserDetailsService;
import ai.claritywalk.security.JwtAuthenticationFilter;
import ai.claritywalk.security.JwtUtil;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = SecurityCorsPreflightTest.TestApp.class)
class SecurityCorsPreflightTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext)
                .addFilters(this.springSecurityFilterChain)
                .build();
    }

    @Configuration
    @EnableWebSecurity
    @Import(SecurityConfig.class)
    static class TestApp {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            return new JwtAuthenticationFilter(Mockito.mock(JwtUtil.class), Mockito.mock(CustomUserDetailsService.class));
        }

        @Bean
        UserDetailsService userDetailsService() {
            return username -> User.withUsername(username).password("{noop}pw").roles("USER").build();
        }

        @Bean
        TestAuthController testAuthController() {
            return new TestAuthController();
        }
    }

    @RestController
    static class TestAuthController {
        @RequestMapping(path = "/api/auth/login", method = RequestMethod.POST)
        void login() {
        }
    }

    @Test
    void preflight_allows_localhost5174_for_login() throws Exception {
        var result = mockMvc.perform(
                        options("/api/auth/login")
                                .header("Origin", "http://localhost:5174")
                                .header("Access-Control-Request-Method", "POST")
                                .header("Access-Control-Request-Headers", "content-type")
                )
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5174"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isIn(200, 204);
    }

    @Test
    void preflight_rejects_unknown_origin_for_login() throws Exception {
        var result = mockMvc.perform(
                        options("/api/auth/login")
                                .header("Origin", "http://evil.example")
                                .header("Access-Control-Request-Method", "POST")
                                .header("Access-Control-Request-Headers", "content-type")
                )
                .andReturn();

        assertThat(result.getResponse().getHeader("Access-Control-Allow-Origin")).isNull();
    }
}
