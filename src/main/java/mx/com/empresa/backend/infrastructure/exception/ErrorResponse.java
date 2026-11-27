package mx.com.empresa.backend.infrastructure.exception;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {

    private String code;
    private String message;
    private LocalDateTime timestamp;
    private List<ErrorDetail> details;

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, LocalDateTime.now(ZoneOffset.UTC), List.of());
    }

    public static ErrorResponse of(String code, String message, List<ErrorDetail> details) {
        return new ErrorResponse(code, message, LocalDateTime.now(ZoneOffset.UTC), details);
    }
}
