package et.yoseph.lms.notification.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BroadcastRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 4000) String body,
        List<String> userIds,
        List<String> realmRoleNames) {

    public BroadcastRequest {
        userIds = userIds == null ? List.of() : List.copyOf(userIds);
        realmRoleNames = realmRoleNames == null ? List.of() : List.copyOf(realmRoleNames);
    }
}
