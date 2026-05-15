package com.unitbv.myquiz.thy.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.UUID;

/**
 * Request Logging Interceptor for debugging and monitoring.
 * Logs all incoming HTTP requests with detailed information including:
 * - Request ID (for tracing)
 * - HTTP method and URI
 * - Query parameters
 * - Request headers (excluding sensitive data)
 * - Session information
 * - Response status and execution time
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // Thread-local storage for request start time and request ID
    private static final ThreadLocal<Long> startTimeThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<String> requestIdThreadLocal = new ThreadLocal<>();

    // Sensitive headers that should not be logged
    private static final String[] SENSITIVE_HEADERS = {
        "authorization", "cookie", "set-cookie", "x-auth-token", "x-csrf-token"
    };

    /**
     * Called before the handler is executed.
     * Logs incoming request details.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        // Store in thread-local for later use
        startTimeThreadLocal.set(startTime);
        requestIdThreadLocal.set(requestId);

        // Add request ID to response header for client-side correlation
        response.setHeader("X-Request-Id", requestId);

        if (log.isInfoEnabled()) {
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("\n========== Incoming Request [").append(requestId).append("] ==========\n");
            logMessage.append("Timestamp: ").append(OffsetDateTime.now().format(DATE_FORMATTER)).append("\n");
            logMessage.append("Method: ").append(request.getMethod()).append("\n");
            logMessage.append("URI: ").append(request.getRequestURI()).append("\n");

            // Query string
            if (request.getQueryString() != null) {
                logMessage.append("Query String: ").append(request.getQueryString()).append("\n");
            }

            // Remote address
            logMessage.append("Remote Address: ").append(getClientIpAddress(request)).append("\n");

            // Session info
            if (request.getSession(false) != null) {
                logMessage.append("Session ID: ").append(request.getSession().getId()).append("\n");
            }

            // Headers (excluding sensitive ones)
            if (log.isDebugEnabled()) {
                logMessage.append("Headers:\n");
                Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    if (!isSensitiveHeader(headerName)) {
                        logMessage.append("  ").append(headerName).append(": ")
                                 .append(request.getHeader(headerName)).append("\n");
                    } else {
                        logMessage.append("  ").append(headerName).append(": [REDACTED]\n");
                    }
                }
            }

            logMessage.append("Handler: ").append(handler.getClass().getSimpleName()).append("\n");
            logMessage.append("=".repeat(50));

            log.info(logMessage.toString());
        }

        return true;
    }

    /**
     * Called after the handler is executed but before the view is rendered.
     * Logs model attributes if debug logging is enabled.
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                          ModelAndView modelAndView) {
        if (log.isDebugEnabled() && modelAndView != null) {
            String requestId = requestIdThreadLocal.get();
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("\n---------- Post Handle [").append(requestId).append("] ----------\n");
            logMessage.append("View Name: ").append(modelAndView.getViewName()).append("\n");

            if (!modelAndView.getModel().isEmpty()) {
                logMessage.append("Model Attributes: ");
                modelAndView.getModel().keySet().forEach(key ->
                    logMessage.append(key).append(", ")
                );
                logMessage.append("\n");
            }

            logMessage.append("-".repeat(50));
            log.debug(logMessage.toString());
        }
    }

    /**
     * Called after the complete request has finished.
     * Logs response status and execution time.
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        Long startTime = startTimeThreadLocal.get();
        String requestId = requestIdThreadLocal.get();

        if (startTime != null) {
            long executionTime = System.currentTimeMillis() - startTime;

            if (log.isInfoEnabled()) {
                StringBuilder logMessage = new StringBuilder();
                logMessage.append("\n========== Request Completed [").append(requestId).append("] ==========\n");
                logMessage.append("Status: ").append(response.getStatus()).append("\n");
                logMessage.append("Execution Time: ").append(executionTime).append(" ms\n");

                if (ex != null) {
                    logMessage.append("Exception: ").append(ex.getClass().getSimpleName())
                             .append(" - ").append(ex.getMessage()).append("\n");
                }

                // Performance warning for slow requests
                if (executionTime > 1000) {
                    logMessage.append("⚠ WARNING: Slow request detected (>1s)\n");
                }

                logMessage.append("=".repeat(50));

                if (ex != null) {
                    log.error(logMessage.toString(), ex);
                } else if (executionTime > 1000) {
                    log.warn(logMessage.toString());
                } else {
                    log.info(logMessage.toString());
                }
            }

            // Clean up thread-local storage
            startTimeThreadLocal.remove();
            requestIdThreadLocal.remove();
        }
    }

    /**
     * Gets the real client IP address, considering proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Get first IP if multiple IPs are present
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Checks if a header is sensitive and should be redacted from logs.
     */
    private boolean isSensitiveHeader(String headerName) {
        if (headerName == null) {
            return false;
        }
        String lowerCaseHeader = headerName.toLowerCase();
        for (String sensitiveHeader : SENSITIVE_HEADERS) {
            if (lowerCaseHeader.contains(sensitiveHeader)) {
                return true;
            }
        }
        return false;
    }
}

