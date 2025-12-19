package uk.humbkr.xtream2jellyfin.streamhandler.nameformat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uk.humbkr.xtream2jellyfin.nameformat.CategoryNameFormat;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CategoryNameFormatTest {

    private CategoryNameFormat formatter;

    @BeforeEach
    void setUp() {
        formatter = new CategoryNameFormat(null);
    }

    @Test
    void testFormat_simpleCategory() {
        // GIVEN / WHEN
        String result = formatter.format("Action Movies");

        // THEN
        assertEquals("Action Movies", result);
    }

    @Test
    void testFormat_jellyfinProhibitedChars() {
        // GIVEN / WHEN
        String result = formatter.format("Action<Movies>: \"Special|Chars?/Path\\Name&More*Stars");

        // THEN
        // < > : ? removed, " -> ', / \ | -> -, * -> _, & -> and, \t removed
        assertEquals("ActionMovies 'Special-Chars-Path-NameandMore_Stars", result);
    }

    @Test
    void testFormat_withLeadingTrailingSpaces() {
        // GIVEN / WHEN
        String result = formatter.format("  Drama  ");

        // THEN
        assertEquals("Drama", result);
    }

    @Test
    void testFormat_multipleSpacesNormalized() {
        // GIVEN / WHEN
        String result = formatter.format("Action    Movies    HD");

        // THEN
        assertEquals("Action Movies HD", result);
    }

    @Test
    void testFormat_emptyBracketsCleanup() {
        // GIVEN / WHEN
        String result = formatter.format("Movies [] () [  ] (  )");

        // THEN
        assertEquals("Movies", result);
    }

    @Test
    void testFormat_emptyBracketsWithHyphen() {
        // GIVEN / WHEN
        String result = formatter.format("Action [-] Movies");

        // THEN
        assertEquals("Action Movies", result);
    }

    @Test
    void testFormat_nullInput() {
        // GIVEN / WHEN
        String result = formatter.format(null);

        // THEN
        assertEquals("", result);
    }

    @Test
    void testFormat_emptyInput() {
        // GIVEN / WHEN
        String result = formatter.format("");

        // THEN
        assertEquals("", result);
    }

    @Test
    void testFormat_blankInput() {
        // GIVEN / WHEN
        String result = formatter.format("   ");

        // THEN
        assertEquals("", result);
    }

    @Test
    void testFormat_unicodeCharacters() {
        // GIVEN / WHEN
        String result = formatter.format("Cinéma Français Les Films");

        // THEN
        // Unicode chars preserved
        assertEquals("Cinéma Français Les Films", result);
    }

    @Test
    void testFormat_withUserRegexPatterns() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("\\[HD\\]", "");
        regexPatterns.put("\\(4K\\)", "");
        regexPatterns.put("_", " ");

        CategoryNameFormat customFormatter = new CategoryNameFormat(regexPatterns);

        // WHEN
        String result = customFormatter.format("Action_Movies_[HD]_(4K)");

        // THEN
        assertEquals("Action Movies", result);
    }

    @Test
    void testFormat_removeQualityTags() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("(?i)\\s*(HD|4K|UHD|FHD)", "");

        CategoryNameFormat customFormatter = new CategoryNameFormat(regexPatterns);

        // WHEN
        String result = customFormatter.format("Movies HD");

        // THEN
        assertEquals("Movies", result);
    }

    @Test
    void testFormat_complexStreamingServiceCategory() {
        // GIVEN / WHEN
        String result = formatter.format("ACTION ( NETFLIX| PRIME | HBO | APPLE TV+ | STARZ | PARAMOUNT+ )");

        // THEN
        // Pipes replaced with hyphens, spaces preserved
        assertEquals("ACTION ( NETFLIX- PRIME - HBO - APPLE TV+ - STARZ - PARAMOUNT+ )", result);
    }

    @Test
    void testFormat_streamingServiceCategoryWithRegex() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("\\s*\\([^)]*\\)", ""); // Remove content in parentheses

        CategoryNameFormat customFormatter = new CategoryNameFormat(regexPatterns);

        // WHEN
        String result = customFormatter.format("ACTION ( NETFLIX| PRIME | HBO | APPLE TV+ | STARZ | PARAMOUNT+ )");

        // THEN
        assertEquals("ACTION", result);
    }

    @Test
    void testFormat_pipesReplacedWithHyphens() {
        // GIVEN / WHEN
        String result = formatter.format("AVENTURE | FANTASTIQUE ( NETFLIX| PRIME | HBO | APPLE TV+ | STARZ | PARAMOUNT+ )");

        // THEN
        assertEquals("AVENTURE - FANTASTIQUE ( NETFLIX- PRIME - HBO - APPLE TV+ - STARZ - PARAMOUNT+ )", result);
    }

    @Test
    void testFormat_ampersandReplacedWithAnd() {
        // GIVEN / WHEN
        String result = formatter.format("BRAQUAGE & ARNAQUE ( NETFLIX| PRIME | HBO | APPLE TV+ | STARZ | PARAMOUNT+ )");

        // THEN
        assertEquals("BRAQUAGE and ARNAQUE ( NETFLIX- PRIME - HBO - APPLE TV+ - STARZ - PARAMOUNT+ )", result);
    }

    @Test
    void testFormat_multipleAmpersands() {
        // GIVEN / WHEN
        String result = formatter.format("MAFIA & GANG");

        // THEN
        assertEquals("MAFIA and GANG", result);
    }

    @Test
    void testFormat_animationCategory() {
        // GIVEN / WHEN
        String result = formatter.format("ANIMATION - ENFANT ( NETFLIX| PRIME | HBO | APPLE TV+ | STARZ | PARAMOUNT+ )");

        // THEN
        assertEquals("ANIMATION - ENFANT ( NETFLIX- PRIME - HBO - APPLE TV+ - STARZ - PARAMOUNT+ )", result);
    }

    @Test
    void testFormat_koreanSeries() {
        // GIVEN / WHEN
        String result = formatter.format("CORÉENNE | KOREA SERIES ( NETFLIX| PRIME | HBO | APPLE TV+ | STARZ | PARAMOUNT+ )");

        // THEN
        assertEquals("CORÉENNE - KOREA SERIES ( NETFLIX- PRIME - HBO - APPLE TV+ - STARZ - PARAMOUNT+ )", result);
    }

    @Test
    void testFormat_multipleRegexPatterns() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("\\[.*?\\]", ""); // Remove square brackets content
        regexPatterns.put("\\(.*?\\)", ""); // Remove parentheses content
        regexPatterns.put("_", " "); // Replace underscores with spaces

        CategoryNameFormat customFormatter = new CategoryNameFormat(regexPatterns);

        // WHEN
        String result = customFormatter.format("Action_Movies_[HD]_(Premium)");

        // THEN
        assertEquals("Action Movies", result);
    }

    @Test
    void testFormat_caseInsensitiveRegex() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("(?i)\\s+(hd|4k|uhd)\\s*", " "); // Case-insensitive quality removal

        CategoryNameFormat customFormatter = new CategoryNameFormat(regexPatterns);

        // WHEN
        String result = customFormatter.format("Movies HD and Series 4K");

        // THEN
        assertEquals("Movies and Series", result);
    }

    @Test
    void testFormat_removeLeadingTrailingHyphens() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("^-+\\s*", ""); // Remove leading hyphens
        regexPatterns.put("\\s*-+$", ""); // Remove trailing hyphens

        CategoryNameFormat customFormatter = new CategoryNameFormat(regexPatterns);

        // WHEN
        String result = customFormatter.format("- Action Movies -");

        // THEN
        assertEquals("Action Movies", result);
    }

    @Test
    void testFormat_spaceNormalization() {
        // GIVEN / WHEN
        String result = formatter.format("Action  Movies  With  Spaces");

        // THEN
        // Multiple spaces normalized to single space
        assertEquals("Action Movies With Spaces", result);
    }

    @Test
    void testFormat_tabCharactersRemoved() {
        // GIVEN / WHEN
        String result = formatter.format("Action\tMovies\tWith\tTabs");

        // THEN
        // Tabs removed, spaces normalized
        assertEquals("ActionMoviesWithTabs", result);
    }

    @Test
    void testFormat_colonAndQuotesInCategory() {
        // GIVEN / WHEN
        String result = formatter.format("Action: \"The Best Movies\"");

        // THEN
        // : removed, " -> '
        assertEquals("Action 'The Best Movies'", result);
    }

    @Test
    void testFormat_realWorldScenario() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("\\[.*?\\]", ""); // Remove square brackets
        regexPatterns.put("(?i)\\s+(hd|4k)", ""); // Remove quality tags

        CategoryNameFormat customFormatter = new CategoryNameFormat(regexPatterns);

        // WHEN
        String result = customFormatter.format("MOVIES [PREMIUM] 4K");

        // THEN
        assertEquals("MOVIES", result);
    }

    @Test
    void testFormat_replaceDotWithSpace() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("\\.", " ");

        CategoryNameFormat customFormatter = new CategoryNameFormat(regexPatterns);

        // WHEN
        String result = customFormatter.format("Action.Movies.HD");

        // THEN
        assertEquals("Action Movies HD", result);
    }

    @Test
    void testFormat_removeNumbers() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("\\d+", ""); // Remove all numbers

        CategoryNameFormat customFormatter = new CategoryNameFormat(regexPatterns);

        // WHEN
        String result = customFormatter.format("Movies 2024 HD");

        // THEN
        assertEquals("Movies HD", result);
    }

    @Test
    void testFormat_capitalizeWords() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("(?i)action", "ACTION");
        regexPatterns.put("(?i)movies", "MOVIES");

        CategoryNameFormat customFormatter = new CategoryNameFormat(regexPatterns);

        // WHEN
        String result = customFormatter.format("action movies");

        // THEN
        assertEquals("ACTION MOVIES", result);
    }

}
