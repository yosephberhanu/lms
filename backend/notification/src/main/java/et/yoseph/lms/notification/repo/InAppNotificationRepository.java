package et.yoseph.lms.notification.repo;

import et.yoseph.lms.notification.domain.InAppNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, UUID> {

    Page<InAppNotification> findByUserSubOrderByCreatedAtDesc(String userSub, Pageable pageable);

    Optional<InAppNotification> findByIdAndUserSub(UUID id, String userSub);
}
