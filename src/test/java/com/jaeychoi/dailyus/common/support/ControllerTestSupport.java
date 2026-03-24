package com.jaeychoi.dailyus.common.support;

import org.assertj.core.api.Assertions;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public abstract class ControllerTestSupport extends UnitTestSupport {

  protected void assertStatus(ResponseEntity<?> response, HttpStatus expectedStatus) {
    Assertions.assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
  }

  protected void assertCreated(ResponseEntity<?> response) {
    assertStatus(response, HttpStatus.CREATED);
  }
}
