package et.yoseph.lms.property.service;

import et.yoseph.lms.property.domain.PropertyOwnership;
import et.yoseph.lms.property.web.dto.OwnershipUpsertRequest;

final class OwnershipRequestSupport {

    private OwnershipRequestSupport() {
    }

    static void validateOwnershipDates(OwnershipUpsertRequest o) {
        if (o.effectiveTo() != null && o.effectiveTo().isBefore(o.effectiveFrom())) {
            throw new BadRequestException("effectiveTo must be on or after effectiveFrom");
        }
    }

    static PropertyOwnership toOwnershipEntity(OwnershipUpsertRequest o) {
        PropertyOwnership entity = new PropertyOwnership();
        entity.setOwnerPartyId(o.ownerPartyId());
        entity.setRole(o.role());
        entity.setOwnershipPercentage(o.ownershipPercentage());
        entity.setEffectiveFrom(o.effectiveFrom());
        entity.setEffectiveTo(o.effectiveTo());
        entity.setNotes(o.notes());
        return entity;
    }
}
