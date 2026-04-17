package com.bachld.config;

import com.bachld.model.response.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CustomErrorHandler.
 * 
 * Tests error message extraction from HTTP error responses, particularly
 * for 422 status codes.
 * 
 * Validates: Requirements 6.4, 7.6
 */
class CustomErrorHandlerTest {
    
    private CustomErrorHandler errorHandler;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        errorHandler = new CustomErrorHandler(objectMapper);
    }
    
    @Test
    void handleError_422WithValidErrorResponse_ExtractsMessageField() throws IOException {
        // Arrange
        ApiErrorResponse apiError = new ApiErrorResponse(
            422,
            "Email hoặc mật khẩu không chính xác.",
            "Unprocessable Entity"
        );
        String jsonResponse = objectMapper.writeValueAsString(apiError);
        
        ClientHttpResponse response = new TestClientHttpResponse(
            HttpStatus.UNPROCESSABLE_ENTITY.value(),
            jsonResponse
        );
        
        // Act & Assert
        HttpClientErrorException exception = assertThrows(
            HttpClientErrorException.class,
            () -> errorHandler.handleError(response)
        );
        
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        assertEquals("Email hoặc mật khẩu không chính xác.", exception.getStatusText());
    }
    
    @Test
    void handleError_422WithEmptyMessage_UsesDefaultMessage() throws IOException {
        // Arrange
        ApiErrorResponse apiError = new ApiErrorResponse(422, "", "Unprocessable Entity");
        String jsonResponse = objectMapper.writeValueAsString(apiError);
        
        ClientHttpResponse response = new TestClientHttpResponse(
            HttpStatus.UNPROCESSABLE_ENTITY.value(),
            jsonResponse
        );
        
        // Act & Assert
        HttpClientErrorException exception = assertThrows(
            HttpClientErrorException.class,
            () -> errorHandler.handleError(response)
        );
        
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        assertEquals("Unprocessable Entity", exception.getStatusText());
    }
    
    @Test
    void handleError_422WithNullMessage_UsesDefaultMessage() throws IOException {
        // Arrange
        ApiErrorResponse apiError = new ApiErrorResponse(422, null, "Unprocessable Entity");
        String jsonResponse = objectMapper.writeValueAsString(apiError);
        
        ClientHttpResponse response = new TestClientHttpResponse(
            HttpStatus.UNPROCESSABLE_ENTITY.value(),
            jsonResponse
        );
        
        // Act & Assert
        HttpClientErrorException exception = assertThrows(
            HttpClientErrorException.class,
            () -> errorHandler.handleError(response)
        );
        
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        assertEquals("Unprocessable Entity", exception.getStatusText());
    }
    
    @Test
    void handleError_422WithInvalidJson_FallsBackToDefaultHandling() throws IOException {
        // Arrange
        String invalidJson = "{ invalid json }";
        ClientHttpResponse response = new TestClientHttpResponse(
            HttpStatus.UNPROCESSABLE_ENTITY.value(),
            invalidJson
        );
        
        // Act & Assert
        // Should throw exception from default handler
        assertThrows(Exception.class, () -> errorHandler.handleError(response));
    }
    
    @Test
    void handleError_400BadRequest_UsesDefaultHandling() throws IOException {
        // Arrange
        ClientHttpResponse response = new TestClientHttpResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request"
        );
        
        // Act & Assert
        assertThrows(HttpClientErrorException.class, () -> errorHandler.handleError(response));
    }
    
    @Test
    void handleError_500InternalServerError_UsesDefaultHandling() throws IOException {
        // Arrange
        ClientHttpResponse response = new TestClientHttpResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error"
        );
        
        // Act & Assert
        assertThrows(HttpServerErrorException.class, () -> errorHandler.handleError(response));
    }
    
    /**
     * Test implementation of ClientHttpResponse for testing purposes.
     */
    private static class TestClientHttpResponse implements ClientHttpResponse {
        private final int statusCode;
        private final String body;
        private final HttpHeaders headers;
        
        public TestClientHttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
            this.headers = new HttpHeaders();
        }
        
        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return HttpStatusCode.valueOf(statusCode);
        }
        
        @Override
        @Deprecated
        public int getRawStatusCode() throws IOException {
            return statusCode;
        }
        
        @Override
        public String getStatusText() throws IOException {
            HttpStatus status = HttpStatus.resolve(statusCode);
            return status != null ? status.getReasonPhrase() : "";
        }
        
        @Override
        public void close() {
            // No-op for test implementation
        }
        
        @Override
        public InputStream getBody() throws IOException {
            return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        }
        
        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }
}
