# Xtream2Jellyfin

IPTV stream converter for Jellyfin.

**<!> Work in progress**

## Overview

`xtream2jellyfin` is a Java-based IPTV stream converter that bridges Xtream Codes IPTV providers with media servers like Jellyfin. It converts IPTV content from Xtream API endpoints into organized file structures (STRM and JSON files) that media servers can consume.

## Features

- multi-provider support: handle multiple IPTV providers simultaneously
- content types:
  - live TV channels with M3U playlists and EPG (Electronic Program Guide)
  - movies (VOD) with .strm files and optional metadata
  - TV series with organized season/episode structures
- flexible configuration: per-media-type settings with regex-based name cleaning and category filtering
- Jellyfin integration: automatic library refresh after content updates

## Requirements

- Java 21 or higher
- Maven 3.6 or higher
- access to Xtream Codes IPTV provider(s)
- Jellyfin server (optional, for library refresh)

## Installation

### Build From Source

```bash
mvn clean package
```

This will create a JAR file with all dependencies in `target/xtream2jellyfin-1.0.0-jar-with-dependencies.jar`.

## Configuration

1. Copy the example configuration file:
```bash
cp config/config.example.yaml config/config.yaml
```

2. Edit `config/config.yaml` with your settings:

```yaml
app:
  runOnce: false              # run once and exit (default: false)
  fileManagerType: "simple"   # file manager: "simple" or "cached" (default: "simple")
  mediaDir: "media"           # base media output directory (default: "media")
  writeMetadataJson: false    # write metadata JSON files (default: false)

providers:
  provider1:
    username: "your_username"
    password: "your_password"
    url: "http://your-xtream-server.com"
    interval: 360
    category_name_regex:
      "^\\[.*\\]\\s*": ""
      "\\s*\\[.*\\]$": ""
    libraryRefresh:
      enabled: true
      protocol: "http"
      hostname: "localhost"
      port: 8096
      token: "your_jellyfin_api_token"
    settings:
      live:
        enabled: true
        category_folder: true
        use_server_info: false
        name_regex:
          "^\\[.*\\]\\s*": ""
          "\\s*\\[.*\\]$": ""
        exclude_categories: []
      movie:
        enabled: true
        category_folder: true
        use_server_info: false
        name_regex:
          "^\\[.*\\]\\s*": ""
          "\\s*\\[.*\\]$": ""
        exclude_categories: []
      series:
        enabled: true
        category_folder: true
        use_server_info: false
        name_regex:
          "^\\[.*\\]\\s*": ""
          "\\s*\\[.*\\]$": ""
        exclude_categories: []
```

### Configuration Options

#### Global Application Settings (`app`)

- `runOnce`: run once and exit instead of continuous scanning (default: `false`)
- `fileManagerType`: file manager implementation - `simple` or `cached` (default: `simple`)
- `mediaDir`: base media output directory (default: `media`)
- `writeMetadataJson`: write metadata JSON files for movies and series (default: `false`)

#### Provider Settings (`providers`)

Each provider can have the following settings:

- `username`/`password`: Xtream provider credentials
- `url`: Xtream provider URL
- `interval`: scan interval in minutes (default: 360 = 6 hours)
- `category_name_regex`: optional regex patterns to clean category names (key: regex pattern, value: replacement string)
- `libraryRefresh`:
  - `enabled`: whether to trigger library refresh after updates
  - `protocol`/`hostname`/`port`: Jellyfin server details
  - `token`: API token for authentication

#### Media-Type Settings (Live, Movie, Series)

- `enabled`: enable/disable this media type
- `category_folder`: organize content by category
- `use_server_info`: use server-provided URL (if false, constructs URL from provider details)
- `name_regex`: regex patterns to clean stream names (key: regex pattern, value: replacement string)
- `exclude_categories`: list of category IDs to exclude

## Running

### Run Directly

```bash
java -jar target/xtream2jellyfin-1.0.0-jar-with-dependencies.jar
```

All configuration is now centralized in `config/config.yaml`. No environment variables are needed.

## Output Structure

```
media/
  {provider_name}/
    live/
      live.m3u          # M3U playlist for all live channels
      epg.xml           # EPG data
    movie/
      {category}/       # optional category folder
        {movie_name}/
          {movie_name}.strm     # stream URL
          {movie_name}.json     # metadata (if WRITE_METADATA_JSON=true)
    series/
      {category}/       # optional category folder
        {series_name}/
          {series_name}.json    # series metadata (if WRITE_METADATA_JSON=true)
          Season 01/
            {series} - S01E01.strm
            {series} - S01E02.strm
```

## Docker Support

Build and run using the provided Dockerfile:

```bash
docker build -t xtream2jellyfin .
docker run -v $(pwd)/config:/app/config -v $(pwd)/media:/app/media xtream2jellyfin
```

All configuration is managed through the `config/config.yaml` file mounted as a volume.
