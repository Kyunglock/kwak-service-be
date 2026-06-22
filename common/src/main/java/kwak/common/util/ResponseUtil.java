package kwak.common.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import kwak.common.application.dto.RokResponse;

public class ResponseUtil {

    public static <T> ResponseEntity<RokResponse<T>> success(T data) {
        return success(data, "요청 성공");
    }

    public static <T> ResponseEntity<RokResponse<T>> success(T data, String message) {
        return ResponseEntity.ok(
            RokResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build()
        );
    }

    public static <T> ResponseEntity<RokResponse<T>> created(T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            RokResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build()
        );
    }

    public static <T> ResponseEntity<RokResponse<T>> error(String message) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public static <T> ResponseEntity<RokResponse<T>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
            RokResponse.<T>builder()
                .success(false)
                .message(message)
                .build()
        );
    }

    public static <T> ResponseEntity<RokResponse<T>> badRequest(String message) {
        return error(HttpStatus.BAD_REQUEST, message);
    }

    public static <T> ResponseEntity<RokResponse<T>> notFound(String message) {
        return error(HttpStatus.NOT_FOUND, message);
    }

    public static ResponseEntity<?> redirect(String location) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location)
                .build();
    }
    
    public static ResponseEntity<?> noContent() {
        return ResponseEntity.noContent().build();
    }
}
