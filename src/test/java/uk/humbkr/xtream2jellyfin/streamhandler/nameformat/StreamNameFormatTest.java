package uk.humbkr.xtream2jellyfin.streamhandler.nameformat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uk.humbkr.xtream2jellyfin.nameformat.StreamNameFormat;
import uk.humbkr.xtream2jellyfin.nameformat.StreamNameFormatContext;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StreamNameFormatTest {

    private static final String DEFAULT_TEMPLATE = "${name} (${year}) [${externalProviderId}-${externalId}]";

    private StreamNameFormat formatter;

    @BeforeEach
    void setUp() {
        formatter = new StreamNameFormat(DEFAULT_TEMPLATE, null);
    }

    @Test
    void testFormat_withNameOnly() {
        // GIVEN
        StreamNameFormatContext context = StreamNameFormatContext.builder().build();

        // WHEN
        String result = formatter.format("Test Movie", context);

        // THEN
        assertEquals("Test Movie", result);
    }

    @Test
    void testFormat_withNameAndYear() {
        // GIVEN
        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .year("2024")
                .build();

        // WHEN
        String result = formatter.format("Test Movie", context);

        // THEN
        assertEquals("Test Movie (2024)", result);
    }

    @Test
    void testFormat_withNameYearAndExternalId() {
        // GIVEN
        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .year("2024")
                .externalProviderId("tmdbid")
                .externalId("12345")
                .build();

        // WHEN
        String result = formatter.format("Test Movie", context);

        // THEN
        assertEquals("Test Movie (2024) [tmdbid-12345]", result);
    }

    @Test
    void testFormat_withExternalIdOnly() {
        // GIVEN
        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .externalProviderId("imdbid")
                .externalId("tt1234567")
                .build();

        // WHEN
        String result = formatter.format("Test Movie", context);

        // THEN
        assertEquals("Test Movie [imdbid-tt1234567]", result);
    }

    @Test
    void testFormat_jellyfinProhibitedChars() {
        // GIVEN
        StreamNameFormatContext context = StreamNameFormatContext.builder().build();

        // WHEN
        String result = formatter.format("Test<Movie>: \"With|Special*Chars?/Path\\Name&More\tTabs", context);

        // THEN
        // < > : ? removed, " -> ', / \ | -> -, * -> _, & -> and, \t removed
        assertEquals("TestMovie 'With-Special_Chars-Path-NameandMoreTabs", result);
    }

    @Test
    void testFormat_userRegexPatterns() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("\\[HD\\]", "");
        regexPatterns.put("\\(MULTI\\)", "");
        regexPatterns.put("_", " ");

        StreamNameFormat customFormatter = new StreamNameFormat(DEFAULT_TEMPLATE, regexPatterns);
        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .year("2024")
                .build();

        // WHEN
        String result = customFormatter.format("Test_Movie_[HD]_(MULTI)", context);

        // THEN
        assertEquals("Test Movie (2024)", result);
    }

    @Test
    void testFormat_emptyBracketCleanup() {
        // GIVEN
        StreamNameFormatContext context = StreamNameFormatContext.builder().build();

        // WHEN
        String result = formatter.format("Movie Name [] () [  ] (  )", context);

        // THEN
        assertEquals("Movie Name", result);
    }

    @Test
    void testFormat_multipleSpacesNormalized() {
        // GIVEN
        StreamNameFormatContext context = StreamNameFormatContext.builder().build();

        // WHEN
        String result = formatter.format("Movie    Name    With     Spaces", context);

        // THEN
        assertEquals("Movie Name With Spaces", result);
    }

    @Test
    void testFormat_nullInput() {
        // GIVEN
        StreamNameFormatContext context = StreamNameFormatContext.builder().build();

        // WHEN
        String result = formatter.format(null, context);

        // THEN
        assertEquals("", result);
    }

    @Test
    void testFormat_emptyInput() {
        // GIVEN
        StreamNameFormatContext context = StreamNameFormatContext.builder().build();

        // WHEN
        String result = formatter.format("", context);

        // THEN
        assertEquals("", result);
    }

    @Test
    void testFormat_blankInput() {
        // GIVEN
        StreamNameFormatContext context = StreamNameFormatContext.builder().build();

        // WHEN
        String result = formatter.format("   ", context);

        // THEN
        assertEquals("", result);
    }

    @Test
    void testFormat_nullContext() {
        // GIVEN / WHEN
        String result = formatter.format("Test Movie", null);

        // THEN
        assertEquals("Test Movie", result);
    }

    @Test
    void testFormat_complexScenario() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("\\[.*?\\]", ""); // Remove all content in square brackets
        regexPatterns.put("^(.*?)\\s*-\\s*.*", "$1"); // Remove everything after first hyphen

        StreamNameFormat customFormatter = new StreamNameFormat(DEFAULT_TEMPLATE, regexPatterns);
        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .year("2008")
                .externalProviderId("tvdbid")
                .externalId("67890")
                .build();

        // WHEN
        String result = customFormatter.format("Breaking Bad [HD] - Complete Series", context);

        // THEN
        assertEquals("Breaking Bad (2008) [tvdbid-67890]", result);
    }

    @Test
    void testFormat_withLeadingTrailingSpaces() {
        // GIVEN
        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .year("2024")
                .build();

        // WHEN
        String result = formatter.format("  Test Movie  ", context);

        // THEN
        assertEquals("Test Movie (2024)", result);
    }

    @Test
    void testFormat_unicodeCharacters() {
        // GIVEN
        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .year("2024")
                .build();

        // WHEN
        String result = formatter.format("Café Müller: El Niño", context);

        // THEN
        // : is replaced, but unicode chars preserved
        assertEquals("Café Müller El Niño (2024)", result);
    }

    @Test
    void testFormat_emptyBracketsWithHyphen() {
        // GIVEN
        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .year("2024")
                .build();

        // WHEN
        // This simulates what happens when externalProviderId and externalId are empty
        String result = formatter.format("Movie Name [-]", context);

        // THEN
        assertEquals("Movie Name (2024)", result);
    }

    @Test
    void testFormat_customTemplateSimple() {
        // GIVEN
        StreamNameFormat simpleFormatter = new StreamNameFormat("${name}", null);
        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .year("2024")
                .externalProviderId("tmdbid")
                .externalId("12345")
                .build();

        // WHEN
        String result = simpleFormatter.format("Test Movie", context);

        // THEN
        assertEquals("Test Movie", result);
    }

    @Test
    void testFormat_customTemplateWithYearOnly() {
        // GIVEN
        StreamNameFormat yearOnlyFormatter = new StreamNameFormat("${name} (${year})", null);
        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .year("2024")
                .externalProviderId("tmdbid")
                .externalId("12345")
                .build();

        // WHEN
        String result = yearOnlyFormatter.format("Test Movie", context);

        // THEN
        assertEquals("Test Movie (2024)", result);
    }

    @Test
    void testFormat_regexWithSpecialCharacters() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("\\.", " "); // Replace dots with spaces
        regexPatterns.put("([a-z])([A-Z])", "$1 $2"); // Add space between camelCase

        StreamNameFormat customFormatter = new StreamNameFormat(DEFAULT_TEMPLATE, regexPatterns);
        StreamNameFormatContext context = StreamNameFormatContext.builder().build();

        // WHEN
        String result = customFormatter.format("test.movie.MyTitle", context);

        // THEN
        assertEquals("test movie My Title", result);
    }

    @Test
    void testFormat_multipleSequentialRegexPatterns() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("\\[.*?\\]", ""); // Remove all square brackets and content
        regexPatterns.put("\\(.*?\\)", ""); // Remove all parentheses and content
        regexPatterns.put("_", " "); // Replace underscores with spaces
        regexPatterns.put("\\s+", " "); // Normalize spaces

        StreamNameFormat customFormatter = new StreamNameFormat(DEFAULT_TEMPLATE, regexPatterns);
        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .year("2024")
                .build();

        // WHEN
        String result = customFormatter.format("Movie_Name_[1080p]_(BluRay)", context);

        // THEN
        assertEquals("Movie Name (2024)", result);
    }

    @Test
    void testFormat_regexWithCaptureGroups() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        // Remove everything after year in parentheses: "Movie (2024) Extra Stuff" -> "Movie (2024)"
        regexPatterns.put("(.*?\\(\\d{4}\\)).*", "$1");

        StreamNameFormat customFormatter = new StreamNameFormat("${name}", regexPatterns);
        StreamNameFormatContext context = StreamNameFormatContext.builder().build();

        // WHEN
        String result = customFormatter.format("The Matrix (1999) Reloaded Edition", context);

        // THEN
        assertEquals("The Matrix (1999)", result);
    }

    @Test
    void testFormat_regexRemoveVersionNumbers() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("\\s+[vV]\\d+(\\.\\d+)*", ""); // Remove version numbers like v1.0, V2.3.4

        StreamNameFormat customFormatter = new StreamNameFormat(DEFAULT_TEMPLATE, regexPatterns);
        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .year("2024")
                .build();

        // WHEN
        String result = customFormatter.format("Software Name v2.1.0", context);

        // THEN
        assertEquals("Software Name (2024)", result);
    }

    @Test
    void testFormat_regexRemoveQualityTags() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("(?i)\\s*(720p|1080p|4k|uhd|hdr|bluray|webrip|web-dl)", "");

        StreamNameFormat customFormatter = new StreamNameFormat(DEFAULT_TEMPLATE, regexPatterns);
        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .year("2024")
                .externalProviderId("tmdbid")
                .externalId("12345")
                .build();

        // WHEN
        String result = customFormatter.format("Movie Name 1080p BluRay", context);

        // THEN
        assertEquals("Movie Name (2024) [tmdbid-12345]", result);
    }

    @Test
    void testFormat_regexComplexRealWorldScenario() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("\\[.*?\\]", ""); // Remove square brackets content
        regexPatterns.put("\\s*-\\s+.*$", ""); // Remove everything from hyphen onwards
        regexPatterns.put("(?i)(1080p|720p|4k|BluRay|web-dl|webrip|x264|x265|hevc)", ""); // Remove quality/codec

        StreamNameFormat customFormatter = new StreamNameFormat(DEFAULT_TEMPLATE, regexPatterns);
        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .year("2019")
                .externalProviderId("imdbid")
                .externalId("tt1234567")
                .build();

        // WHEN
        String result = customFormatter.format("Awesome Movie [2019] 1080p BluRay x264 - RELEASE", context);

        // THEN
        assertEquals("Awesome Movie (2019) [imdbid-tt1234567]", result);
    }

    @Test
    void testFormat_regexMatchesNothing() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("ZZZZZZZ", "replaced"); // Pattern that won't match

        StreamNameFormat customFormatter = new StreamNameFormat(DEFAULT_TEMPLATE, regexPatterns);
        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .year("2024")
                .build();

        // WHEN
        String result = customFormatter.format("Movie Name", context);

        // THEN
        assertEquals("Movie Name (2024)", result);
    }

    @Test
    void testFormat_regexEmptyReplacement() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("(?i)\\s+extended\\s+edition", ""); // Remove "extended edition"
        regexPatterns.put("(?i)\\s+director'?s\\s+cut", ""); // Remove "director's cut"

        StreamNameFormat customFormatter = new StreamNameFormat(DEFAULT_TEMPLATE, regexPatterns);
        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .year("2003")
                .build();

        // WHEN
        String result = customFormatter.format("Lord of the Rings Extended Edition", context);

        // THEN
        assertEquals("Lord of the Rings (2003)", result);
    }

    @Test
    void testFormat_regexBeforeJellyfinSanitization() {
        // GIVEN - Test that regex runs before Jellyfin character replacement
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put(":", " -"); // Replace colons with space-dash

        StreamNameFormat customFormatter = new StreamNameFormat(DEFAULT_TEMPLATE, regexPatterns);
        StreamNameFormatContext context = StreamNameFormatContext.builder().build();

        // WHEN
        String result = customFormatter.format("Movie: The Sequel", context);

        // THEN
        // Regex replaces : with " -", but Jellyfin sanitization would have removed :
        assertEquals("Movie - The Sequel", result);
    }

    @Test
    void testFormat_regexWithCaptureGroupsAndSequence() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        // Remove leading/trailing hyphens and replace internal hyphens with spaces
        regexPatterns.put("^-+", "");
        regexPatterns.put("-+$", "");
        regexPatterns.put("-", " ");

        StreamNameFormat customFormatter = new StreamNameFormat(DEFAULT_TEMPLATE, regexPatterns);
        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .year("2024")
                .build();

        // WHEN
        String result = customFormatter.format("The-Matrix-Resurrections", context);

        // THEN
        assertEquals("The Matrix Resurrections (2024)", result);
    }

    @Test
    void testFormat_regexMultipleReplacements() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("a", "A"); // Replace all 'a' with 'A'
        regexPatterns.put("e", "E"); // Replace all 'e' with 'E'

        StreamNameFormat customFormatter = new StreamNameFormat("${name}", regexPatterns);
        StreamNameFormatContext context = StreamNameFormatContext.builder().build();

        // WHEN
        String result = customFormatter.format("test case", context);

        // THEN
        assertEquals("tEst cAsE", result);
    }

    @Test
    void testFormat_regexRemoveLeadingTrailingMarkers() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("^[-\\s]+", ""); // Remove leading hyphens and spaces
        regexPatterns.put("[-\\s]+$", ""); // Remove trailing hyphens and spaces

        StreamNameFormat customFormatter = new StreamNameFormat(DEFAULT_TEMPLATE, regexPatterns);
        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .year("2024")
                .build();

        // WHEN
        String result = customFormatter.format("- Movie Name -", context);

        // THEN
        assertEquals("Movie Name (2024)", result);
    }

    @Test
    void testFormat_regexCaseInsensitive() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        regexPatterns.put("(?i)\\b(the|a|an)\\b\\s+", ""); // Remove articles case-insensitively

        StreamNameFormat customFormatter = new StreamNameFormat(DEFAULT_TEMPLATE, regexPatterns);
        StreamNameFormatContext context = StreamNameFormatContext.builder().build();

        // WHEN
        String result = customFormatter.format("The Matrix An Epic Movie", context);

        // THEN
        assertEquals("Matrix Epic Movie", result);
    }

    @Test
    void testFormat_regexGreedyVsLazy() {
        // GIVEN
        Map<String, String> regexPatterns = new HashMap<>();
        // Lazy match: remove content between first [ and first ]
        regexPatterns.put("\\[.*?\\]", "");

        StreamNameFormat customFormatter = new StreamNameFormat("${name}", regexPatterns);
        StreamNameFormatContext context = StreamNameFormatContext.builder().build();

        // WHEN
        String result = customFormatter.format("Movie [Tag1] Name [Tag2]", context);

        // THEN
        // Lazy matching removes each bracket pair separately
        assertEquals("Movie Name", result);
    }

}
