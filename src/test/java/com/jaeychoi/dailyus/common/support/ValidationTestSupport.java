package com.jaeychoi.dailyus.common.support;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;

public abstract class ValidationTestSupport {

  private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
  protected static final Validator validator = VALIDATOR_FACTORY.getValidator();

  @AfterAll
  static void closeValidatorFactory() {
    VALIDATOR_FACTORY.close();
  }

  protected <T> Set<String> validatePropertyNames(T target) {
    return validator.validate(target)
        .stream()
        .map(ConstraintViolation::getPropertyPath)
        .map(Object::toString)
        .collect(Collectors.toSet());
  }

  protected <T> void assertValid(T target) {
    Assertions.assertThat(validator.validate(target)).isEmpty();
  }
}
