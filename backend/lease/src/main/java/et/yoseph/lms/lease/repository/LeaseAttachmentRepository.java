package et.yoseph.lms.lease.repository;

import et.yoseph.lms.lease.domain.LeaseAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaseAttachmentRepository extends JpaRepository<LeaseAttachment, Long> {

    List<LeaseAttachment> findByLease_IdOrderByUploadedAtDesc(Long leaseId);

    Optional<LeaseAttachment> findByIdAndLease_Id(Long id, Long leaseId);
}

