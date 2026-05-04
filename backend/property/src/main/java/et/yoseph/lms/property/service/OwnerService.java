package et.yoseph.lms.property.service;

import et.yoseph.lms.property.domain.Owner;
import et.yoseph.lms.property.repository.OwnerRepository;
import et.yoseph.lms.property.web.dto.OwnerResponse;
import et.yoseph.lms.property.web.dto.OwnerUpsertRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.List;

@Service
public class OwnerService {

    private final OwnerRepository ownerRepository;

    public OwnerService(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    @Transactional(readOnly = true)
    public List<OwnerResponse> list(String q, String partyId, String displayName, String email, String phone) {
        Specification<Owner> spec = (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (partyId != null && !partyId.isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.get("partyId"), partyId.trim()));
            }

            if (displayName != null && !displayName.isBlank()) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("displayName")), "%" + displayName.trim().toLowerCase() + "%"));
            }

            if (email != null && !email.isBlank()) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("email")), "%" + email.trim().toLowerCase() + "%"));
            }

            if (phone != null && !phone.isBlank()) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("phone")), "%" + phone.trim().toLowerCase() + "%"));
            }

            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                Predicate any = cb.or(
                        cb.like(cb.lower(root.get("partyId")), like),
                        cb.like(cb.lower(root.get("displayName")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("phone")), like)
                );
                predicate = cb.and(predicate, any);
            }

            return predicate;
        };

        return ownerRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(OwnerMapper::toOwnerResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OwnerResponse get(Long id) {
        Owner owner = ownerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Owner not found: " + id));
        return OwnerMapper.toOwnerResponse(owner);
    }

    @Transactional
    public OwnerResponse create(OwnerUpsertRequest request) {
        if (ownerRepository.existsByPartyId(request.partyId())) {
            throw new BadRequestException("Owner already exists for partyId: " + request.partyId());
        }
        Owner owner = new Owner();
        applyFields(owner, request);
        Owner saved = ownerRepository.save(owner);
        return OwnerMapper.toOwnerResponse(saved);
    }

    @Transactional
    public OwnerResponse update(Long id, OwnerUpsertRequest request) {
        Owner owner = ownerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Owner not found: " + id));
        if (!owner.getPartyId().equals(request.partyId())
                && ownerRepository.existsByPartyId(request.partyId())) {
            throw new BadRequestException("Owner already exists for partyId: " + request.partyId());
        }
        applyFields(owner, request);
        Owner saved = ownerRepository.save(owner);
        return OwnerMapper.toOwnerResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!ownerRepository.existsById(id)) {
            throw new NotFoundException("Owner not found: " + id);
        }
        ownerRepository.deleteById(id);
    }

    private static void applyFields(Owner owner, OwnerUpsertRequest request) {
        owner.setPartyId(request.partyId());
        owner.setDisplayName(request.displayName());
        owner.setEmail(request.email());
        owner.setPhone(request.phone());
    }
}

