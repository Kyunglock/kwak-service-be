package kwak.common.util;

public class ObjectUtil {
    
    /**
     * 값이 null이 아닌지 확인
     */
    public static boolean isNotNull(Object obj) {
        return obj != null;
    }
    
    /**
     * 값이 null인지 확인
     */
    public static boolean isNull(Object obj) {
        return obj == null;
    }
    
    /**
     * 값이 null이면 기본값 반환
     */
    public static <T> T getOrDefault(T value, T defaultValue) {
        return isNotNull(value) ? value : defaultValue;
    }
    
    /**
     * 값이 null이면 supplier로 기본값 생성
     */
    public static <T> T getOrElse(T value, java.util.function.Supplier<T> supplier) {
        return isNotNull(value) ? value : supplier.get();
    }
    
    /**
     * 중첩된 null 체크 (체인 형태)
     */
    public static <T> T getNestedOrDefault(T value, T defaultValue) {
        return getOrDefault(value, defaultValue);
    }
    
    /**
     * String이 null 또는 빈 문자열인지 확인
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * String이 null이거나 빈 문자열이 아닌지 확인
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
    
    /**
     * String이 null이면 기본값 반환
     */
    public static String getStringOrDefault(String value, String defaultValue) {
        return isNotBlank(value) ? value : defaultValue;
    }
}