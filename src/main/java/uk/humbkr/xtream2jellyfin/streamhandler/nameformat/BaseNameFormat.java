package uk.humbkr.xtream2jellyfin.streamhandler.nameformat;

import uk.humbkr.xtream2jellyfin.util.RegexUtils;

import java.util.Map;

abstract class BaseNameFormat {

    private static final Map<String, String> JELLYFIN_PROHIBITED_CHARS = Map.ofEntries(
            Map.entry("<", ""),
            Map.entry(">", ""),
            Map.entry(":", ""),
            Map.entry("\"", "'"),
            Map.entry("/", "-"),
            Map.entry("\\", "-"),
            Map.entry("|", "-"),
            Map.entry("?", ""),
            Map.entry("*", "_"),
            Map.entry("&", "and"),
            Map.entry("\t", "")
    );

    protected final Map<String, String> regexPatterns;

    protected BaseNameFormat(Map<String, String> regexPatterns) {
        this.regexPatterns = regexPatterns != null ? regexPatterns : Map.of();
    }

    protected String applyRegexPatterns(String text) {
        if (regexPatterns.isEmpty()) {
            return text.trim();
        }

        String result = text;
        for (Map.Entry<String, String> entry : regexPatterns.entrySet()) {
            result = RegexUtils.replaceAll(result, entry.getKey(), entry.getValue());
        }
        return result.trim();
    }

    protected String sanitizeForJellyfin(String text) {
        String result = text;
        for (Map.Entry<String, String> entry : JELLYFIN_PROHIBITED_CHARS.entrySet()) {
            if (result.contains(entry.getKey())) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    protected String cleanupEmptyMarkers(String text) {
        // Remove empty brackets: [], ()
        String result = text;

        // Remove brackets with only hyphens/spaces: [-], [ - ], etc.
        result = RegexUtils.replaceAll(result, "\\[\\s*-\\s*\\]", "");

        // Remove empty square brackets with optional space
        result = RegexUtils.replaceAll(result, "\\[\\s*\\]", "");

        // Remove empty parentheses with optional space
        result = RegexUtils.replaceAll(result, "\\(\\s*\\)", "");

        // Normalize multiple spaces to single space
        result = RegexUtils.replaceAll(result, "\\s+", " ");

        // Trim leading/trailing whitespace
        return result.trim();
    }

}
