package uk.humbkr.xtream2jellyfin.metadata;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.humbkr.xtream2jellyfin.common.XmlUtils;
import uk.humbkr.xtream2jellyfin.metadata.nfo.EpisodeNfo;
import uk.humbkr.xtream2jellyfin.metadata.nfo.MovieNfo;
import uk.humbkr.xtream2jellyfin.metadata.nfo.TvShowNfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class NfoGenerator {

    /**
     * Generate NFO XML content for a TV show
     *
     * @param seriesData The series metadata from Xtream
     * @return NFO XML content as String, or null if generation fails
     */
    public static String generateTvShowNfo(Map<String, Object> seriesData) {
        try {
            TvShowNfo nfo = buildTvShowNfo(seriesData);
            return XmlUtils.getXmlMapper().writeValueAsString(nfo);
        } catch (JsonProcessingException e) {
            log.error("Failed to generate TV show NFO", e);
            return null;
        }
    }

    /**
     * Generate NFO XML content for an episode
     *
     * @param episodeData The episode metadata from Xtream
     * @return NFO XML content as String, or null if generation fails
     */
    public static String generateEpisodeNfo(Map<String, Object> episodeData) {
        try {
            EpisodeNfo nfo = buildEpisodeNfo(episodeData);
            return XmlUtils.getXmlMapper().writeValueAsString(nfo);
        } catch (JsonProcessingException e) {
            log.error("Failed to generate episode NFO", e);
            return null;
        }
    }

    /**
     * Generate NFO XML content for a movie
     *
     * @param movieData The movie metadata from Xtream
     * @return NFO XML content as String, or null if generation fails
     */
    public static String generateMovieNfo(Map<String, Object> movieData) {
        try {
            MovieNfo nfo = buildMovieNfo(movieData);
            return XmlUtils.getXmlMapper().writeValueAsString(nfo);
        } catch (JsonProcessingException e) {
            log.error("Failed to generate movie NFO", e);
            return null;
        }
    }

    private static TvShowNfo buildTvShowNfo(Map<String, Object> seriesData) {
        TvShowNfo.TvShowNfoBuilder builder = TvShowNfo.builder();

        // Title
        String title = getString(seriesData, "name");
        if (StringUtils.isNotBlank(title)) {
            builder.title(cleanTitle(title));
        }

        // Plot
        String plot = getString(seriesData, "plot");
        if (StringUtils.isNotBlank(plot)) {
            builder.plot(plot);
        }

        // Premiered
        String premiered = getString(seriesData, "releaseDate");
        if (premiered == null) {
            premiered = getString(seriesData, "release_date");
        }
        if (StringUtils.isNotBlank(premiered)) {
            builder.premiered(premiered);
        }

        // Rating
        String rating = getString(seriesData, "rating");
        if (StringUtils.isNotBlank(rating)) {
            try {
                builder.userrating(Double.parseDouble(rating));
            } catch (NumberFormatException e) {
                log.debug("Failed to parse rating: {}", rating);
            }
        }

        // Unique IDs
        List<TvShowNfo.UniqueId> uniqueIds = new ArrayList<>();
        String tmdbId = getString(seriesData, "tmdb");
        if (StringUtils.isNotBlank(tmdbId)) {
            uniqueIds.add(TvShowNfo.UniqueId.builder()
                    .type("tmdb")
                    .isDefault(true)
                    .value(tmdbId)
                    .build());
        }
        if (!uniqueIds.isEmpty()) {
            builder.uniqueids(uniqueIds);
        }

        // Genres
        String genre = getString(seriesData, "genre");
        if (StringUtils.isNotBlank(genre)) {
            List<String> genres = Arrays.stream(genre.split("/"))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            if (!genres.isEmpty()) {
                builder.genres(genres);
            }
        }

        // Actors
        String cast = getString(seriesData, "cast");
        if (StringUtils.isNotBlank(cast)) {
            List<TvShowNfo.Actor> actors = Arrays.stream(cast.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .map(name -> TvShowNfo.Actor.builder().name(name).build())
                    .collect(Collectors.toList());
            if (!actors.isEmpty()) {
                builder.actors(actors);
            }
        }

        // Directors
        String director = getString(seriesData, "director");
        if (StringUtils.isNotBlank(director)) {
            List<String> directors = Arrays.stream(director.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            if (!directors.isEmpty()) {
                builder.directors(directors);
            }
        }

        // Runtime
        String episodeRunTime = getString(seriesData, "episode_run_time");
        if (StringUtils.isNotBlank(episodeRunTime)) {
            try {
                builder.runtime(Integer.parseInt(episodeRunTime));
            } catch (NumberFormatException e) {
                log.debug("Failed to parse runtime: {}", episodeRunTime);
            }
        }

        return builder.build();
    }

    private static EpisodeNfo buildEpisodeNfo(Map<String, Object> episodeData) {
        EpisodeNfo.EpisodeNfoBuilder builder = EpisodeNfo.builder();

        // Title
        String title = getString(episodeData, "title");
        if (StringUtils.isNotBlank(title)) {
            builder.title(extractEpisodeTitle(title));
        }

        // Season
        Object seasonObj = episodeData.get("season");
        if (seasonObj != null) {
            try {
                builder.season(Integer.parseInt(String.valueOf(seasonObj)));
            } catch (NumberFormatException e) {
                log.debug("Failed to parse season: {}", seasonObj);
            }
        }

        // Episode
        Object episodeNumObj = episodeData.get("episode_num");
        if (episodeNumObj != null) {
            try {
                builder.episode(Integer.parseInt(String.valueOf(episodeNumObj)));
            } catch (NumberFormatException e) {
                log.debug("Failed to parse episode number: {}", episodeNumObj);
            }
        }

        // Get info map
        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) episodeData.get("info");
        if (info != null) {
            // Aired date
            String airDate = getString(info, "air_date");
            if (StringUtils.isNotBlank(airDate)) {
                builder.aired(airDate);
            }

            // Plot
            String plot = getString(info, "plot");
            if (StringUtils.isNotBlank(plot)) {
                builder.plot(plot);
            }

            // Rating
            Object ratingObj = info.get("rating");
            if (ratingObj != null) {
                try {
                    builder.userrating(Double.parseDouble(String.valueOf(ratingObj)));
                } catch (NumberFormatException e) {
                    log.debug("Failed to parse rating: {}", ratingObj);
                }
            }

            // Director/Crew
            String crew = getString(info, "crew");
            if (StringUtils.isNotBlank(crew)) {
                builder.director(crew);
            }
        }

        return builder.build();
    }

    private static MovieNfo buildMovieNfo(Map<String, Object> movieData) {
        MovieNfo.MovieNfoBuilder builder = MovieNfo.builder();

        // Title
        String name = getString(movieData, "name");
        if (StringUtils.isNotBlank(name)) {
            String cleanedTitle = cleanTitle(name);
            builder.title(cleanedTitle);
            builder.originaltitle(cleanedTitle);
        }

        // Plot
        String plot = getString(movieData, "plot");
        if (StringUtils.isNotBlank(plot)) {
            builder.plot(plot);
        }

        // Premiered and year
        String premiered = getString(movieData, "releaseDate");
        if (premiered == null) {
            premiered = getString(movieData, "release_date");
        }
        if (StringUtils.isNotBlank(premiered)) {
            builder.premiered(premiered);
            if (premiered.length() >= 4) {
                builder.year(premiered.substring(0, 4));
            }
        }

        // Rating
        String rating = getString(movieData, "rating");
        if (StringUtils.isNotBlank(rating)) {
            try {
                builder.userrating(Double.parseDouble(rating));
            } catch (NumberFormatException e) {
                log.debug("Failed to parse rating: {}", rating);
            }
        }

        // Unique IDs
        List<MovieNfo.UniqueId> uniqueIds = new ArrayList<>();

        String tmdbId = getString(movieData, "tmdb");
        if (StringUtils.isNotBlank(tmdbId)) {
            uniqueIds.add(MovieNfo.UniqueId.builder()
                    .type("tmdb")
                    .isDefault(true)
                    .value(tmdbId)
                    .build());
        }

        String imdbId = getString(movieData, "imdb_id");
        if (imdbId == null) {
            imdbId = getString(movieData, "imdb");
        }
        if (StringUtils.isNotBlank(imdbId)) {
            uniqueIds.add(MovieNfo.UniqueId.builder()
                    .type("imdb")
                    .isDefault(false)
                    .value(imdbId)
                    .build());
        }

        if (!uniqueIds.isEmpty()) {
            builder.uniqueids(uniqueIds);
        }

        // Genres
        String genre = getString(movieData, "genre");
        if (StringUtils.isNotBlank(genre)) {
            List<String> genres = Arrays.stream(genre.split("/"))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            if (!genres.isEmpty()) {
                builder.genres(genres);
            }
        }

        // Actors
        String cast = getString(movieData, "cast");
        if (StringUtils.isNotBlank(cast)) {
            List<MovieNfo.Actor> actors = Arrays.stream(cast.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .map(actorName -> MovieNfo.Actor.builder().name(actorName).build())
                    .collect(Collectors.toList());
            if (!actors.isEmpty()) {
                builder.actors(actors);
            }
        }

        // Directors
        String director = getString(movieData, "director");
        if (StringUtils.isNotBlank(director)) {
            List<String> directors = Arrays.stream(director.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            if (!directors.isEmpty()) {
                builder.directors(directors);
            }
        }

        // Runtime
        String runtime = getString(movieData, "runtime");
        if (StringUtils.isNotBlank(runtime)) {
            try {
                builder.runtime(Integer.parseInt(runtime));
            } catch (NumberFormatException e) {
                log.debug("Failed to parse runtime: {}", runtime);
            }
        }

        return builder.build();
    }

    /**
     * Clean title by removing metadata tags like (MULTI), [4K], etc.
     */
    private static String cleanTitle(String title) {
        if (title == null) {
            return null;
        }
        // Remove patterns like (MULTI), [4K], |IMAX UHD|, etc.
        title = title.replaceAll("\\s*\\([^)]*\\)\\s*$", ""); // Remove trailing (...)
        title = title.replaceAll("\\s*\\[[^]]*\\]\\s*$", ""); // Remove trailing [...]
        title = title.replaceAll("^\\|[^|]*\\|\\s*", "");     // Remove leading |...|
        return title.trim();
    }

    /**
     * Extract episode title from full title format like "Series Name - S01E01 - Episode Title"
     */
    private static String extractEpisodeTitle(String fullTitle) {
        if (fullTitle == null) {
            return null;
        }
        // Try to extract title after the last dash
        int lastDash = fullTitle.lastIndexOf(" - ");
        if (lastDash > 0 && lastDash < fullTitle.length() - 3) {
            String episodeTitle = fullTitle.substring(lastDash + 3);
            if (StringUtils.isNotBlank(episodeTitle)) {
                return episodeTitle.trim();
            }
        }
        return fullTitle;
    }

    /**
     * Get string value from map, handling both direct keys and nested "info" object
     */
    private static String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value != null) {
            return String.valueOf(value);
        }

        // Try to get from nested "info" object
        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) map.get("info");
        if (info != null) {
            value = info.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }

        return null;
    }

}
