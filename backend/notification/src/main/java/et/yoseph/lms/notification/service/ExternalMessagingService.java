package et.yoseph.lms.notification.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.pingram.ApiException;
import io.pingram.Pingram;
import io.pingram.model.SenderPostBody;
import io.pingram.model.SenderPostBodyEmail;
import io.pingram.model.SenderPostBodySms;
import io.pingram.model.SenderPostBodyTo;
import io.pingram.model.SenderPostResponse;

@Service
public class ExternalMessagingService {

    private static final Logger log = LoggerFactory.getLogger(ExternalMessagingService.class);

    private Pingram pingram;

    @Value("${lms.pingram.api-key:}")
    private String apiKey;

    @Value("${lms.pingram.base-url:}")
    private String baseUrl;

    @Value("${lms.pingram.sender-email:}")
    private String senderEmail;
    
    @Value("${lms.pingram.sender-name:}")
    private String senderName;

    @PostConstruct
    void initPingram() {
        String key = nn(apiKey).trim();
        String url = trimTrailingSlash(nn(baseUrl).trim());
        if (StringUtils.hasText(key) && StringUtils.hasText(url)) {
            this.pingram = new Pingram(key, url);
        }
    }

    /**
     * Pingram SDK models mark string setters as {@code @Nonnull}; parameters may be null at runtime.
     */
    @SuppressWarnings("null")
    public void sendEmail(String to_address, String subject, String email_body) {
        if (pingram == null) {
            return;
        }
        String to = nn(to_address);
        String subj = nn(subject);
        String html = nn(email_body);
        String fromName = nn(senderName);
        String fromEmail = nn(senderEmail);

        SenderPostBodyTo bodyTo = new SenderPostBodyTo().email(to);

        SenderPostBody body = new SenderPostBody()
                .type("email_compose_preview")
                .to(bodyTo)
                .email(new SenderPostBodyEmail()
                        .subject(subj)
                        .html(html)
                        .senderName(fromName)
                        .senderEmail(fromEmail));

        try {
            SenderPostResponse response = pingram.send(body);
            log.info("Success! Tracking ID: " + response.getTrackingId());
            if (response.getMessages() != null && !response.getMessages().isEmpty()) {
                System.out.println("Messages: " + response.getMessages());
            }
        } catch (ApiException e) {
            log.error("API Error: " + e.getMessage());
            if (e.getResponseBody() != null) {
                log.error("Response: " + e.getResponseBody());
            }
        }
    }

    @SuppressWarnings("null")
    public void sendSMS(String to_phone, String message) {
        if (pingram == null) {
            return;
        }
        String phone = nn(to_phone);
        String msg = nn(message);

        SenderPostBodyTo to = new SenderPostBodyTo().number(phone);

        SenderPostBody body = new SenderPostBody()
                .type("sms_compose_preview")
                .to(to)
                .sms(new SenderPostBodySms().message(msg));

        try {
            SenderPostResponse response = pingram.send(body);
            log.info("Success! Tracking ID: " + response.getTrackingId());
        } catch (ApiException e) {
            log.error("API Error: " + e.getMessage());
            if (e.getResponseBody() != null) {
                log.error("Response: " + e.getResponseBody());
            }
        }
    }

    private static String nn(String s) {
        return s != null ? s : "";
    }

    private static String trimTrailingSlash(String u) {
        if (!StringUtils.hasText(u) || !u.endsWith("/")) {
            return u;
        }
        return u.substring(0, u.length() - 1);
    }
}
