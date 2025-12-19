package uk.humbkr.xtream2jellyfin.metadata.nfo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "episodedetails")
public class EpisodeNfo {

    @JacksonXmlProperty(localName = "title")
    private String title;

    @JacksonXmlProperty(localName = "season")
    private Integer season;

    @JacksonXmlProperty(localName = "episode")
    private Integer episode;

    @JacksonXmlProperty(localName = "aired")
    private String aired;

    @JacksonXmlProperty(localName = "plot")
    private String plot;

    @JacksonXmlProperty(localName = "userrating")
    private Double userrating;

    @JacksonXmlProperty(localName = "director")
    private String director;
}
