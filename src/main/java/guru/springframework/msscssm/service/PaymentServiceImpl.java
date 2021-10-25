package guru.springframework.msscssm.service;

import guru.springframework.msscssm.domain.Payment;
import guru.springframework.msscssm.domain.PaymentEvent;
import guru.springframework.msscssm.domain.PaymentState;
import guru.springframework.msscssm.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

  private final PaymentRepository paymentRepository;
  private final StateMachineFactory<PaymentState, PaymentEvent> stateMachineFactory;

  @Override
  public Payment newPayment(Payment payment) {
    payment.setState(PaymentState.NEW);
    return paymentRepository.save(payment);
  }

  @Override
  public StateMachine<PaymentState, PaymentEvent> preAuth(Long paymentId) {
    StateMachine<PaymentState, PaymentEvent> stateMachine = build(paymentId);
    return null;
  }

  @Override
  public StateMachine<PaymentState, PaymentEvent> authorizePayment(Long paymentId) {
    StateMachine<PaymentState, PaymentEvent> stateMachine = build(paymentId);
    return null;
  }

  @Override
  public StateMachine<PaymentState, PaymentEvent> declineAuthorization(Long paymentId) {
    StateMachine<PaymentState, PaymentEvent> stateMachine = build(paymentId);
    return null;
  }

  private StateMachine<PaymentState, PaymentEvent> build(Long paymentId) {
    Payment payment = paymentRepository.getOne(paymentId);

    StateMachine<PaymentState, PaymentEvent> stateMachine = stateMachineFactory.getStateMachine(Long.toString(payment.getId()));

    stateMachine.stop();

    stateMachine.getStateMachineAccessor()
        .doWithAllRegions(paymentStatePaymentEventStateMachineAccess -> {
          paymentStatePaymentEventStateMachineAccess.resetStateMachine(new DefaultStateMachineContext<>(payment.getState(),null,null,null));
        });

    stateMachine.start();

    return stateMachine;
  }
}
