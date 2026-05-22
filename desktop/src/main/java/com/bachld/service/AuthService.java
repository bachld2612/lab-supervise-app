package com.bachld.service;

import com.bachld.client.AuthApiClient;
import com.bachld.exception.RestClientException;
import com.bachld.model.response.AuthResponse;
import com.bachld.model.response.AuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;

import javax.swing.SwingWorker;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * AuthService coordinates the authentication workflow.
 * Executes API calls on background threads and marshals callbacks to EDT for UI updates.
 * 
 * Requirements: 7.4, 7.5, 7.6, 8.1, 9.1, 9.2, 11.1, 12.1, 12.2, 12.3
 */
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final AuthApiClient authApiClient;
    private final TokenManager tokenManager;
    private final SessionManager sessionManager;
    
    /**
     * Constructs an AuthService with dependency injection.
     * 
     * @param authApiClient the API client for authentication endpoints
     * @param tokenManager the token manager for JWT storage
     * @param sessionManager the session manager for user session storage
     */
    public AuthService(AuthApiClient authApiClient, 
                      TokenManager tokenManager,
                      SessionManager sessionManager) {
        this.authApiClient = authApiClient;
        this.tokenManager = tokenManager;
        this.sessionManager = sessionManager;
    }
    
    /**
     * Performs asynchronous login authentication.
     * Creates and executes a LoginWorker to handle the API call on a background thread.
     * Callbacks are invoked on the Event Dispatch Thread for thread-safe UI updates.
     * 
     * @param email the user's email address
     * @param password the user's password
     * @param callback the callback to invoke on success or error
     */
    public void loginAsync(String email, String password, String wifiSsid, AuthCallback callback) {
        logger.info("Authentication attempt for email: {}", email);
        LoginWorker worker = new LoginWorker(email, password, wifiSsid, callback);
        worker.execute();
    }
    
    /**
     * Callback interface for authentication results.
     * Methods are invoked on the Event Dispatch Thread for thread-safe UI updates.
     */
    public interface AuthCallback {
        /**
         * Called when authentication succeeds.
         * 
         * @param response the authentication response containing token, user, and role
         */
        void onSuccess(AuthResponse response);
        
        /**
         * Called when authentication fails.
         * 
         * @param errorMessage a user-friendly error message
         */
        void onError(String errorMessage);
    }
    
    /**
     * SwingWorker that executes authentication API call in background thread.
     * Extracts result and invokes callback on EDT in done() method.
     */
    private class LoginWorker extends SwingWorker<AuthResponse, Void> {

        private final String email;
        private final String password;
        private final String wifiSsid;
        private final AuthCallback callback;

        public LoginWorker(String email, String password, String wifiSsid, AuthCallback callback) {
            this.email = email;
            this.password = password;
            this.wifiSsid = wifiSsid;
            this.callback = callback;
        }
        
        /**
         * Executes API call in background thread.
         * 
         * @return the authentication response
         * @throws Exception if the API call fails
         */
        @Override
        protected AuthResponse doInBackground() throws Exception {
            return authApiClient.login(email, password, "desktop", wifiSsid);
        }
        
        /**
         * Invoked on EDT after doInBackground completes.
         * Extracts result, handles exceptions, and invokes callback.
         */
        @Override
        protected void done() {
            try {
                // Extract result from background thread
                AuthResponse response = get();
                
                // Check status code
                if (response.getStatusCode() == 200) {
                    // Success - extract and store token and session
                    AuthData data = response.getData();
                    
                    if (data != null) {
                        // Store token in TokenManager (Requirement 8.1)
                        if (data.getToken() != null) {
                            tokenManager.setToken(data.getToken());
                        }
                        
                        // Store session in SessionManager (Requirements 9.1, 9.2)
                        if (data.getUser() != null && data.getRole() != null) {
                            sessionManager.setSession(data.getUser(), data.getRole());
                        }
                    }
                    
                    logger.info("Authentication successful for email: {}", email);
                    
                    // Invoke success callback on EDT (Requirement 12.1)
                    callback.onSuccess(response);
                    
                } else if (response.getStatusCode() == 422) {
                    // Validation error - extract error message (Requirement 7.6)
                    String errorMessage = response.getMessage();
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "Email hoặc mật khẩu không chính xác.";
                    }
                    logger.warn("Authentication failed for email: {} - Status: 422, Message: {}", email, errorMessage);
                    callback.onError(errorMessage);
                    
                } else {
                    // Unexpected status code
                    String errorMessage = response.getMessage();
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.";
                    }
                    logger.error("Authentication failed for email: {} - Unexpected status: {}, Message: {}", 
                                email, response.getStatusCode(), errorMessage);
                    callback.onError(errorMessage);
                }
                
            } catch (InterruptedException e) {
                // Thread was interrupted
                Thread.currentThread().interrupt();
                logger.warn("Authentication interrupted for email: {}", email);
                callback.onError("Yêu cầu đã bị hủy.");
                
            } catch (java.util.concurrent.ExecutionException e) {
                // Exception occurred in doInBackground
                Throwable cause = e.getCause();
                String errorMessage = mapExceptionToUserMessage(cause);
                
                logger.error("Authentication error for email: {}", email, cause);
                
                // Invoke error callback on EDT (Requirement 12.2)
                callback.onError(errorMessage);
            }
        }
        
        /**
         * Maps exceptions to user-friendly error messages.
         * 
         * @param throwable the exception to map
         * @return a user-friendly error message
         */
        private String mapExceptionToUserMessage(Throwable throwable) {
            // Unwrap RestClientException to get the real cause
            Throwable cause = throwable;
            if (throwable instanceof RestClientException && throwable.getCause() != null) {
                cause = throwable.getCause();
            }

            // Handle 422 from HttpClientErrorException - lấy message từ statusText
            if (cause instanceof HttpClientErrorException) {
                HttpClientErrorException httpEx = (HttpClientErrorException) cause;
                if (httpEx.getStatusCode().value() == 422) {
                    String msg = httpEx.getStatusText();
                    return (msg != null && !msg.isBlank()) ? msg : "Tài khoản hoặc mật khẩu không chính xác.";
                }
            }

            // Network errors
            return mapNetworkException(cause);
        }
        
        /**
         * Maps network exceptions to user-friendly error messages.
         * 
         * @param throwable the exception to map
         * @return a user-friendly error message
         */
        private String mapNetworkException(Throwable throwable) {
            if (throwable instanceof SocketTimeoutException) {
                return "Yêu cầu đã hết thời gian chờ. Vui lòng thử lại.";
            } else if (throwable instanceof ConnectException) {
                return "Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.";
            } else if (throwable instanceof UnknownHostException) {
                return "Không thể kết nối đến máy chủ. Vui lòng kiểm tra cấu hình.";
            } else {
                return "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.";
            }
        }
    }
}
