package et.yoseph.lms.property.service;

import et.yoseph.lms.property.domain.Property;
import et.yoseph.lms.property.domain.PropertyOwnership;
import et.yoseph.lms.property.web.dto.OwnershipResponse;
import et.yoseph.lms.property.web.dto.PropertyResponse;

import java.util.Comparator;
import java.util.List;

final class PropertyMapper {

    private PropertyMapper() {
    }

    static OwnershipResponse toOwnershipResponse(PropertyOwnership o) {
        return new OwnershipResponse(
                o.getId(),
                o.getOwnerPartyId(),
                o.getRole(),
                o.getOwnershipPercentage(),
                o.getEffectiveFrom(),
                o.getEffectiveTo(),
                o.getNotes()
        );
    }

    static PropertyResponse toPropertyResponse(Property p) {
        List<OwnershipResponse> ownerships = p.getOwnerships().stream()
                .sorted(Comparator.comparing(PropertyOwnership::getId, Comparator.nullsLast(Long::compareTo)))
                .map(PropertyMapper::toOwnershipResponse)
                .toList();
        return new PropertyResponse(
                p.getId(),
                p.getName(),
                p.getAddressLine1(),
                p.getAddressLine2(),
                p.getCity(),
                p.getStateOrProvince(),
                p.getPostalCode(),
                p.getCountry(),
                p.getPropertyType(),
                p.getDescription(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                ownerships
        );
    }
}
