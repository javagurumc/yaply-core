package ai.yaply.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import jakarta.servlet.FilterChain;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    @Test void deletedProfileDoesNotTurnAStaleTokenIntoServerError() throws Exception {
        var jwt = mock(JwtService.class);
        var users = mock(CustomUserDetailsService.class);
        var chain = mock(FilterChain.class);
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer stale-token");
        when(jwt.extractEmail("stale-token")).thenReturn("deleted@example.com");
        when(users.loadUserByUsername("deleted@example.com")).thenThrow(new UsernameNotFoundException("missing"));
        SecurityContextHolder.clearContext();
        try {
            new JwtAuthenticationFilter(jwt, users).doFilter(request, response, chain);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(chain).doFilter(request, response);
        } finally { SecurityContextHolder.clearContext(); }
    }
}
