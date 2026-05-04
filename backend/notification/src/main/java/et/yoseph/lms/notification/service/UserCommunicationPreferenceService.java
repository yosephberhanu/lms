package et.yoseph.lms.notification.service;

import et.yoseph.lms.notification.domain.UserCommunicationPreference;
import et.yoseph.lms.notification.repo.UserCommunicationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;

@Service
public class UserCommunicationPreferenceService {

    private final UserCommunicationPreferenceRepository repository;

    public UserCommunicationPreferenceService(UserCommunicationPreferenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<UserCommunicationPreference> find(String userSub) {
        return repository.findByUserSub(userSub);
    }

    @Transactional
    public UserCommunicationPreference upsert(String userSub, boolean notifyEmail, boolean notifySms, String email, String phone) {
        UserCommunicationPreference row = repository.findByUserSub(userSub).orElseGet(UserCommunicationPreference::new);
        row.setUserSub(userSub);
        row.setNotifyEmail(notifyEmail);
        row.setNotifySms(notifySms);
        // Only change contact columns when the client supplied them (non-null after JSON bind).
        // Missing keys stay null in Java and must not wipe existing values; send "" to clear.
        if (email != null) {
            row.setEmail(StringUtils.hasText(email) ? email.trim() : null);
        }
        if (phone != null) {
            row.setPhone(StringUtils.hasText(phone) ? phone.trim() : null);
        }
        row.setUpdatedAt(Instant.now());
        return repository.save(row);
    }
}
