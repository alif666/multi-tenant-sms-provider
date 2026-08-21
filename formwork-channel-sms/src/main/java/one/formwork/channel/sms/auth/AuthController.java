package one.formwork.channel.sms.auth;

import one.formwork.channel.sms.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final UUID ADMIN_ROLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AppUserRepository users;
    private final AppRoleRepository roles;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
                          PasswordEncoder passwordEncoder, AppUserRepository users, AppRoleRepository roles) {
        this.authenticationManager = authenticationManager; this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder; this.users = users; this.roles = roles;
    }

    @PostMapping({"/register/public", "/register"})
    public ResponseEntity<?> register(@RequestBody RegistrationRequest request) {
        if (request.username() == null || request.username().isBlank() ||
                request.password() == null || request.password().length() < 12) {
            return ResponseEntity.badRequest().body(Map.of("error", "username and a 12-character password are required"));
        }
        if (users.existsByUsernameIgnoreCase(request.username().trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "username already exists"));
        }
        AppRole admin = roles.findByName("ROLE_ADMIN")
                .orElseGet(() -> roles.save(new AppRole(ADMIN_ROLE_ID, "ROLE_ADMIN")));
        AppUser user = users.save(new AppUser(request.username().trim(), passwordEncoder.encode(request.password()), admin));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", user.getId(), "username", user.getUsername(), "role", "ROLE_ADMIN"));
    }

    @PostMapping({"/login/public", "/login"})
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            AppUser user = (AppUser) authentication.getPrincipal();
            return ResponseEntity.ok(Map.of("token", jwtService.createToken(user), "username", user.getUsername(), "roles", user.getAuthorities()));
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid username or password"));
        }
    }

    public record RegistrationRequest(String username, String password) {}
    public record LoginRequest(String username, String password) {}
}
