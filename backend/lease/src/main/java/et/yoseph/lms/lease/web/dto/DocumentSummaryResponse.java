package et.yoseph.lms.lease.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Placeholder until Document Service is integrated.
 */
@Schema(name = "LeaseDocumentsResponse")
public record DocumentSummaryResponse(List<Object> items) {
}
