package et.yoseph.lms.property.service;

import et.yoseph.lms.property.domain.Property;
import et.yoseph.lms.property.domain.PropertyOwnership;
import et.yoseph.lms.property.repository.PropertyOwnershipRepository;
import et.yoseph.lms.property.repository.PropertyRepository;
import et.yoseph.lms.property.web.dto.OwnershipResponse;
import et.yoseph.lms.property.web.dto.OwnershipUpsertRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PropertyOwnershipService {

    private final PropertyRepository propertyRepository;
    private final PropertyOwnershipRepository ownershipRepository;

    public PropertyOwnershipService(
            PropertyRepository propertyRepository,
            PropertyOwnershipRepository ownershipRepository) {
        this.propertyRepository = propertyRepository;
        this.ownershipRepository = ownershipRepository;
    }

    @Transactional(readOnly = true)
    public List<OwnershipResponse> listForProperty(Long propertyId) {
        ensurePropertyExists(propertyId);
        return ownershipRepository.findByPropertyIdOrderByIdAsc(propertyId).stream()
                .map(PropertyMapper::toOwnershipResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OwnershipResponse get(Long propertyId, Long ownershipId) {
        PropertyOwnership o = ownershipRepository.findByPropertyIdAndId(propertyId, ownershipId)
                .orElseThrow(() -> new NotFoundException(
                        "Ownership not found: propertyId=" + propertyId + ", ownershipId=" + ownershipId));
        return PropertyMapper.toOwnershipResponse(o);
    }

    @Transactional
    public OwnershipResponse create(Long propertyId, OwnershipUpsertRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new NotFoundException("Property not found: " + propertyId));
        OwnershipRequestSupport.validateOwnershipDates(request);
        PropertyOwnership entity = OwnershipRequestSupport.toOwnershipEntity(request);
        property.addOwnership(entity);
        propertyRepository.save(property);
        return PropertyMapper.toOwnershipResponse(entity);
    }

    @Transactional
    public OwnershipResponse update(Long propertyId, Long ownershipId, OwnershipUpsertRequest request) {
        PropertyOwnership entity = ownershipRepository.findByPropertyIdAndId(propertyId, ownershipId)
                .orElseThrow(() -> new NotFoundException(
                        "Ownership not found: propertyId=" + propertyId + ", ownershipId=" + ownershipId));
        OwnershipRequestSupport.validateOwnershipDates(request);
        entity.setOwnerPartyId(request.ownerPartyId());
        entity.setRole(request.role());
        entity.setOwnershipPercentage(request.ownershipPercentage());
        entity.setEffectiveFrom(request.effectiveFrom());
        entity.setEffectiveTo(request.effectiveTo());
        entity.setNotes(request.notes());
        ownershipRepository.save(entity);
        return PropertyMapper.toOwnershipResponse(entity);
    }

    @Transactional
    public void delete(Long propertyId, Long ownershipId) {
        PropertyOwnership entity = ownershipRepository.findByPropertyIdAndId(propertyId, ownershipId)
                .orElseThrow(() -> new NotFoundException(
                        "Ownership not found: propertyId=" + propertyId + ", ownershipId=" + ownershipId));
        ownershipRepository.delete(entity);
    }

    private void ensurePropertyExists(Long propertyId) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new NotFoundException("Property not found: " + propertyId);
        }
    }
}
