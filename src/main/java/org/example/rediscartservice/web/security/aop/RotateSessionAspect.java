package org.example.rediscartservice.web.security.aop;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.rediscartservice.web.security.annotations.RotateSession;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class RotateSessionAspect {

    @Around("@annotation(rotateSession)")
    public Object around(ProceedingJoinPoint pjp, RotateSession rotateSession) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            HttpSession old = request.getSession(false);
            if (old != null) old.invalidate();
            request.getSession(true); // create fresh session id before proceeding
        }
        return pjp.proceed();
    }
}