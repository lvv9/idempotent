package me.liuweiqiang.idempotent.aop;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Order(1)
@Component
public class ExceptionAOP {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String THROW_EXCEPTION = "throwException";

    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional) " +
            "&& execution(* process(..)) && args(consumer, reqId, req)")
    public void throwException(String consumer, String reqId, String req) {}

    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional) " +
            "&& execution(* processOnNest(..)) && args(req)")
    public void throwExceptionNested(String req) {}

    @After("throwException(consumer, reqId, req)")
    public void afterCommit(String consumer, String reqId, String req) {
        logger.info("afterCommit: {}", req);
        if (THROW_EXCEPTION.equals(req)) {
            throw new RuntimeException();
        }
    }

    @After("throwExceptionNested(req)")
    public void afterCommit(String req) {
        logger.info("afterCommit: {}", req);
        if (THROW_EXCEPTION.equals(req)) {
            throw new RuntimeException();
        }
    }
}
