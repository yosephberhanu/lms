package et.yoseph.lms.lease.integration;

import et.yoseph.lms.lease.service.BadRequestException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class PaymentClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public PaymentClient(RestClient paymentServiceRestClient) {
        this.restClient = paymentServiceRestClient;
    }

    public Map<String, Object> fetchPayments(long leaseId, long tenantId, String bearerToken) {
        try {
            return restClient.get()
                    .uri("/api/v1/leases/{id}/payments", leaseId)
                    .header("X-Tenant-Id", String.valueOf(tenantId))
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .body(MAP_TYPE);
        } catch (HttpStatusCodeException e) {
            throw mapError(e);
        }
    }

    public Map<String, Object> createCheckoutSession(long leaseId, long tenantId, String bearerToken, Map<String, Object> body) {
        try {
            return restClient.post()
                    .uri("/api/v1/leases/{id}/payments/checkout-session", leaseId)
                    .header("X-Tenant-Id", String.valueOf(tenantId))
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .body(body)
                    .retrieve()
                    .body(MAP_TYPE);
        } catch (HttpStatusCodeException e) {
            throw mapError(e);
        }
    }

    public void confirmCheckoutSession(long leaseId, long tenantId, String bearerToken, Map<String, Object> body) {
        try {
            restClient.post()
                    .uri("/api/v1/leases/{id}/payments/confirm", leaseId)
                    .header("X-Tenant-Id", String.valueOf(tenantId))
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            throw mapError(e);
        }
    }

    private static RuntimeException mapError(HttpStatusCodeException e) {
        HttpStatusCode status = e.getStatusCode();
        String msg = e.getResponseBodyAsString();
        if (status.value() == 404) {
            return new BadRequestException("Payment service returned 404");
        }
        return new BadRequestException("Payment service error: " + (msg != null && !msg.isBlank() ? msg : e.getMessage()));
    }
}

