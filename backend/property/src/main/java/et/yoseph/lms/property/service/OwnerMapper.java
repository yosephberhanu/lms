package et.yoseph.lms.property.service;

import et.yoseph.lms.property.domain.Owner;
import et.yoseph.lms.property.web.dto.OwnerResponse;

final class OwnerMapper {

    private OwnerMapper() {
    }

    static OwnerResponse toOwnerResponse(Owner owner) {
        return new OwnerResponse(
                owner.getId(),
                owner.getPartyId(),
                owner.getDisplayName(),
                owner.getEmail(),
                owner.getPhone(),
                owner.getCreatedAt(),
                owner.getUpdatedAt()
        );
    }
}

