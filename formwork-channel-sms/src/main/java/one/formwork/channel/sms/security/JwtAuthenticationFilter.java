package one.formwork.channel.sms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtDecoder decoder;

    public JwtAuthenticationFilter(JwtDecoder decoder) { this.decoder = decoder; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") &&
                SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Jwt jwt = decoder.decode(header.substring(7));
                Object roles = jwt.getClaims().get("roles");
                Collection<SimpleGrantedAuthority> authorities = roles instanceof Collection<?> values
                        ? values.stream().map(String::valueOf).map(SimpleGrantedAuthority::new).toList()
                        : java.util.List.of();
                var authentication = new UsernamePasswordAuthenticationToken(jwt.getSubject(), null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException ignored) {
                // The security chain will reject the request as unauthenticated.
            }
        }
        filterChain.doFilter(request, response);
    }
}
