package com.eubican.practices.brokerage.oms.unit;

import com.eubican.practices.brokerage.oms.domain.exception.ApplicationException;
import com.eubican.practices.brokerage.oms.web.advice.ApiError;
import com.eubican.practices.brokerage.oms.web.advice.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private HttpServletRequest mockReq() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getRequestURI()).thenReturn("/api/test");
        return req;
    }

    @Test
    void badRequestMapsIllegalArgument() {
        ResponseEntity<ApiError> resp = handler.badRequest(new IllegalArgumentException("oops"), mockReq());

        Assertions.assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(resp.getBody()).isNotNull();
        Assertions.assertThat(resp.getBody().message()).isEqualTo("oops");
        Assertions.assertThat(resp.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    void conflictMapsOptimisticLock() {
        ResponseEntity<ApiError> resp = handler.conflict(new OptimisticLockingFailureException("conflict"), mockReq());

        Assertions.assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        Assertions.assertThat(resp.getBody()).isNotNull();
        Assertions.assertThat(resp.getBody().message()).isEqualTo("conflict");
    }

    @Test
    void validationMapsFieldErrorFirstMessage() {
        BindingResult binding = Mockito.mock(BindingResult.class);
        Mockito.when(binding.getFieldErrors()).thenReturn(List.of(new FieldError("obj", "field1", "must not be null")));
        MethodArgumentNotValidException ex = Mockito.mock(MethodArgumentNotValidException.class);
        Mockito.when(ex.getBindingResult()).thenReturn(binding);

        ResponseEntity<ApiError> resp = handler.validation(ex, mockReq());

        Assertions.assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(resp.getBody()).isNotNull();
        Assertions.assertThat(resp.getBody().message()).isEqualTo("field1: must not be null");
    }

    @Test
    void applicationMapsStatusFromException() {
        ApplicationException ex = new ApplicationException(HttpStatus.FORBIDDEN, "forbidden!");

        ResponseEntity<ApiError> resp = handler.application(ex, mockReq());

        Assertions.assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Assertions.assertThat(resp.getBody()).isNotNull();
        Assertions.assertThat(resp.getBody().message()).isEqualTo("forbidden!");
    }

    @Test
    void genericMapsInternalServerError() {
        ResponseEntity<ApiError> resp = handler.generic(new RuntimeException("boom"), mockReq());

        Assertions.assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        Assertions.assertThat(resp.getBody()).isNotNull();
        Assertions.assertThat(resp.getBody().message()).isEqualTo("boom");
    }
}
