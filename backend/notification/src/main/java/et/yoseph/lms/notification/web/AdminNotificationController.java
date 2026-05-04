package et.yoseph.lms.notification.web;

import et.yoseph.lms.notification.service.InAppNotificationService;
import et.yoseph.lms.notification.web.dto.BroadcastRequest;
import et.yoseph.lms.notification.web.dto.BroadcastResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification/v1/admin")
public class AdminNotificationController {

    private final InAppNotificationService notificationService;

    public AdminNotificationController(InAppNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/broadcast")
    public BroadcastResponse broadcast(@RequestBody @Valid BroadcastRequest request) {
        return notificationService.broadcast(request);
    }
}
