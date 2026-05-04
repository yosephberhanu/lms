package et.yoseph.lms.payment.integration;

import et.yoseph.lms.payment.service.BadRequestException;
import et.yoseph.lms.payment.service.NotFoundException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Component
public class LeaseClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public LeaseClient(RestClient leaseServiceRestClient) {
        this.restClient = leaseServiceRestClient;
    }

    public LeaseSnapshot fetchLease(long leaseId) {
        try {
            Map<String, Object> root = restClient.get()
                    .uri("/api/v1/leases/{id}", leaseId)
                    .retrieve()
                    .body(MAP_TYPE);
            if (root == null) {
                throw new NotFoundException("Lease not found: " + leaseId);
            }

            long tenantId = asLong(root.get("tenantId"));
            long propertyId = asLong(root.get("propertyId"));
            BigDecimal monthlyRent = asBigDecimal(root.get("monthlyRent"));
            LocalDate startDate = LocalDate.parse(asString(root.get("startDate")));
            LocalDate endDate = LocalDate.parse(asString(root.get("endDate")));
            String propertyNameSnapshot = asNullableString(root.get("propertyNameSnapshot"));

            return new LeaseSnapshot(leaseId, tenantId, propertyId, propertyNameSnapshot, monthlyRent, startDate, endDate);
        } catch (HttpStatusCodeException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 404) {
                throw new NotFoundException("Lease not found: " + leaseId);
            }
            throw new BadRequestException("Lease service error: " + e.getMessage());
        }
    }

    private static String asString(Object v) {
        if (v == null) {
            throw new BadRequestException("Lease payload missing required field");
        }
        return v.toString();
    }

    private static String asNullableString(Object v) {
        return v != null ? v.toString() : null;
    }

    private static long asLong(Object v) {
        if (v instanceof Number n) {
            return n.longValue();
        }
        if (v == null) {
            throw new BadRequestException("Lease payload missing required field");
        }
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            throw new BadRequestException("Lease payload field is not a number");
        }
    }

    private static BigDecimal asBigDecimal(Object v) {
        if (v instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        if (v == null) {
            throw new BadRequestException("Lease payload missing required field");
        }
        try {
            return new BigDecimal(v.toString());
        } catch (NumberFormatException e) {
            throw new BadRequestException("Lease payload field is not a decimal");
        }
    }
}

