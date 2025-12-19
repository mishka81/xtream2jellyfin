package uk.humbkr.xtream2jellyfin.nameformat;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class CategoryNameFormat extends BaseNameFormat {

    public CategoryNameFormat(Map<String, String> regexPatterns) {
        super(regexPatterns);
    }

    public String format(String categoryName) {
        if (StringUtils.isBlank(categoryName)) {
            return "";
        }

        // Phase 1: Apply user-configured regex patterns
        String result = applyRegexPatterns(categoryName);

        // Phase 2: Apply Jellyfin character sanitization
        result = sanitizeForJellyfin(result);

        // Phase 3: Clean up empty brackets/parentheses and normalize whitespace
        return cleanupEmptyMarkers(result);
    }

}
