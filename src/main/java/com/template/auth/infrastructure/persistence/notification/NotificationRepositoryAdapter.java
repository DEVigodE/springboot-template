package com.template.auth.infrastructure.persistence.notification;

import com.template.auth.application.notification.NotificationRepository;
import com.template.auth.application.shared.PaginatedResult;
import com.template.auth.application.shared.PaginationParams;
import com.template.auth.domain.notification.NotificationEntity;
import com.template.auth.infrastructure.persistence.shared.PageMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository jpa;

    public NotificationRepositoryAdapter(NotificationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public NotificationEntity create(NotificationEntity notification) {
        return toDomain(jpa.save(toJpa(notification, new NotificationJpaEntity())));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationEntity> findById(UUID id) {
        return jpa.findById(id).map(NotificationRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResult<NotificationEntity> findAllForUser(UUID userId, PaginationParams params) {
        var page = jpa.findAllByUserId(userId, PageMapper.toPageable(params, Sort.by("createdAt").descending()));
        return PageMapper.toResult(page, params, NotificationRepositoryAdapter::toDomain);
    }

    @Override
    public NotificationEntity save(NotificationEntity notification) {
        NotificationJpaEntity entity = jpa.findById(notification.getId()).orElseGet(NotificationJpaEntity::new);
        return toDomain(jpa.save(toJpa(notification, entity)));
    }

    private static NotificationJpaEntity toJpa(NotificationEntity notification, NotificationJpaEntity entity) {
        entity.setId(notification.getId());
        entity.setUserId(notification.getUserId());
        entity.setType(notification.getType());
        entity.setTitle(notification.getTitle());
        entity.setMessage(notification.getMessage());
        entity.setReadAt(notification.getReadAt());
        entity.setCreatedAt(notification.getCreatedAt());
        return entity;
    }

    static NotificationEntity toDomain(NotificationJpaEntity entity) {
        return NotificationEntity.reconstitute(entity.getId(), entity.getUserId(), entity.getType(),
                entity.getTitle(), entity.getMessage(), entity.getReadAt(), entity.getCreatedAt());
    }
}
