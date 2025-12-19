package uk.humbkr.xtream2jellyfin.metadata.nfo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "tvshow")
public class TvShowNfo {

    @JacksonXmlProperty(localName = "title")
    private String title;

    @JacksonXmlProperty(localName = "plot")
    private String plot;

    @JacksonXmlProperty(localName = "premiered")
    private String premiered;

    @JacksonXmlProperty(localName = "userrating")
    private Double userrating;

    @JacksonXmlProperty(localName = "uniqueid")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<UniqueId> uniqueids;

    @JacksonXmlProperty(localName = "genre")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<String> genres;

    @JacksonXmlProperty(localName = "actor")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Actor> actors;

    @JacksonXmlProperty(localName = "director")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<String> directors;

    @JacksonXmlProperty(localName = "runtime")
    private Integer runtime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UniqueId {
        @JacksonXmlProperty(isAttribute = true, localName = "type")
        private String type;

        @JacksonXmlProperty(isAttribute = true, localName = "default")
        private Boolean isDefault;

        @JacksonXmlProperty(localName = "")
        private String value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Actor {
        @JacksonXmlProperty(localName = "name")
        private String name;
    }
}
