package et.yoseph.lms.notification.web;

import et.yoseph.lms.notification.service.InAppNotificationService;
import et.yoseph.lms.notification.service.NotificationSseRegistry;
import et.yoseph.lms.notification.web.dto.InAppNotificationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/notification/v1")
public class UserNotificationController {

    private final InAppNotificationService notificationService;
    private final NotificationSseRegistry sseRegistry;
    private final boolean securityEnabled;

    public UserNotificationController(
            InAppNotificationService notificationService,
            NotificationSseRegistry sseRegistry,
            @Value("${lms.security.enabled:false}") boolean securityEnabled) {
        this.notificationService = notificationService;
        this.sseRegistry = sseRegistry;
        this.securityEnabled = securityEnabled;
    }

    @GetMapping("/messages")
    public Page<InAppNotificationResponse> list(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        // Harden pageable params: UI may accidentally send JSON-ish sort or huge numbers.
        int page = Math.max(0, pageable.getPageNumber());
        int size = pageable.getPageSize();
        if (size <= 0) size = 20;
        size = Math.min(size, 100);
        // Prevent offset overflow inside Spring Data JPA (offset is computed as page*size).
        long offset = (long) page * (long) size;
        if (offset > Integer.MAX_VALUE) {
            page = 0;
        }

        // Repository query method already defines ORDER BY createdAt DESC.
        Pageable safePageable = PageRequest.of(page, size);
        return notificationService.listForUser(subject(authentication), safePageable);
    }

    @PatchMapping("/messages/{id}/read")
    public InAppNotificationResponse markRead(Authentication authentication, @PathVariable UUID id) {
        return notificationService.markRead(id, subject(authentication));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication authentication) {
        return sseRegistry.subscribe(subject(authentication));
    }

    private String subject(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            // Use username as the canonical user key (matches how broadcasts are resolved and how UIs identify users).
            String username = jwt.getClaimAsString("preferred_username");
            if (username != null && !username.isBlank()) return username;

            return "dev-anonymous";
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
}
