package et.yoseph.lms.notification.service;

import et.yoseph.lms.notification.domain.InAppNotification;
import et.yoseph.lms.notification.repo.InAppNotificationRepository;
import et.yoseph.lms.notification.web.dto.BroadcastRequest;
import et.yoseph.lms.notification.web.dto.BroadcastResponse;
import et.yoseph.lms.notification.web.dto.InAppNotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Persists in-app notifications and schedules delivery after commit.
 * Email/SMS are not implemented here: {@link NotificationDispatchService} delivers each
 * {@link NotificationPush} to SSE, then {@link ExternalNotificationDeliveryService} sends
 * Pingram email/SMS when the user’s preferences and contact info allow it.
 */
@Service
public class InAppNotificationService {

    private final InAppNotificationRepository repository;
    private final KeycloakUserIdByRoleResolver roleResolver;
    private final NotificationDispatchService dispatchService;

    public InAppNotificationService(
            InAppNotificationRepository repository,
            KeycloakUserIdByRoleResolver roleResolver,
            NotificationDispatchService dispatchService) {
        this.repository = repository;
        this.roleResolver = roleResolver;
        this.dispatchService = dispatchService;
    }

    @Transactional
    public BroadcastResponse broadcast(BroadcastRequest request) {
        Set<String> recipientSubs = resolveRecipientUserIds(request);
        if (recipientSubs.isEmpty()) {
            throw new BadRequestException("No recipients: add Keycloak user ids and/or realm role names (e.g. lms-staff, lms-user).");
        }

        Instant now = Instant.now();
        List<InAppNotification> entities = new ArrayList<>(recipientSubs.size());
        for (String sub : recipientSubs) {
            InAppNotification row = new InAppNotification();
            row.setUserSub(sub);
            row.setTitle(request.title().trim());
            row.setBody(request.body().trim());
            row.setCreatedAt(now);
            entities.add(row);
        }
        repository.saveAll(entities);

        List<NotificationPush> pushes = new ArrayList<>(entities.size());
        for (InAppNotification row : entities) {
            pushes.add(new NotificationPush(
                    row.getUserSub(),
                    row.getId(),
                    row.getTitle(),
                    row.getBody(),
                    row.getCreatedAt().toEpochMilli()));
        }

        Runnable dispatch = () -> dispatchService.enqueueAfterCommit(pushes);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
        } else {
            dispatch.run();
        }

        return new BroadcastResponse(recipientSubs.size(), entities.size());
    }

    private Set<String> resolveRecipientUserIds(BroadcastRequest request) {
        Set<String> subs = new LinkedHashSet<>();
        for (String id : request.userIds()) {
            if (StringUtils.hasText(id)) {
                subs.add(id.trim());
            }
        }
        List<String> roles = request.realmRoleNames().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        if (!roles.isEmpty()) {
            subs.addAll(roleResolver.userIdsForRealmRoles(roles));
        }
        return subs;
    }

    @Transactional(readOnly = true)
    public Page<InAppNotificationResponse> listForUser(String userSub, Pageable pageable) {
        return repository.findByUserSubOrderByCreatedAtDesc(userSub, pageable)
                .map(InAppNotificationService::toResponse);
    }

    @Transactional
    public InAppNotificationResponse markRead(UUID id, String userSub) {
        InAppNotification row = repository.findByIdAndUserSub(id, userSub)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        if (row.getReadAt() == null) {
            row.setReadAt(Instant.now());
        }
        return toResponse(row);
    }

    private static InAppNotificationResponse toResponse(InAppNotification row) {
        return new InAppNotificationResponse(row.getId(), row.getTitle(), row.getBody(), row.getCreatedAt(), row.getReadAt());
    }
}
