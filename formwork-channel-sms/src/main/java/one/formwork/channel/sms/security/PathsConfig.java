package one.formwork.channel.sms.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class PathsConfig {

    @Bean(name = "publicPaths")
    public List<String> publicPaths() {
        return List.of(
                "/auth/register/public",
                "/auth/register",
                "/auth/login/public",
                "/auth/login",
                "/csrf-token/public",
                "/error",
                "/demo/gateways",
                "/demo/providers/**"
        );
    }

    @Bean(name = "adminPaths")
    public List<String> adminPaths() {
        return List.of(
                "/api/admin/**",
                "/demo/tenants/**"
        );
    }

    @Bean(name = "securedPaths")
    public List<String> securedPaths() {
        return List.of(
                "/api/**",
                "/demo/**"
        );
    }
}
