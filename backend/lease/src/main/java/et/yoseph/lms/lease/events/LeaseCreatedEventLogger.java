package et.yoseph.lms.lease.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LeaseCreatedEventLogger {

    private static final Logger log = LoggerFactory.getLogger(LeaseCreatedEventLogger.class);

    @EventListener
    public void onLeaseCreated(LeaseCreatedEvent event) {
        log.info(
                "LeaseCreatedEvent leaseId={} propertyId={} tenantId={} monthlyRent={} start={} end={} createdAt={}",
                event.leaseId(),
                event.propertyId(),
                event.tenantId(),
                event.monthlyRent(),
                event.startDate(),
                event.endDate(),
                event.createdAt());
    }
}
