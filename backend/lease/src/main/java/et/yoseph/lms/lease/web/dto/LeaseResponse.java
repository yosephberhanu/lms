package et.yoseph.lms.lease.web.dto;

import et.yoseph.lms.lease.domain.LeaseStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Schema(name = "LeaseResponse")
public record LeaseResponse(
        Long id,
        Long propertyId,
        Long tenantId,
        TenantResponse tenant,
        String ownerId,
        BigDecimal monthlyRent,
        LocalDate startDate,
        LocalDate endDate,
        LeaseStatus status,
        BigDecimal depositAmount,
        String paymentSchedule,
        String propertyNameSnapshot,
        String tenantNameSnapshot,
        Instant createdAt,
        Instant updatedAt,
        Instant ownerApprovedAt,
        Instant tenantApprovedAt,
        List<LeaseStatusHistoryResponse> statusHistory
) {
}
