package co.edu.ucc.orientacion.dto.response;

public record ApiResponse(boolean success, Object data, String message) {

    public static ApiResponse ok(Object data, String message) {
        return new ApiResponse(true, data, message);
    }

    public static ApiResponse error(String message) {
        return new ApiResponse(false, null, message);
    }
}
