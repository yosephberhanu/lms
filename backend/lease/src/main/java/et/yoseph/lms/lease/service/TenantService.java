package et.yoseph.lms.lease.service;

import et.yoseph.lms.lease.domain.Tenant;
import et.yoseph.lms.lease.repository.LeaseRepository;
import et.yoseph.lms.lease.repository.TenantRepository;
import et.yoseph.lms.lease.web.dto.TenantResponse;
import et.yoseph.lms.lease.web.dto.TenantUpsertRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class TenantService {

       private final TenantRepository tenantRepository;
    private final LeaseRepository leaseRepository;

    public TenantService(TenantRepository tenantRepository, LeaseRepository leaseRepository) {
        this.tenantRepository = tenantRepository;
        this.leaseRepository = leaseRepository;
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> list(String q, String externalPartyId, String displayName, String email, String phone) {
        Specification<Tenant> spec = (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (StringUtils.hasText(externalPartyId)) {
                predicate = cb.and(predicate, cb.equal(root.get("externalPartyId"), externalPartyId.trim()));
            }

            if (StringUtils.hasText(displayName)) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("displayName")), "%" + displayName.trim().toLowerCase() + "%"));
            }

            if (StringUtils.hasText(email)) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("email")), "%" + email.trim().toLowerCase() + "%"));
            }

            if (StringUtils.hasText(phone)) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("phone")), "%" + phone.trim().toLowerCase() + "%"));
            }

            if (StringUtils.hasText(q)) {
                String like = "%" + q.trim().toLowerCase() + "%";
                Predicate any = cb.or(
                        cb.like(cb.lower(root.get("displayName")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("email"), cb.literal(""))), like),
                        cb.like(cb.lower(cb.coalesce(root.get("phone"), cb.literal(""))), like),
                        cb.like(cb.lower(cb.coalesce(root.get("externalPartyId"), cb.literal(""))), like)
                );
                predicate = cb.and(predicate, any);
            }

            return predicate;
        };

        return tenantRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(TenantMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TenantResponse get(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + id));
        return TenantMapper.toResponse(tenant);
    }

    @Transactional
    public TenantResponse create(TenantUpsertRequest request) {
        assertExternalPartyIdAvailable(request.externalPartyId(), null);
        Tenant tenant = new Tenant();
        applyFields(tenant, request);
        Tenant saved = tenantRepository.save(tenant);
        return TenantMapper.toResponse(saved);
    }

    @Transactional
    public TenantResponse update(Long id, TenantUpsertRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + id));
        assertExternalPartyIdAvailable(request.externalPartyId(), id);
        applyFields(tenant, request);
        Tenant saved = tenantRepository.save(tenant);
        return TenantMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!tenantRepository.existsById(id)) {
            throw new NotFoundException("Tenant not found: " + id);
        }
        if (leaseRepository.countByTenant_Id(id) > 0) {
            throw new BadRequestException("Cannot delete tenant referenced by one or more leases: " + id);
        }
        tenantRepository.deleteById(id);
    }

    private void assertExternalPartyIdAvailable(String externalPartyId, Long excludeTenantId) {
        if (!StringUtils.hasText(externalPartyId)) {
            return;
        }
        String trimmed = externalPartyId.trim();
        tenantRepository.findByExternalPartyId(trimmed).ifPresent(existing -> {
            if (excludeTenantId == null || !existing.getId().equals(excludeTenantId)) {
                throw new BadRequestException("Tenant already exists for externalPartyId: " + trimmed);
            }
        });
    }

    private static void applyFields(Tenant tenant, TenantUpsertRequest request) {
        tenant.setExternalPartyId(StringUtils.hasText(request.externalPartyId()) ? request.externalPartyId().trim() : null);
        tenant.setDisplayName(request.displayName());
        tenant.setEmail(request.email());
        tenant.setPhone(request.phone());
        tenant.setStatus(request.status());
    }
}
