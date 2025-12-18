package uk.humbkr.xtream2jellyfin.streamhandler.nameformat;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import java.util.HashMap;
import java.util.Map;

public class StreamNameFormat extends BaseNameFormat {

    private final String patternTemplate;

    public StreamNameFormat(String patternTemplate, Map<String, String> regexPatterns) {
        super(regexPatterns);
        this.patternTemplate = patternTemplate;
    }

    public String format(String streamName, StreamNameFormatContext context) {
        if (StringUtils.isBlank(streamName)) {
            return "";
        }

        // Phase 1: Apply user-configured regex patterns
        streamName = applyRegexPatterns(streamName);

        // Phase 2: Replace template placeholders
        streamName = applyTemplate(streamName, context);

        // Phase 3: Apply Jellyfin character sanitization
        streamName = sanitizeForJellyfin(streamName);

        // Phase 4: Clean up empty brackets/parentheses and normalize whitespace
        return cleanupEmptyMarkers(streamName);
    }

    private String applyTemplate(String cleanedName, StreamNameFormatContext context) {
        // Create placeholder value map with default empty strings
        // This keeps templates simple (no need for ${var:-} syntax) for end users
        Map<String, String> placeholderValues = new HashMap<>(4);
        placeholderValues.put("name", cleanedName);
        placeholderValues.put("year", "");
        placeholderValues.put("externalProviderId", "");
        placeholderValues.put("externalId", "");

        if (context != null) {
            putIfNotBlank(placeholderValues, "year", context.getYear());
            putIfNotBlank(placeholderValues, "externalProviderId", context.getExternalProviderId());
            putIfNotBlank(placeholderValues, "externalId", context.getExternalId());
        }

        // Use StringSubstitutor to replace placeholders
        return new StringSubstitutor(placeholderValues).replace(patternTemplate);
    }

    private void putIfNotBlank(Map<String, String> map, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            map.put(key, value);
        }
    }

}
