package br.com.blockready.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFoundReturns404() {
        ProblemDetail problem = handler.handleNotFound(new ResourceNotFoundException("Território 7 não existe"));

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getDetail()).contains("Território 7");
    }

    @Test
    void unexpectedErrorDoesNotLeakInternalMessage() {
        ProblemDetail problem = handler.handleUnexpected(new IllegalStateException("senha=abc"));

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail()).doesNotContain("senha");
    }
}
