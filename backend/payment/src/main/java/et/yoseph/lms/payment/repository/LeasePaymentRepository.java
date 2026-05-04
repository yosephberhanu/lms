package et.yoseph.lms.payment.repository;

import et.yoseph.lms.payment.domain.LeasePayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeasePaymentRepository extends JpaRepository<LeasePayment, Long> {

    List<LeasePayment> findByLeaseIdOrderByPeriodAsc(Long leaseId);

    Optional<LeasePayment> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);
}

