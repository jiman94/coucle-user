package com.example.user.aop;

import com.example.user.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class AopAspect {
    // define pointcut and advice
    @Around(value="@annotation(LogExecutionTime) && args(member)", argNames = "member")
    public Object LogExecutionTime(ProceedingJoinPoint joinPoint, Member member) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object proceed = joinPoint.proceed(); // method return value
        long executionTime = System.currentTimeMillis() - startTime;

        log.info(joinPoint.getSignature() + " executed in " + executionTime);
        log.info("email: " + member.getEmail() + " assigned to coupon(" + proceed + ")");
        return proceed;
    }
}
