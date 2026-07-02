package kwak.common.ai;

public final class JsonExtractor {

    private JsonExtractor() {}

    /** 문자열에서 첫 '{' ~ 마지막 '}' 구간을 잘라 반환. 없으면 null. */
    public static String extractJsonObject(String text) {
        if (text == null) return null;
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        return text.substring(start, end + 1);
    }
}
