package guru.springframework.msscssm.service;

import guru.springframework.msscssm.domain.Payment;
import guru.springframework.msscssm.domain.PaymentEvent;
import guru.springframework.msscssm.domain.PaymentState;
import guru.springframework.msscssm.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.plaf.PanelUI;
import java.util.Random;

@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

  public static final String PAYMENT_ID_HEADER = "payment_id";

  private final PaymentRepository paymentRepository;
  private final StateMachineFactory<PaymentState, PaymentEvent> stateMachineFactory;
  private final PaymentStateChangeInterceptor paymentStateChangeInterceptor;

  @Override
  public Payment newPayment(Payment payment) {
    payment.setState(PaymentState.NEW);
    return paymentRepository.save(payment);
  }

  @Transactional
  @Override
  public StateMachine<PaymentState, PaymentEvent> preAuth(Long paymentId) {
    StateMachine<PaymentState, PaymentEvent> stateMachine = build(paymentId);

    sendEvent(paymentId, stateMachine, PaymentEvent.PRE_AUTHORIZE);
    return stateMachine;
  }

  @Transactional
  @Override
  public StateMachine<PaymentState, PaymentEvent> authorizePayment(Long paymentId) {
    StateMachine<PaymentState, PaymentEvent> stateMachine = build(paymentId);

    sendEvent(paymentId, stateMachine, PaymentEvent.AUTHORIZE);
    return stateMachine;
  }

  @Deprecated // Not needed
  @Transactional
  @Override
  public StateMachine<PaymentState, PaymentEvent> declineAuthorization(Long paymentId) {
    StateMachine<PaymentState, PaymentEvent> stateMachine = build(paymentId);

    sendEvent(paymentId, stateMachine, PaymentEvent.AUTH_DECLINED);
    return stateMachine;
  }

  private void sendEvent(Long paymentId, StateMachine<PaymentState, PaymentEvent> sm, PaymentEvent event) {
    Message message = MessageBuilder.withPayload(event)
        .setHeader(PAYMENT_ID_HEADER, paymentId)
        .build();

    sm.sendEvent(message);
  }

  private StateMachine<PaymentState, PaymentEvent> build(Long paymentId) {
    Payment payment = paymentRepository.getOne(paymentId);

    StateMachine<PaymentState, PaymentEvent> stateMachine = stateMachineFactory.getStateMachine(Long.toString(payment.getId()));

    stateMachine.stop();

    stateMachine.getStateMachineAccessor()
        .doWithAllRegions(paymentStatePaymentEventStateMachineAccess -> {
          paymentStatePaymentEventStateMachineAccess.addStateMachineInterceptor(paymentStateChangeInterceptor);
          paymentStatePaymentEventStateMachineAccess.resetStateMachine(new DefaultStateMachineContext<>(payment.getState(),null,null,null));
        });

    stateMachine.start();

    return stateMachine;
  }

}
