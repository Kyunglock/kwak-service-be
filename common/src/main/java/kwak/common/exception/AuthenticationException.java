package kwak.common.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends RuntimeException {
    
    private final HttpStatus httpStatus;
    private final String errorCode;
    
    public AuthenticationException(String message) {
        this(message, HttpStatus.UNAUTHORIZED, "AUTH_ERROR");
    }
    
    public AuthenticationException(String message, Throwable cause) {
        this(message, cause, HttpStatus.UNAUTHORIZED, "AUTH_ERROR");
    }
    
    public AuthenticationException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
    
    public AuthenticationException(String message, Throwable cause, HttpStatus httpStatus, String errorCode) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
    
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}