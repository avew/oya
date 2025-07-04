package io.github.avew.oya.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.zalando.problem.Problem;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private Integer status; // 0 for success, 1 for error
    private String code; // Specific error/success code like FILE_0001
    private String message; // Localized message
    private T data; // Response data

    public static <T> ApiResponse<T> success(String code, String message, T data) {
        return ApiResponse.<T>builder()
                .status(0)
                .code(code)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String code, String message) {
        return ApiResponse.<T>builder()
                .status(0)
                .code(code)
                .message(message)
                .data(null)
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .status(1)
                .code(code)
                .message(message)
                .data(null)
                .build();
    }

    // Convert from Problem to ApiResponse
    public static <T> ApiResponse<T> fromProblem(Problem problem, String code) {
        return ApiResponse.<T>builder()
                .status(1)
                .code(code)
                .message(problem.getDetail())
                .data(null)
                .build();
    }
}
