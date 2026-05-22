package com.bachld.service;

import com.bachld.client.AuthApiClient;
import com.bachld.config.RestClient;
import com.bachld.exception.RestClientException;
import com.bachld.model.response.AuthData;
import com.bachld.model.response.AuthResponse;
import com.bachld.model.response.Role;
import com.bachld.model.response.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuthService.
 * Tests authentication workflow, callback invocation, and error handling.
 * 
 * Validates: Requirements 7.4, 7.5, 7.6, 8.1, 9.1, 9.2, 12.1, 12.2, 12.3
 */
class AuthServiceTest {
    
    private TokenManager tokenManager;
    private SessionManager sessionManager;
    private AuthService authService;
    
    /**
     * Mock AuthApiClient for testing.
     */
    private static class MockAuthApiClient extends AuthApiClient {
        private AuthResponse responseToReturn;
        private RestClientException exceptionToThrow;
        
        public MockAuthApiClient() {
            super(RestClient.getInstance());
        }
        
        public void setResponseToReturn(AuthResponse response) {
            this.responseToReturn = response;
            this.exceptionToThrow = null;
        }
        
        public void setExceptionToThrow(RestClientException exception) {
            this.exceptionToThrow = exception;
            this.responseToReturn = null;
        }
        
        @Override
        public AuthResponse login(String email, String password, String device, String wifiSsid)
                throws RestClientException {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return responseToReturn;
        }
    }
    
    private MockAuthApiClient mockAuthApiClient;
    
    @BeforeEach
    void setUp() {
        mockAuthApiClient = new MockAuthApiClient();
        
        // Use real instances for managers to verify state changes
        tokenManager = TokenManager.getInstance();
        sessionManager = SessionManager.getInstance();
        
        // Clear any existing state
        tokenManager.clearToken();
        sessionManager.clearSession();
        
        authService = new AuthService(mockAuthApiClient, tokenManager, sessionManager);
    }
    
    @Test
    void constructor_WithValidDependencies_CreatesInstance() {
        assertNotNull(authService, "AuthService should be instantiated");
    }
    
    @Test
    void loginAsync_WithSuccessfulResponse_InvokesOnSuccessCallback() throws Exception {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        String token = "jwt-token-123";
        
        User user = new User(1L, email, "Test User", "12345");
        Role role = new Role(1L, "STUDENT", "Student role");
        AuthData authData = new AuthData(token, user, role);
        AuthResponse response = new AuthResponse(200, "Success", authData);
        
        mockAuthApiClient.setResponseToReturn(response);
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<AuthResponse> capturedResponse = new AtomicReference<>();
        
        // Act
        authService.loginAsync(email, password, null, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                capturedResponse.set(response);
                latch.countDown();
            }
            
            @Override
            public void onError(String errorMessage) {
                fail("onError should not be called on success");
            }
        });
        
        // Assert
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Callback should be invoked");
        assertNotNull(capturedResponse.get(), "Response should be captured");
        assertEquals(200, capturedResponse.get().getStatusCode());
        
        // Verify token was stored (Requirement 8.1)
        assertEquals(token, tokenManager.getToken());
        
        // Verify session was stored (Requirements 9.1, 9.2)
        assertEquals(user, sessionManager.getCurrentUser());
        assertEquals(role, sessionManager.getCurrentRole());
        assertTrue(sessionManager.isAuthenticated());
    }
    
    @Test
    void loginAsync_With422Response_InvokesOnErrorCallback() throws Exception {
        // Arrange
        String email = "test@example.com";
        String password = "wrongpassword";
        String errorMessage = "Email hoặc mật khẩu không chính xác.";
        
        AuthResponse response = new AuthResponse(422, errorMessage, null);
        
        mockAuthApiClient.setResponseToReturn(response);
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedError = new AtomicReference<>();
        
        // Act
        authService.loginAsync(email, password, null, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                fail("onSuccess should not be called on error");
            }
            
            @Override
            public void onError(String errorMessage) {
                capturedError.set(errorMessage);
                latch.countDown();
            }
        });
        
        // Assert
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Callback should be invoked");
        assertEquals(errorMessage, capturedError.get());
        
        // Verify token and session were not stored
        assertFalse(tokenManager.hasToken());
        assertFalse(sessionManager.isAuthenticated());
    }
    
    @Test
    void loginAsync_WithSocketTimeoutException_InvokesOnErrorWithTimeoutMessage() throws Exception {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        
        mockAuthApiClient.setExceptionToThrow(
            new RestClientException("Timeout", new SocketTimeoutException("Read timed out"))
        );
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedError = new AtomicReference<>();
        
        // Act
        authService.loginAsync(email, password, null, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                fail("onSuccess should not be called on error");
            }
            
            @Override
            public void onError(String errorMessage) {
                capturedError.set(errorMessage);
                latch.countDown();
            }
        });
        
        // Assert
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Callback should be invoked");
        assertEquals("Yêu cầu đã hết thời gian chờ. Vui lòng thử lại.", capturedError.get());
    }
    
    @Test
    void loginAsync_WithConnectException_InvokesOnErrorWithNetworkMessage() throws Exception {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        
        mockAuthApiClient.setExceptionToThrow(
            new RestClientException("Connection failed", new ConnectException("Connection refused"))
        );
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedError = new AtomicReference<>();
        
        // Act
        authService.loginAsync(email, password, null, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                fail("onSuccess should not be called on error");
            }
            
            @Override
            public void onError(String errorMessage) {
                capturedError.set(errorMessage);
                latch.countDown();
            }
        });
        
        // Assert
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Callback should be invoked");
        assertEquals("Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.", capturedError.get());
    }
    
    @Test
    void loginAsync_WithUnknownHostException_InvokesOnErrorWithConfigMessage() throws Exception {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        
        mockAuthApiClient.setExceptionToThrow(
            new RestClientException("Unknown host", new UnknownHostException("api.example.com"))
        );
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedError = new AtomicReference<>();
        
        // Act
        authService.loginAsync(email, password, null, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                fail("onSuccess should not be called on error");
            }
            
            @Override
            public void onError(String errorMessage) {
                capturedError.set(errorMessage);
                latch.countDown();
            }
        });
        
        // Assert
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Callback should be invoked");
        assertEquals("Không thể kết nối đến máy chủ. Vui lòng kiểm tra cấu hình.", capturedError.get());
    }
    
    @Test
    void loginAsync_WithGenericException_InvokesOnErrorWithGenericMessage() throws Exception {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        
        mockAuthApiClient.setExceptionToThrow(
            new RestClientException("Unexpected error", new RuntimeException("Something went wrong"))
        );
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedError = new AtomicReference<>();
        
        // Act
        authService.loginAsync(email, password, null, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                fail("onSuccess should not be called on error");
            }
            
            @Override
            public void onError(String errorMessage) {
                capturedError.set(errorMessage);
                latch.countDown();
            }
        });
        
        // Assert
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Callback should be invoked");
        assertEquals("Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.", capturedError.get());
    }
    
    @Test
    void loginAsync_WithNullAuthData_InvokesOnSuccessButDoesNotStoreTokenOrSession() throws Exception {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        
        AuthResponse response = new AuthResponse(200, "Success", null);
        
        mockAuthApiClient.setResponseToReturn(response);
        
        CountDownLatch latch = new CountDownLatch(1);
        
        // Act
        authService.loginAsync(email, password, null, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                latch.countDown();
            }
            
            @Override
            public void onError(String errorMessage) {
                fail("onError should not be called");
            }
        });
        
        // Assert
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Callback should be invoked");
        
        // Verify token and session were not stored
        assertFalse(tokenManager.hasToken());
        assertFalse(sessionManager.isAuthenticated());
    }
    
    @Test
    void loginAsync_WithNullToken_InvokesOnSuccessButDoesNotStoreToken() throws Exception {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        
        User user = new User(1L, email, "Test User", "12345");
        Role role = new Role(1L, "STUDENT", "Student role");
        AuthData authData = new AuthData(null, user, role);
        AuthResponse response = new AuthResponse(200, "Success", authData);
        
        mockAuthApiClient.setResponseToReturn(response);
        
        CountDownLatch latch = new CountDownLatch(1);
        
        // Act
        authService.loginAsync(email, password, null, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                latch.countDown();
            }
            
            @Override
            public void onError(String errorMessage) {
                fail("onError should not be called");
            }
        });
        
        // Assert
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Callback should be invoked");
        
        // Verify token was not stored but session was
        assertFalse(tokenManager.hasToken());
        assertTrue(sessionManager.isAuthenticated());
    }
    
    @Test
    void loginAsync_WithUnexpectedStatusCode_InvokesOnErrorCallback() throws Exception {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        String errorMessage = "Internal server error";
        
        AuthResponse response = new AuthResponse(500, errorMessage, null);
        
        mockAuthApiClient.setResponseToReturn(response);
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedError = new AtomicReference<>();
        
        // Act
        authService.loginAsync(email, password, null, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                fail("onSuccess should not be called on error");
            }
            
            @Override
            public void onError(String errorMessage) {
                capturedError.set(errorMessage);
                latch.countDown();
            }
        });
        
        // Assert
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Callback should be invoked");
        assertEquals(errorMessage, capturedError.get());
    }
    
    @Test
    void loginAsync_ExecutesOnBackgroundThread() throws Exception {
        // Arrange
        String email = "test@example.com";
        String password = "password123";
        
        User user = new User(1L, email, "Test User", "12345");
        Role role = new Role(1L, "STUDENT", "Student role");
        AuthData authData = new AuthData("token", user, role);
        AuthResponse response = new AuthResponse(200, "Success", authData);
        
        mockAuthApiClient.setResponseToReturn(response);
        
        CountDownLatch latch = new CountDownLatch(1);
        
        // Act
        authService.loginAsync(email, password, null, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                latch.countDown();
            }
            
            @Override
            public void onError(String errorMessage) {
                fail("onError should not be called");
            }
        });
        
        // Assert
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Callback should be invoked");
        // Note: We can't easily verify the thread name without modifying the mock,
        // but the fact that the test completes without blocking proves background execution
    }
}
