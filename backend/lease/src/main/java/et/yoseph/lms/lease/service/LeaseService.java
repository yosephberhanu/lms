package et.yoseph.lms.lease.service;

import et.yoseph.lms.lease.domain.Lease;
import et.yoseph.lms.lease.domain.LeaseStatus;
import et.yoseph.lms.lease.domain.Tenant;
import et.yoseph.lms.lease.events.LeaseCreatedEvent;
import et.yoseph.lms.lease.integration.PropertyClient;
import et.yoseph.lms.lease.integration.UserClient;
import et.yoseph.lms.lease.repository.LeaseRepository;
import et.yoseph.lms.lease.repository.TenantRepository;
import et.yoseph.lms.lease.web.dto.DocumentSummaryResponse;
import et.yoseph.lms.lease.web.dto.LeaseResponse;
import et.yoseph.lms.lease.web.dto.LeaseWriteRequest;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class LeaseService {

    private final LeaseRepository leaseRepository;
    private final TenantRepository tenantRepository;
    private final PropertyClient propertyClient;
    private final UserClient userClient;
    private final ApplicationEventPublisher eventPublisher;

    public LeaseService(
            LeaseRepository leaseRepository,
            TenantRepository tenantRepository,
            PropertyClient propertyClient,
            UserClient userClient,
            ApplicationEventPublisher eventPublisher) {
        this.leaseRepository = leaseRepository;
        this.tenantRepository = tenantRepository;
        this.propertyClient = propertyClient;
        this.userClient = userClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<LeaseResponse> list(
            Long propertyId,
            Long tenantId,
            LeaseStatus status,
            String ownerId,
            String propertyOwnerPartyId) {
        final List<Long> ownedPropertyIds;
        if (StringUtils.hasText(propertyOwnerPartyId)) {
            ownedPropertyIds = propertyClient.fetchPropertyIdsForOwnerParty(propertyOwnerPartyId.trim());
        } else {
            ownedPropertyIds = null;
        }

        Specification<Lease> spec = (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            if (propertyId != null) {
                parts.add(cb.equal(root.get("propertyId"), propertyId));
            }
            if (tenantId != null) {
                Join<Lease, Tenant> tenant = root.join("tenant");
                parts.add(cb.equal(tenant.get("id"), tenantId));
            }
            if (status != null) {
                parts.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(ownerId)) {
                parts.add(cb.equal(root.get("ownerId"), ownerId.trim()));
            }
            if (ownedPropertyIds != null) {
                if (ownedPropertyIds.isEmpty()) {
                    parts.add(cb.disjunction());
                } else {
                    parts.add(root.get("propertyId").in(ownedPropertyIds));
                }
            }
            if (parts.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(parts.toArray(Predicate[]::new));
        };
        return leaseRepository.findAll(spec).stream()
                .map(LeaseMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LeaseResponse get(long id) {
        Lease lease = leaseRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Lease not found: " + id));
        return LeaseMapper.toResponse(lease);
    }

    @Transactional
    public LeaseResponse create(LeaseWriteRequest request) {
        validateDates(request);
        String propertyName = propertyClient.fetchPropertyName(request.propertyId());
        Tenant tenant = loadTenant(request.tenantId());
        validateTenantWithUserService(tenant);
        String tenantSnapshot = resolveTenantSnapshotName(tenant);

        Lease lease = new Lease();
        applyWrite(lease, request, tenant);
        lease.setPropertyNameSnapshot(propertyName);
        lease.setTenantNameSnapshot(tenantSnapshot);
        lease.setStatus(LeaseStatus.DRAFT);
        lease.addStatusHistory(null, LeaseStatus.DRAFT, "lease-api");

        Lease saved = leaseRepository.save(lease);

        eventPublisher.publishEvent(new LeaseCreatedEvent(
                saved.getId(),
                saved.getPropertyId(),
                saved.getTenant().getId(),
                saved.getMonthlyRent(),
                saved.getStartDate(),
                saved.getEndDate(),
                saved.getCreatedAt()));

        return LeaseMapper.toResponse(leaseRepository.findDetailById(saved.getId()).orElse(saved));
    }

    @Transactional
    public LeaseResponse update(long id, LeaseWriteRequest request) {
        Lease lease = leaseRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Lease not found: " + id));
        if (lease.getStatus() != LeaseStatus.DRAFT && lease.getStatus() != LeaseStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Lease can only be updated in DRAFT or PENDING_APPROVAL status");
        }
        validateDates(request);
        boolean propertyChanged = !lease.getPropertyId().equals(request.propertyId());
        Tenant newTenant = loadTenant(request.tenantId());
        boolean tenantChanged = !lease.getTenant().getId().equals(newTenant.getId());
        if (propertyChanged) {
            lease.setPropertyNameSnapshot(propertyClient.fetchPropertyName(request.propertyId()));
        }
        validateTenantWithUserService(newTenant);
        applyWrite(lease, request, newTenant);
        if (tenantChanged) {
            lease.setTenantNameSnapshot(resolveTenantSnapshotName(newTenant));
        }
        if (lease.getStatus() == LeaseStatus.PENDING_APPROVAL) {
            lease.setOwnerApprovedAt(null);
            lease.setTenantApprovedAt(null);
        }
        leaseRepository.save(lease);
        return LeaseMapper.toResponse(leaseRepository.findDetailById(id).orElse(lease));
    }

    @Transactional
    public LeaseResponse submitForApproval(long id) {
        Lease lease = leaseRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Lease not found: " + id));
        if (lease.getStatus() != LeaseStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT leases can be submitted for approval");
        }
        LeaseStatus old = lease.getStatus();
        lease.setStatus(LeaseStatus.PENDING_APPROVAL);
        lease.setOwnerApprovedAt(null);
        lease.setTenantApprovedAt(null);
        lease.addStatusHistory(old, LeaseStatus.PENDING_APPROVAL, "lease-api");
        leaseRepository.save(lease);
        return LeaseMapper.toResponse(leaseRepository.findDetailById(id).orElse(lease));
    }

    /**
     * Records owner-side approval. When both owner and tenant have approved, the lease becomes {@link LeaseStatus#ACTIVE}.
     */
    @Transactional
    public LeaseResponse approveAsOwner(long id, String actingOwnerPartyId) {
        Lease lease = leaseRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Lease not found: " + id));
        if (lease.getStatus() != LeaseStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Owner approval is only allowed when the lease is pending approval");
        }
        if (!StringUtils.hasText(actingOwnerPartyId)) {
            throw new BadRequestException("Missing owner party id (X-Owner-Party-Id)");
        }
        if (!StringUtils.hasText(lease.getOwnerId())) {
            throw new BadRequestException("Lease has no owner_id; set the lease owner before owner approval");
        }
        if (!lease.getOwnerId().trim().equals(actingOwnerPartyId.trim())) {
            throw new ForbiddenException("X-Owner-Party-Id does not match this lease's owner_id");
        }
        if (lease.getOwnerApprovedAt() != null) {
            throw new BadRequestException("Owner approval has already been recorded");
        }
        lease.setOwnerApprovedAt(Instant.now());
        maybeCompleteDualApproval(lease);
        leaseRepository.save(lease);
        return LeaseMapper.toResponse(leaseRepository.findDetailById(id).orElse(lease));
    }

    /**
     * Records tenant-side approval. When both owner and tenant have approved, the lease becomes {@link LeaseStatus#ACTIVE}.
     */
    @Transactional
    public LeaseResponse approveAsTenant(long id, long actingTenantId) {
        Lease lease = leaseRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Lease not found: " + id));
        if (lease.getStatus() != LeaseStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Tenant approval is only allowed when the lease is pending approval");
        }
        if (!lease.getTenant().getId().equals(actingTenantId)) {
            throw new ForbiddenException("X-Tenant-Id does not match this lease's tenant");
        }
        if (lease.getTenantApprovedAt() != null) {
            throw new BadRequestException("Tenant approval has already been recorded");
        }
        lease.setTenantApprovedAt(Instant.now());
        maybeCompleteDualApproval(lease);
        leaseRepository.save(lease);
        return LeaseMapper.toResponse(leaseRepository.findDetailById(id).orElse(lease));
    }

    private static void maybeCompleteDualApproval(Lease lease) {
        if (lease.getStatus() != LeaseStatus.PENDING_APPROVAL) {
            return;
        }
        if (lease.getOwnerApprovedAt() != null && lease.getTenantApprovedAt() != null) {
            lease.addStatusHistory(LeaseStatus.PENDING_APPROVAL, LeaseStatus.ACTIVE, "lease-api");
            lease.setStatus(LeaseStatus.ACTIVE);
        }
    }

    @Transactional
    public LeaseResponse terminate(long id) {
        Lease lease = leaseRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Lease not found: " + id));
        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new BadRequestException("Only ACTIVE leases can be terminated");
        }
        lease.setStatus(LeaseStatus.TERMINATED);
        lease.addStatusHistory(LeaseStatus.ACTIVE, LeaseStatus.TERMINATED, "lease-api");
        leaseRepository.save(lease);
        return LeaseMapper.toResponse(leaseRepository.findDetailById(id).orElse(lease));
    }

    @Transactional(readOnly = true)
    public DocumentSummaryResponse documents(long leaseId) {
        ensureLeaseExists(leaseId);
        return new DocumentSummaryResponse(List.of());
    }

    private void ensureLeaseExists(long leaseId) {
        if (!leaseRepository.existsById(leaseId)) {
            throw new NotFoundException("Lease not found: " + leaseId);
        }
    }

    private Tenant loadTenant(long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + tenantId));
    }

    private void validateTenantWithUserService(Tenant tenant) {
        if (StringUtils.hasText(tenant.getExternalPartyId())) {
            userClient.assertTenantExists(tenant.getExternalPartyId());
        }
    }

    private String resolveTenantSnapshotName(Tenant tenant) {
        if (StringUtils.hasText(tenant.getExternalPartyId())) {
            String fromUser = userClient.fetchTenantDisplayName(tenant.getExternalPartyId());
            if (StringUtils.hasText(fromUser)) {
                return fromUser;
            }
        }
        return tenant.getDisplayName();
    }

    private static void validateDates(LeaseWriteRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("endDate must not be before startDate");
        }
    }

    private static void applyWrite(Lease lease, LeaseWriteRequest request, Tenant tenant) {
        lease.setPropertyId(request.propertyId());
        lease.setTenant(tenant);
        lease.setOwnerId(request.ownerId());
        lease.setMonthlyRent(request.monthlyRent());
        lease.setStartDate(request.startDate());
        lease.setEndDate(request.endDate());
        lease.setDepositAmount(request.depositAmount());
        lease.setPaymentSchedule(request.paymentSchedule());
    }
}
