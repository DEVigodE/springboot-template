package com.template.auth.infrastructure.seed;

import com.template.auth.application.user.PasswordHasherPort;
import com.template.auth.config.AppProperties;
import com.template.auth.infrastructure.persistence.permission.PermissionJpaEntity;
import com.template.auth.infrastructure.persistence.permission.PermissionJpaRepository;
import com.template.auth.infrastructure.persistence.role.RoleJpaEntity;
import com.template.auth.infrastructure.persistence.role.RoleJpaRepository;
import com.template.auth.infrastructure.persistence.user.UserJpaEntity;
import com.template.auth.infrastructure.persistence.user.UserJpaRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Demo data: permissions, roles and three users. Runs only under the `seed`
 * profile and the process exits right after (see AuthTemplateApplication):
 *
 * <pre>./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,seed</pre>
 */
@Component
@Profile("seed")
public class DatabaseSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);
    private static final String SEED_PASSWORD = "Senha123";

    private record PermissionSeed(String name, String description) {
    }

    private record RoleSeed(String name, String description, List<String> permissions) {
    }

    private record UserSeed(String name, String email, String role) {
    }

    private static final List<PermissionSeed> PERMISSIONS = List.of(
            new PermissionSeed("users:read", "Read users"),
            new PermissionSeed("users:write", "Create/update users"),
            new PermissionSeed("roles:read", "Read roles"),
            new PermissionSeed("roles:write", "Create/update roles"),
            new PermissionSeed("permissions:read", "Read permissions"),
            new PermissionSeed("permissions:write", "Create/update permissions"),
            new PermissionSeed("payments:read", "Read payments"),
            new PermissionSeed("payments:write", "Create/update payments"),
            new PermissionSeed("reports:read", "Read reports"));

    private static final List<RoleSeed> ROLES = List.of(
            new RoleSeed("admin", "Full access", PERMISSIONS.stream().map(PermissionSeed::name).toList()),
            new RoleSeed("support", "Customer support", List.of("users:read", "users:write")),
            new RoleSeed("financial", "Financial operations",
                    List.of("payments:read", "payments:write", "reports:read")));

    private static final List<UserSeed> USERS = List.of(
            new UserSeed("Admin", "admin@example.com", "admin"),
            new UserSeed("Support", "support@example.com", "support"),
            new UserSeed("Financial", "financial@example.com", "financial"));

    private final AppProperties properties;
    private final PermissionJpaRepository permissions;
    private final RoleJpaRepository roles;
    private final UserJpaRepository users;
    private final PasswordHasherPort passwordHasher;

    public DatabaseSeeder(AppProperties properties, PermissionJpaRepository permissions, RoleJpaRepository roles,
            UserJpaRepository users, PasswordHasherPort passwordHasher) {
        this.properties = properties;
        this.permissions = permissions;
        this.roles = roles;
        this.users = users;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (properties.isProduction()) {
            throw new IllegalStateException("Refusing to seed: APP_ENV=production. This creates demo accounts "
                    + "with a hardcoded password - never run it against a production database.");
        }

        Instant now = Instant.now();

        Map<String, PermissionJpaEntity> permissionByName = new HashMap<>();
        for (PermissionSeed seed : PERMISSIONS) {
            PermissionJpaEntity permission = permissions.findByNameAndDeletedAtIsNull(seed.name()).orElseGet(() -> {
                PermissionJpaEntity created = new PermissionJpaEntity();
                created.setId(UUID.randomUUID());
                created.setName(seed.name());
                created.setCreatedAt(now);
                return created;
            });
            permission.setDescription(seed.description());
            permission.setUpdatedAt(now);
            permissionByName.put(seed.name(), permissions.save(permission));
        }

        Map<String, RoleJpaEntity> roleByName = new HashMap<>();
        for (RoleSeed seed : ROLES) {
            RoleJpaEntity role = roles.findWithPermissionsByNameAndDeletedAtIsNull(seed.name()).orElseGet(() -> {
                RoleJpaEntity created = new RoleJpaEntity();
                created.setId(UUID.randomUUID());
                created.setName(seed.name());
                created.setCreatedAt(now);
                return created;
            });
            role.setDescription(seed.description());
            role.setUpdatedAt(now);
            seed.permissions().forEach(name -> role.getPermissions().add(permissionByName.get(name)));
            roleByName.put(seed.name(), roles.save(role));
        }

        String passwordHash = passwordHasher.hash(SEED_PASSWORD);
        for (UserSeed seed : USERS) {
            UserJpaEntity user = users.findWithRolesByEmailAndDeletedAtIsNull(seed.email()).orElseGet(() -> {
                UserJpaEntity created = new UserJpaEntity();
                created.setId(UUID.randomUUID());
                created.setName(seed.name());
                created.setEmail(seed.email());
                created.setPasswordHash(passwordHash);
                created.setActive(true);
                created.setCreatedAt(now);
                created.setUpdatedAt(now);
                return created;
            });
            user.getRoles().add(roleByName.get(seed.role()));
            users.save(user);
        }

        log.info("Seeded {} permissions, {} roles, {} users (password: \"{}\"):", PERMISSIONS.size(), ROLES.size(),
                USERS.size(), SEED_PASSWORD);
        USERS.forEach(user -> log.info("  - {} ({})", user.email(), user.role()));
    }
}
