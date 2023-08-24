package com.hn2.util;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class BusinessExceptionSupplierUtil {
  public Supplier<BusinessException> getSupplier(String message) {
    return () -> new BusinessException(message);
  }

  public Supplier<BusinessException> getSupplier(ErrorType errorType, String message) {
    return () -> new BusinessException(errorType, message);
  }
}
