package et.yoseph.lms.lease.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "leases")
public class Lease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "owner_id", length = 128)
    private String ownerId;

    @Column(name = "monthly_rent", nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyRent;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LeaseStatus status;

    @Column(name = "deposit_amount", precision = 14, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "payment_schedule", length = 255)
    private String paymentSchedule;

    /** Intentional snapshot for research / denormalization. */
    @Column(name = "property_name_snapshot", length = 512)
    private String propertyNameSnapshot;

    /** Intentional snapshot for research / denormalization. */
    @Column(name = "tenant_name_snapshot", length = 512)
    private String tenantNameSnapshot;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Set when the designated owner approves while status is {@link LeaseStatus#PENDING_APPROVAL}. */
    @Column(name = "owner_approved_at")
    private Instant ownerApprovedAt;

    /** Set when the tenant approves while status is {@link LeaseStatus#PENDING_APPROVAL}. */
    @Column(name = "tenant_approved_at")
    private Instant tenantApprovedAt;

    @OneToMany(mappedBy = "lease", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeaseStatusHistory> statusHistory = new ArrayList<>();

    @OneToMany(mappedBy = "lease", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeaseAttachment> attachments = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(BigDecimal monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LeaseStatus getStatus() {
        return status;
    }

    public void setStatus(LeaseStatus status) {
        this.status = status;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public String getPaymentSchedule() {
        return paymentSchedule;
    }

    public void setPaymentSchedule(String paymentSchedule) {
        this.paymentSchedule = paymentSchedule;
    }

    public String getPropertyNameSnapshot() {
        return propertyNameSnapshot;
    }

    public void setPropertyNameSnapshot(String propertyNameSnapshot) {
        this.propertyNameSnapshot = propertyNameSnapshot;
    }

    public String getTenantNameSnapshot() {
        return tenantNameSnapshot;
    }

    public void setTenantNameSnapshot(String tenantNameSnapshot) {
        this.tenantNameSnapshot = tenantNameSnapshot;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getOwnerApprovedAt() {
        return ownerApprovedAt;
    }

    public void setOwnerApprovedAt(Instant ownerApprovedAt) {
        this.ownerApprovedAt = ownerApprovedAt;
    }

    public Instant getTenantApprovedAt() {
        return tenantApprovedAt;
    }

    public void setTenantApprovedAt(Instant tenantApprovedAt) {
        this.tenantApprovedAt = tenantApprovedAt;
    }

    public List<LeaseStatusHistory> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(List<LeaseStatusHistory> statusHistory) {
        this.statusHistory = statusHistory;
    }

    public List<LeaseAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<LeaseAttachment> attachments) {
        this.attachments = attachments;
    }

    public void addStatusHistory(LeaseStatus oldStatus, LeaseStatus newStatus, String changedBy) {
        LeaseStatusHistory row = new LeaseStatusHistory();
        row.setLease(this);
        row.setOldStatus(oldStatus);
        row.setNewStatus(newStatus);
        row.setChangedAt(Instant.now());
        row.setChangedBy(changedBy);
        statusHistory.add(row);
    }
}
