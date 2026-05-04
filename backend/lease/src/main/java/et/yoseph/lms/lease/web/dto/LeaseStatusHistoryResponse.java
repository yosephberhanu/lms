package et.yoseph.lms.lease.web.dto;

import et.yoseph.lms.lease.domain.LeaseStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "LeaseStatusHistoryResponse")
public record LeaseStatusHistoryResponse(
        Long id,
        LeaseStatus oldStatus,
        LeaseStatus newStatus,
        Instant changedAt,
        String changedBy
) {
}
