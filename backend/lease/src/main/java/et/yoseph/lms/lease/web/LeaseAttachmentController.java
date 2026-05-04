package et.yoseph.lms.lease.web;

import et.yoseph.lms.lease.service.LeaseAttachmentService;
import et.yoseph.lms.lease.web.dto.LeaseAttachmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/leases/{leaseId}/attachments")
@Tag(name = "Lease Attachments")
public class LeaseAttachmentController {

    private final LeaseAttachmentService service;

    public LeaseAttachmentController(LeaseAttachmentService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List attachments for a lease")
    public List<LeaseAttachmentResponse> list(
            @PathVariable long leaseId,
            @RequestHeader(value = "X-Owner-Party-Id", required = false) String ownerPartyId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        return service.list(leaseId, ownerPartyId, tenantId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an attachment to a lease (DRAFT or PENDING_APPROVAL)")
    public LeaseAttachmentResponse upload(
            @PathVariable long leaseId,
            @RequestHeader(value = "X-Owner-Party-Id", required = false) String ownerPartyId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam("file") MultipartFile file) {
        return service.upload(leaseId, ownerPartyId, tenantId, file);
    }

    @GetMapping("/{attachmentId}")
    @Operation(summary = "Download an attachment")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable long leaseId,
            @PathVariable long attachmentId,
            @RequestHeader(value = "X-Owner-Party-Id", required = false) String ownerPartyId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        LeaseAttachmentService.LeaseAttachmentDownload dl = service.openForDownload(leaseId, attachmentId, ownerPartyId, tenantId);
        String filename = dl.attachment().getOriginalFileName();
        String contentType = dl.attachment().getContentType();

        String encoded = java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        String dispo = "attachment; filename=\"" + filename.replace("\"", "") + "\"; filename*=UTF-8''" + encoded;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, dispo)
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .body(new InputStreamResource(dl.stream()));
    }

    @DeleteMapping("/{attachmentId}")
    @Operation(summary = "Delete an attachment from a lease (DRAFT or PENDING_APPROVAL)")
    public void delete(
            @PathVariable long leaseId,
            @PathVariable long attachmentId,
            @RequestHeader(value = "X-Owner-Party-Id", required = false) String ownerPartyId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId) {
        service.delete(leaseId, attachmentId, ownerPartyId, tenantId);
    }
}

