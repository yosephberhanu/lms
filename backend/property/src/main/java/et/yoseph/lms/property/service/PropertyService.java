package et.yoseph.lms.property.service;

import et.yoseph.lms.property.domain.Property;
import et.yoseph.lms.property.domain.PropertyType;
import et.yoseph.lms.property.repository.PropertyRepository;
import et.yoseph.lms.property.web.dto.OwnershipUpsertRequest;
import et.yoseph.lms.property.web.dto.PropertyResponse;
import et.yoseph.lms.property.web.dto.PropertyUpsertRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;

    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Transactional(readOnly = true)
    public List<PropertyResponse> list(String q, String city, String country, PropertyType propertyType, String ownerPartyId) {
        String qOrEmpty = toLowerOrEmpty(q);
        String cityOrEmpty = toLowerOrEmpty(city);
        String countryOrEmpty = toLowerOrEmpty(country);
        String ownerPartyIdOrEmpty = ownerPartyIdOrEmpty(ownerPartyId);

        return propertyRepository.searchWithOwnerships(qOrEmpty, cityOrEmpty, countryOrEmpty, propertyType, ownerPartyIdOrEmpty)
                .stream()
                .map(PropertyMapper::toPropertyResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PropertyResponse get(Long id) {
        Property property = propertyRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Property not found: " + id));
        return PropertyMapper.toPropertyResponse(property);
    }

    @Transactional
    public PropertyResponse create(PropertyUpsertRequest request) {
        Property property = new Property();
        applyPropertyFields(property, request);
        if (request.ownerships() != null) {
            for (OwnershipUpsertRequest o : request.ownerships()) {
                OwnershipRequestSupport.validateOwnershipDates(o);
                property.addOwnership(OwnershipRequestSupport.toOwnershipEntity(o));
            }
        }
        Property saved = propertyRepository.save(property);
        return PropertyMapper.toPropertyResponse(
                propertyRepository.findDetailById(saved.getId()).orElse(saved));
    }

    @Transactional
    public PropertyResponse update(Long id, PropertyUpsertRequest request) {
        Property property = propertyRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Property not found: " + id));
        applyPropertyFields(property, request);
        property.getOwnerships().clear();
        if (request.ownerships() != null) {
            for (OwnershipUpsertRequest o : request.ownerships()) {
                OwnershipRequestSupport.validateOwnershipDates(o);
                property.addOwnership(OwnershipRequestSupport.toOwnershipEntity(o));
            }
        }
        Property saved = propertyRepository.save(property);
        return PropertyMapper.toPropertyResponse(
                propertyRepository.findDetailById(saved.getId()).orElse(saved));
    }

    @Transactional
    public void delete(Long id) {
        if (!propertyRepository.existsById(id)) {
            throw new NotFoundException("Property not found: " + id);
        }
        propertyRepository.deleteById(id);
    }

    private static void applyPropertyFields(Property property, PropertyUpsertRequest request) {
        property.setName(request.name());
        property.setAddressLine1(request.addressLine1());
        property.setAddressLine2(request.addressLine2());
        property.setCity(request.city());
        property.setStateOrProvince(request.stateOrProvince());
        property.setPostalCode(request.postalCode());
        property.setCountry(request.country());
        property.setPropertyType(request.propertyType());
        property.setDescription(request.description());
    }

    private static String toLowerOrEmpty(String s) {
        if (s == null) {
            return "";
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? "" : trimmed.toLowerCase();
    }

    /** Exact match for ownership party id (case-sensitive); empty disables the filter. */
    private static String ownerPartyIdOrEmpty(String ownerPartyId) {
        if (ownerPartyId == null) {
            return "";
        }
        String trimmed = ownerPartyId.trim();
        return trimmed.isEmpty() ? "" : trimmed;
    }
}
