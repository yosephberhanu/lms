package et.yoseph.lms.lease.service;

import et.yoseph.lms.lease.config.LeaseAttachmentsS3Config.LeaseAttachmentsS3Properties;
import et.yoseph.lms.lease.domain.Lease;
import et.yoseph.lms.lease.domain.LeaseAttachment;
import et.yoseph.lms.lease.domain.LeaseStatus;
import et.yoseph.lms.lease.repository.LeaseAttachmentRepository;
import et.yoseph.lms.lease.repository.LeaseRepository;
import et.yoseph.lms.lease.web.dto.LeaseAttachmentResponse;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import jakarta.annotation.PostConstruct;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
public class LeaseAttachmentService {

    private final LeaseRepository leaseRepository;
    private final LeaseAttachmentRepository attachmentRepository;
    private final MinioClient minio;
    private final LeaseAttachmentsS3Properties s3;

    public LeaseAttachmentService(
            LeaseRepository leaseRepository,
            LeaseAttachmentRepository attachmentRepository,
            MinioClient leaseAttachmentsMinioClient,
            LeaseAttachmentsS3Properties s3) {
        this.leaseRepository = leaseRepository;
        this.attachmentRepository = attachmentRepository;
        this.minio = leaseAttachmentsMinioClient;
        this.s3 = s3;
    }

    @PostConstruct
    void ensureBucketExists() {
        try {
            boolean exists = minio.bucketExists(BucketExistsArgs.builder().bucket(s3.bucket()).build());
            if (!exists) {
                minio.makeBucket(MakeBucketArgs.builder().bucket(s3.bucket()).build());
            }
            // Keep bucket private. (No public policy set.)
        } catch (Exception e) {
            // Surface as-is; if MinIO isn't reachable, lease API can still start but uploads will fail.
        }
    }

    @Transactional(readOnly = true)
    public List<LeaseAttachmentResponse> list(long leaseId, String ownerPartyId, Long tenantId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new NotFoundException("Lease not found: " + leaseId));
        assertCanRead(lease, ownerPartyId, tenantId);
        return attachmentRepository.findByLease_IdOrderByUploadedAtDesc(leaseId).stream()
                .map(LeaseAttachmentService::toResponse)
                .toList();
    }

    @Transactional
    public LeaseAttachmentResponse upload(long leaseId, String ownerPartyId, Long tenantId, MultipartFile file) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new NotFoundException("Lease not found: " + leaseId));
        assertManagerOnly(ownerPartyId, tenantId);
        if (lease.getStatus() != LeaseStatus.DRAFT && lease.getStatus() != LeaseStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Attachments can only be added while a lease is in DRAFT or PENDING_APPROVAL");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Missing file");
        }

        String original = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename().trim() : "attachment";
        String safeOriginal = original.replaceAll("[\\r\\n]", "_");
        String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        long size = file.getSize();

        String objectKey = "leases/" + leaseId + "/" + UUID.randomUUID() + "/" + safeOriginal;

        try (InputStream in = file.getInputStream()) {
            minio.putObject(
                    PutObjectArgs.builder()
                            .bucket(s3.bucket())
                            .object(objectKey)
                            .stream(in, size, -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            throw new BadRequestException("Unable to store attachment: " + e.getMessage());
        }

        LeaseAttachment row = new LeaseAttachment();
        row.setLease(lease);
        row.setObjectKey(objectKey);
        row.setOriginalFileName(safeOriginal);
        row.setContentType(contentType);
        row.setSizeBytes(size);
        LeaseAttachment saved = attachmentRepository.save(row);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public LeaseAttachmentDownload openForDownload(long leaseId, long attachmentId, String ownerPartyId, Long tenantId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new NotFoundException("Lease not found: " + leaseId));
        assertCanRead(lease, ownerPartyId, tenantId);
        LeaseAttachment row = attachmentRepository.findByIdAndLease_Id(attachmentId, leaseId)
                .orElseThrow(() -> new NotFoundException("Attachment not found: " + attachmentId));

        try {
            InputStream stream = minio.getObject(
                    GetObjectArgs.builder()
                            .bucket(s3.bucket())
                            .object(row.getObjectKey())
                            .build()
            );
            return new LeaseAttachmentDownload(row, stream);
        } catch (ErrorResponseException ex) {
            throw new NotFoundException("Attachment content not found in object storage");
        } catch (Exception e) {
            throw new BadRequestException("Unable to download attachment: " + e.getMessage());
        }
    }

    @Transactional
    public void delete(long leaseId, long attachmentId, String ownerPartyId, Long tenantId) {
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new NotFoundException("Lease not found: " + leaseId));
        assertManagerOnly(ownerPartyId, tenantId);
        if (lease.getStatus() != LeaseStatus.DRAFT && lease.getStatus() != LeaseStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Attachments can only be removed while a lease is in DRAFT or PENDING_APPROVAL");
        }
        LeaseAttachment row = attachmentRepository.findByIdAndLease_Id(attachmentId, leaseId)
                .orElseThrow(() -> new NotFoundException("Attachment not found: " + attachmentId));

        attachmentRepository.delete(row);
        try {
            minio.removeObject(RemoveObjectArgs.builder().bucket(s3.bucket()).object(row.getObjectKey()).build());
        } catch (Exception ignored) {
            // Metadata delete is authoritative; object delete best-effort.
        }
    }

    private static void assertManagerOnly(String ownerPartyId, Long tenantId) {
        if (StringUtils.hasText(ownerPartyId) || tenantId != null) {
            throw new ForbiddenException("Only managers can modify lease attachments");
        }
    }

    private static void assertCanRead(Lease lease, String ownerPartyId, Long tenantId) {
        if (tenantId != null) {
            if (!lease.getTenant().getId().equals(tenantId)) {
                throw new ForbiddenException("X-Tenant-Id does not match this lease's tenant");
            }
            return;
        }
        if (StringUtils.hasText(ownerPartyId)) {
            if (!StringUtils.hasText(lease.getOwnerId()) || !lease.getOwnerId().trim().equals(ownerPartyId.trim())) {
                throw new ForbiddenException("X-Owner-Party-Id does not match this lease's owner_id");
            }
            return;
        }
        // Manager view (no headers).
    }

    private static LeaseAttachmentResponse toResponse(LeaseAttachment a) {
        return new LeaseAttachmentResponse(
                a.getId(),
                a.getOriginalFileName(),
                a.getContentType(),
                a.getSizeBytes(),
                a.getUploadedAt()
        );
    }

    public record LeaseAttachmentDownload(LeaseAttachment attachment, InputStream stream) {
    }
}

