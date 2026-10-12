package com.zone.lesoongen.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zone.lesoongen.application.AppException;
import com.zone.lesoongen.config.PlatformSecurityProperties;
import com.zone.lesoongen.domain.shared.Ulids;

@Service
public class PlatformUserService implements UserDetailsService {
    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9][a-z0-9._-]{3,31}$");

    private final PlatformUserRepository users;
    private final PasswordEncoder passwords;
    private final PlatformSecurityProperties security;
    private final Clock clock;

    public PlatformUserService(PlatformUserRepository users, PasswordEncoder passwords,
            PlatformSecurityProperties security, Clock clock) {
        this.users = users;
        this.passwords = passwords;
        this.security = security;
        this.clock = clock;
    }

    @Transactional
    public PlatformUser register(String rawUsername, String password, String displayName) {
        if (!security.registrationEnabled()) {
            throw new AppException(HttpStatus.FORBIDDEN, "REGISTRATION_DISABLED", "当前未开放注册");
        }
        String username = normalize(rawUsername);
        if (!USERNAME.matcher(username).matches()) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "USERNAME_INVALID",
                    "用户名须为 4–32 位小写字母、数字、点、下划线或连字符");
        }
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "PASSWORD_INVALID",
                    "密码长度须为 8–72 个字符");
        }
        String name = displayName == null ? "" : displayName.trim();
        if (name.isBlank() || name.length() > 100) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "DISPLAY_NAME_INVALID",
                    "显示名称不能为空且不能超过 100 个字符");
        }
        if (users.findByUsername(username).isPresent()) {
            throw new AppException(HttpStatus.CONFLICT, "USERNAME_EXISTS", "用户名已被使用");
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        PlatformUser created = new PlatformUser(Ulids.next(clock), username,
                passwords.encode(password), name, "USER", "ACTIVE", now, now, null);
        users.insert(created);
        return created;
    }

    public PlatformUser requireActive(String username) {
        PlatformUser user = users.findByUsername(normalize(username))
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));
        if (!user.active()) {
            throw new UsernameNotFoundException("user disabled");
        }
        return user;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        PlatformUser user = requireActive(username);
        return User.withUsername(user.username())
                .password(user.passwordHash())
                .roles(user.role())
                .disabled(!user.active())
                .build();
    }

    static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
