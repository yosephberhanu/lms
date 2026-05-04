package et.yoseph.lms.lease.service;

import et.yoseph.lms.lease.domain.Tenant;
import et.yoseph.lms.lease.web.dto.TenantResponse;

final class TenantMapper {

    private TenantMapper() {
    }

    static TenantResponse toResponse(Tenant tenant) {
        if (tenant == null) {
            return null;
        }
        return new TenantResponse(
                tenant.getId(),
                tenant.getExternalPartyId(),
                tenant.getDisplayName(),
                tenant.getEmail(),
                tenant.getPhone(),
                tenant.getStatus(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }
}
