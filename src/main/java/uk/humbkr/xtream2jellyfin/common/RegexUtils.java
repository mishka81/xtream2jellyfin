package uk.humbkr.xtream2jellyfin.common;

import lombok.NoArgsConstructor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Thread-safe utility for regex operations with pattern caching.
 * Avoids redundant pattern compilation overhead by caching patterns on first use.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class RegexUtils {

    private static final ConcurrentHashMap<String, Pattern> patternCache = new ConcurrentHashMap<>();

    /**
     * Get a compiled Pattern from cache, or compile and cache if not present.
     *
     * @param regex the regex pattern string
     * @return compiled Pattern object
     */
    private static Pattern getPattern(String regex) {
        return patternCache.computeIfAbsent(regex, Pattern::compile);
    }

    /**
     * Replace all occurrences of regex pattern with replacement string.
     *
     * @param text        the text to search
     * @param regex       the regex pattern
     * @param replacement the replacement string
     * @return the resulting string
     */
    public static String replaceAll(String text, String regex, String replacement) {
        return getPattern(regex).matcher(text).replaceAll(replacement);
    }

    /**
     * Check if the entire text matches the regex pattern.
     *
     * @param text  the text to check
     * @param regex the regex pattern
     * @return true if the text matches
     */
    public static boolean matches(String text, String regex) {
        return getPattern(regex).matcher(text).matches();
    }

    /**
     * Split text around matches of the regex pattern.
     *
     * @param text  the text to split
     * @param regex the regex pattern
     * @return the array of strings computed by splitting
     */
    public static String[] split(String text, String regex) {
        return getPattern(regex).split(text);
    }

}
