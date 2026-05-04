package et.yoseph.lms.lease.service;

import et.yoseph.lms.lease.domain.Lease;
import et.yoseph.lms.lease.domain.LeaseStatusHistory;
import et.yoseph.lms.lease.web.dto.LeaseResponse;
import et.yoseph.lms.lease.web.dto.LeaseStatusHistoryResponse;

import java.util.Comparator;
import java.util.List;

final class LeaseMapper {

    private LeaseMapper() {
    }

    static LeaseResponse toResponse(Lease lease) {
        List<LeaseStatusHistoryResponse> history = lease.getStatusHistory().stream()
                .sorted(Comparator.comparing(LeaseStatusHistory::getId, Comparator.nullsLast(Long::compareTo)))
                .map(LeaseMapper::toHistoryResponse)
                .toList();

        Long tenantPk = lease.getTenant() != null ? lease.getTenant().getId() : null;

        return new LeaseResponse(
                lease.getId(),
                lease.getPropertyId(),
                tenantPk,
                TenantMapper.toResponse(lease.getTenant()),
                lease.getOwnerId(),
                lease.getMonthlyRent(),
                lease.getStartDate(),
                lease.getEndDate(),
                lease.getStatus(),
                lease.getDepositAmount(),
                lease.getPaymentSchedule(),
                lease.getPropertyNameSnapshot(),
                lease.getTenantNameSnapshot(),
                lease.getCreatedAt(),
                lease.getUpdatedAt(),
                lease.getOwnerApprovedAt(),
                lease.getTenantApprovedAt(),
                history
        );
    }

    private static LeaseStatusHistoryResponse toHistoryResponse(LeaseStatusHistory h) {
        return new LeaseStatusHistoryResponse(
                h.getId(),
                h.getOldStatus(),
                h.getNewStatus(),
                h.getChangedAt(),
                h.getChangedBy()
        );
    }
}
