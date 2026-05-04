package et.yoseph.lms.notification.web;

import et.yoseph.lms.notification.domain.UserCommunicationPreference;
import et.yoseph.lms.notification.service.UserCommunicationPreferenceService;
import et.yoseph.lms.notification.web.dto.UserCommunicationPreferenceResponse;
import et.yoseph.lms.notification.web.dto.UserCommunicationPreferenceUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notification/v1/me")
public class UserCommunicationPreferenceController {

    private final UserCommunicationPreferenceService service;

    public UserCommunicationPreferenceController(UserCommunicationPreferenceService service) {
        this.service = service;
    }

    @GetMapping("/communication-preferences")
    public UserCommunicationPreferenceResponse get(Authentication authentication) {
        String sub = subject(authentication);
        return service.find(sub)
                .map(r -> new UserCommunicationPreferenceResponse(r.isNotifyEmail(), r.isNotifySms(), r.getEmail(), r.getPhone()))
                .orElseGet(() -> new UserCommunicationPreferenceResponse(false, false, null, null));
    }

    @PutMapping("/communication-preferences")
    public UserCommunicationPreferenceResponse put(Authentication authentication, @RequestBody UserCommunicationPreferenceUpdateRequest body) {
        String sub = subject(authentication);
        UserCommunicationPreference saved = service.upsert(sub, body.notifyEmail(), body.notifySms(), body.email(), body.phone());
        return new UserCommunicationPreferenceResponse(saved.isNotifyEmail(), saved.isNotifySms(), saved.getEmail(), saved.getPhone());
    }

    private String subject(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String username = jwt.getClaimAsString("preferred_username");
            if (username != null && !username.isBlank()) return username;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
}
