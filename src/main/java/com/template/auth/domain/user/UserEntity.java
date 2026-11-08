package com.template.auth.domain.user;

import com.template.auth.domain.shared.BaseEntity;
import com.template.auth.domain.shared.exception.UserInactiveException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class UserEntity extends BaseEntity {

    private String name;
    private String email;
    private String passwordHash;
    private boolean active;
    private Instant emailVerifiedAt;
    private List<String> roles;
    private List<String> permissions;

    private UserEntity(UUID id, String name, String email, String passwordHash, boolean active,
            Instant emailVerifiedAt, List<String> roles, List<String> permissions, Instant createdAt,
            Instant updatedAt, Instant deletedAt, UUID createdBy, UUID updatedBy) {
        super(id, createdAt, updatedAt, deletedAt, createdBy, updatedBy);
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
        this.emailVerifiedAt = emailVerifiedAt;
        this.roles = List.copyOf(roles);
        this.permissions = List.copyOf(permissions);
    }

    public static UserEntity create(UUID id, String name, String email, String passwordHash, Instant now,
            UUID createdBy) {
        return new UserEntity(id, name, email, passwordHash, true, null, List.of(), List.of(), now, now, null,
                createdBy, null);
    }

    public static UserEntity reconstitute(UUID id, String name, String email, String passwordHash, boolean active,
            Instant emailVerifiedAt, List<String> roles, List<String> permissions, Instant createdAt,
            Instant updatedAt, Instant deletedAt, UUID createdBy, UUID updatedBy) {
        return new UserEntity(id, name, email, passwordHash, active, emailVerifiedAt, roles, permissions, createdAt,
                updatedAt, deletedAt, createdBy, updatedBy);
    }

    public boolean hasRole(String roleName) {
        return roles.contains(roleName);
    }

    /**
     * Flattened across every role the user has - a role's own permission set is
     * the unit of assignment (see RoleEntity.hasPermission()), a user's is just
     * the union of all their roles'.
     */
    public boolean hasPermission(String permissionName) {
        return permissions.contains(permissionName);
    }

    public void verifyActive() {
        if (!active || !isActive()) {
            throw new UserInactiveException();
        }
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void verifyEmail() {
        this.emailVerifiedAt = Instant.now();
    }

    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isEnabled() {
        return active;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }
}
