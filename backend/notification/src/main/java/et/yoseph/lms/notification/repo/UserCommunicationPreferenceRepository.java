package et.yoseph.lms.notification.repo;

import et.yoseph.lms.notification.domain.UserCommunicationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCommunicationPreferenceRepository extends JpaRepository<UserCommunicationPreference, Long> {
    Optional<UserCommunicationPreference> findByUserSub(String userSub);
}
