package com.eldercare.backend.common.data;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.ReasonRequiredException;
import java.lang.reflect.Method;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Lệnh {@link BusinessCommand} bắt buộc lý do mà tham số {@link Reason} là {@code null} thì trả 422 REASON_REQUIRED
 * trước khi mở transaction, chưa chạm dữ liệu (FR-007, SC-008, data-model mục 5).
 */
@Aspect
@Component
// Chạy trước @Transactional (thứ tự mặc định thấp nhất) nên chưa mở transaction
@Order(0)
public class BusinessCommandAspect {

    @Before("@annotation(command)")
    public void requireReason(JoinPoint joinPoint, BusinessCommand command) {
        if (!command.requiresReason()) {
            return;
        }
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Class<?>[] types = method.getParameterTypes();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < types.length; i++) {
            if (types[i] == Reason.class && args[i] == null) {
                throw new ReasonRequiredException();
            }
        }
    }
}
