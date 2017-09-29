/*
 * Copyright 2012 - 2017 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager.scraper.tmdb;

import static org.tinymediamanager.scraper.MediaMetadata.IMDB;
import static org.tinymediamanager.scraper.MediaMetadata.TVDB;
import static org.tinymediamanager.scraper.tmdb.TmdbMetadataProvider.providerInfo;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaScrapeOptions;
import org.tinymediamanager.scraper.MediaSearchOptions;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.Certification;
import org.tinymediamanager.scraper.entities.MediaArtwork;
import org.tinymediamanager.scraper.entities.MediaArtwork.MediaArtworkType;
import org.tinymediamanager.scraper.entities.MediaCastMember;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.scraper.entities.MediaRating;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.TvUtils;

import com.uwetrottmann.tmdb2.Tmdb;
import com.uwetrottmann.tmdb2.entities.AppendToResponse;
import com.uwetrottmann.tmdb2.entities.BaseCompany;
import com.uwetrottmann.tmdb2.entities.BaseTvEpisode;
import com.uwetrottmann.tmdb2.entities.BaseTvShow;
import com.uwetrottmann.tmdb2.entities.CastMember;
import com.uwetrottmann.tmdb2.entities.ContentRating;
import com.uwetrottmann.tmdb2.entities.TvEpisode;
import com.uwetrottmann.tmdb2.entities.TvSeason;
import com.uwetrottmann.tmdb2.entities.TvShow;
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage;
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem;

class TmdbTvShowMetadataProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(TmdbTvShowMetadataProvider.class);

  private final Tmdb          api;

  public TmdbTvShowMetadataProvider(Tmdb api) {
    this.api = api;
  }

  /**
   * searches a TV show with the given query parameters
   *
   * @param query
   *          the query parameters
   * @return a list of found TV shows
   * @throws Exception
   *           any exception which can be thrown while searching
   */
  List<MediaSearchResult> search(MediaSearchOptions query) throws Exception {
    LOGGER.debug("search() " + query.toString());

    List<MediaSearchResult> resultList = new ArrayList<>();

    String searchString = "";

    // check type
    if (query.getMediaType() != MediaType.TV_SHOW) {
      throw new Exception("wrong media type for this scraper");
    }

    if (StringUtils.isEmpty(searchString) && StringUtils.isNotEmpty(query.getQuery())) {
      searchString = query.getQuery();
    }

    if (StringUtils.isEmpty(searchString)) {
      LOGGER.debug("TMDB Scraper: empty searchString");
      return resultList;
    }

    searchString = MetadataUtil.removeNonSearchCharacters(searchString);
    String language = query.getLanguage().getLanguage();
    if (StringUtils.isNotBlank(query.getLanguage().getCountry())) {
      language += "-" + query.getLanguage().getCountry();
    }

    // begin search
    LOGGER.info("========= BEGIN TMDB Scraper Search for: " + searchString);
    synchronized (api) {
      try {
        TvShowResultsPage resultsPage = api.searchService().tv(searchString, 1, language, null, "phrase").execute().body();
        if (resultsPage != null) {
          for (BaseTvShow show : ListUtils.nullSafe(resultsPage.results)) {
            if (verifyTvShowLanguageTitle(query, resultsPage.results, resultList)) {
              break;
            }
            resultList.add(morphTvShowToSearchResult(show, query));
          }
        }
      }
      catch (Exception e) {
        LOGGER.debug("failed to search: " + e.getMessage());
      }
    }

    LOGGER.info("found " + resultList.size() + " results");
    return resultList;
  }

  /**
   * Get the episode list for the specified TV show
   * 
   * @param options
   *          the scrape options for getting the episode list
   * @return the episode list
   * @throws Exception
   *           any exception which can be thrown while searching
   */
  List<MediaMetadata> getEpisodeList(MediaScrapeOptions options) throws Exception {
    LOGGER.debug("getEpisodeList() " + options.toString());
    List<MediaMetadata> episodes = new ArrayList<>();

    int tmdbId = 0;
    // tmdbId from searchResult
    if (options.getResult() != null) {
      tmdbId = Integer.parseInt(options.getResult().getId());
    }

    // tmdbId from option
    if (tmdbId == 0) {
      tmdbId = options.getTmdbId();
    }

    // no tmdb id, no search..
    if (tmdbId == 0) {
      return episodes;
    }

    String language = options.getLanguage().getLanguage();
    if (StringUtils.isNotBlank(options.getLanguage().getCountry())) {
      language += "-" + options.getLanguage().getCountry();
    }

    // the API does not provide a complete access to all episodes, so we have to
    // fetch the show summary first and every season afterwards..
    synchronized (api) {
      try {
        TvShow complete = api.tvService().tv(tmdbId, language).execute().body();
        if (complete != null) {
          for (TvSeason season : ListUtils.nullSafe(complete.seasons)) {
            List<MediaMetadata> seasonEpisodes = new ArrayList<>();
            TvSeason fullSeason = api.tvSeasonsService().season(tmdbId, season.season_number, language).execute().body();
            if (fullSeason != null) {
              for (TvEpisode episode : ListUtils.nullSafe(fullSeason.episodes)) {

                if (verifyTvEpisodeTitleLanguage(options, season, fullSeason.episodes, seasonEpisodes)) {
                  break;
                }

                seasonEpisodes.add(morphTvEpisodeToMediaMetadata(episode));
              }
            }
            episodes.addAll(seasonEpisodes);
          }
        }
      }
      catch (Exception e) {
        LOGGER.debug("failed to get episode list: " + e.getMessage());
      }
    }

    return episodes;
  }

  /**
   * Get the meta data for either the TV show or an episode
   * 
   * @param options
   *          the scrape options
   * @return the meta data
   * @throws Exception
   *           any exception which can be thrown while searching
   */
  MediaMetadata getMetadata(MediaScrapeOptions options) throws Exception {
    switch (options.getType()) {
      case TV_SHOW:
        return getTvShowMetadata(options);

      case TV_EPISODE:
        return getEpisodeMetadata(options);

      default:
        throw new Exception("unsupported media type");
    }
  }

  private MediaMetadata getTvShowMetadata(MediaScrapeOptions options) throws Exception {
    return getTvShowMetadata(options, false, null);
  }

  private MediaMetadata getTvShowMetadata(MediaScrapeOptions options, Boolean fallback, MediaMetadata metadata) throws Exception {
    LOGGER.debug("getTvShowMetadata() " + options.toString());

    Boolean titleFallback = providerInfo.getConfig().getValueAsBool("titleFallback");

    Locale fallbackLanguage = new Locale(MediaLanguages.get(providerInfo.getConfig().getValue("titleFallbackLanguage")).getLanguage());

    MediaMetadata md = new MediaMetadata(TmdbMetadataProvider.providerInfo.getId());

    int tmdbId = 0;
    // tmdbId from searchResult
    if (options.getResult() != null) {
      tmdbId = Integer.parseInt(options.getResult().getId());
    }

    // tmdbId from option
    if (tmdbId == 0) {
      tmdbId = options.getTmdbId();
    }

    // no tmdb id, no scrape..
    if (tmdbId == 0) {
      return md;
    }

    String language = options.getLanguage().getLanguage();
    if (StringUtils.isNotBlank(options.getLanguage().getCountry())) {
      language += "-" + options.getLanguage().getCountry();
    }
    // get the data from tmdb
    TvShow complete = null;
    synchronized (api) {
      try {
        complete = api.tvService()
            .tv(tmdbId, language,
                new AppendToResponse(AppendToResponseItem.CREDITS, AppendToResponseItem.EXTERNAL_IDS, AppendToResponseItem.CONTENT_RATINGS))
            .execute().body();
      }
      catch (Exception e) {
        LOGGER.debug("failed to get meta data: " + e.getMessage());
      }
    }

    if (complete == null) {
      return md;
    }

    md.setId(TmdbMetadataProvider.providerInfo.getId(), tmdbId);
    md.setTitle(complete.name);
    md.setOriginalTitle(complete.original_name);

    MediaRating rating = new MediaRating("tmdb");
    rating.setRating(complete.vote_average);
    rating.setVoteCount(complete.vote_count);
    rating.setMaxValue(10);
    md.addRating(rating);

    md.setReleaseDate(complete.first_air_date);
    md.setPlot(complete.overview);

    // Poster
    if (StringUtils.isNotBlank(complete.poster_path)) {
      MediaArtwork ma = new MediaArtwork(TmdbMetadataProvider.providerInfo.getId(), MediaArtwork.MediaArtworkType.POSTER);
      ma.setPreviewUrl(TmdbMetadataProvider.configuration.images.base_url + "w185" + complete.poster_path);
      ma.setDefaultUrl(TmdbMetadataProvider.configuration.images.base_url + "w342" + complete.poster_path);
      ma.setLanguage(options.getLanguage().getLanguage());
      ma.setTmdbId(complete.id);
      md.addMediaArt(ma);
    }

    for (BaseCompany company : ListUtils.nullSafe(complete.production_companies)) {
      md.addProductionCompany(company.name.trim());
    }
    md.setStatus(complete.status);

    if (complete.first_air_date != null) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(complete.first_air_date);
      md.setYear(calendar.get(Calendar.YEAR));
    }

    if (complete.credits != null) {
      for (CastMember castMember : ListUtils.nullSafe(complete.credits.cast)) {
        MediaCastMember cm = new MediaCastMember(MediaCastMember.CastType.ACTOR);
        cm.setName(castMember.name);
        cm.setCharacter(castMember.character);
        md.addCastMember(cm);
      }
    }

    // external IDs
    if (complete.external_ids != null) {
      if (complete.external_ids.tvdb_id != null && complete.external_ids.tvdb_id > 0) {
        md.setId(TVDB, complete.external_ids.tvdb_id);
      }
      if (StringUtils.isNotBlank(complete.external_ids.imdb_id)) {
        md.setId(IMDB, complete.external_ids.imdb_id);
      }
      if (complete.external_ids.tvrage_id != null && complete.external_ids.tvrage_id > 0) {
        md.setId("tvrage", complete.external_ids.tvrage_id);
      }
    }

    // content ratings
    if (complete.content_ratings != null) {
      for (ContentRating country : ListUtils.nullSafe(complete.content_ratings.results)) {
        if (options.getCountry() == null || options.getCountry().getAlpha2().compareToIgnoreCase(country.iso_3166_1) == 0) {
          // do not use any empty certifications
          if (StringUtils.isEmpty(country.rating)) {
            continue;
          }

          md.addCertification(Certification.getCertification(country.iso_3166_1, country.rating));

        }
      }
    }

    // check if we need to rescrape in the fallback language
    if (((complete.name.equals(complete.original_name) && !complete.original_language.equals(options.getLanguage().getLanguage()))
        || StringUtils.isBlank(complete.overview)) && (!options.getLanguage().equals(fallbackLanguage) || fallback) && titleFallback) {
      // title in original language or plot was empty - scrape in fallback language

      if (fallback) {
        LOGGER.debug("Movie data not found with fallback language. Returning original.");
        return metadata;
      }

      Locale oldLang = options.getLanguage();
      try {
        options.setLanguage(fallbackLanguage);

        LOGGER.debug("Re-scraping using fallback language " + MediaLanguages.valueOf(options.getLanguage().getLanguage()));
        MediaMetadata fallbackMd = getTvShowMetadata(options, true, md);

        if (StringUtils.isBlank(complete.overview) && !StringUtils.isBlank(fallbackMd.getPlot())) {
          md.setPlot(fallbackMd.getPlot());
        }
        if (complete.name.equals(complete.original_name) && !complete.original_language.equals(oldLang.getLanguage())
            && !StringUtils.isBlank(fallbackMd.getTitle())) {
          md.setTitle(fallbackMd.getTitle());
        }
        if (StringUtils.isBlank(complete.original_name) && !StringUtils.isBlank(fallbackMd.getOriginalTitle())) {
          md.setOriginalTitle(fallbackMd.getOriginalTitle());
        }
      }
      catch (Exception ignored) {
      }
      finally {
        options.setLanguage(oldLang);
      }
    }

    return md;
  }

  private MediaMetadata getEpisodeMetadata(MediaScrapeOptions options) throws Exception {
    LOGGER.debug("getEpisodeMetadata() " + options.toString());
    MediaMetadata md = new MediaMetadata(TmdbMetadataProvider.providerInfo.getId());

    int tmdbId = 0;
    // tmdbId from searchResult
    if (options.getResult() != null) {
      tmdbId = Integer.parseInt(options.getResult().getId());
    }

    // tmdbId from option
    if (tmdbId == 0) {
      tmdbId = options.getTmdbId();
    }

    // no tmdb id, no scrape..
    if (tmdbId == 0) {
      return md;
    }

    // get episode number and season number
    int seasonNr = options.getIdAsIntOrDefault(MediaMetadata.SEASON_NR, -1);
    int episodeNr = options.getIdAsIntOrDefault(MediaMetadata.EPISODE_NR, -1);

    // parsed valid episode number/season number?
    String aired = "";
    if (options.getMetadata() != null && options.getMetadata().getReleaseDate() != null) {
      Format formatter = new SimpleDateFormat("yyyy-MM-dd");
      aired = formatter.format(options.getMetadata().getReleaseDate());
    }
    // does not work - we cannot scrape w/o season
    // if (aired.isEmpty() && (seasonNr == -1 || episodeNr == -1)) {
    if (seasonNr == -1 || episodeNr == -1) {
      LOGGER.warn("season number/episode number found");
      return md; // not even date set? return
    }

    String language = options.getLanguage().getLanguage();
    if (StringUtils.isNotBlank(options.getLanguage().getCountry())) {
      language += "-" + options.getLanguage().getCountry();
    }
    // get the data from tmdb
    TvEpisode episode = null;
    synchronized (api) {
      // get episode via season listing -> improves caching performance
      try {
        TvSeason fullSeason = api.tvSeasonsService().season(tmdbId, seasonNr, language).execute().body();
        if (fullSeason != null) {
          for (TvEpisode ep : ListUtils.nullSafe(fullSeason.episodes)) {
            if (ep.season_number == seasonNr && ep.episode_number == episodeNr) {
              episode = ep;
              break;
            }
          }

          // not found? try to match by date
          if (episode == null && !aired.isEmpty()) {
            for (TvEpisode ep : ListUtils.nullSafe(fullSeason.episodes)) {
              if (ep.air_date != null) {
                Format formatter = new SimpleDateFormat("yyyy-MM-dd");
                String epAired = formatter.format(ep.air_date);
                if (epAired.equals(aired)) {
                  episode = ep;
                  break;
                }
              }
            }
          }
        }

        verifyTvEpisodeTitleLanguage(episode, options);
      }
      catch (Exception e) {
        LOGGER.debug("failed to get meta data: " + e.getMessage());
      }
    }

    if (episode == null) {
      return md;
    }

    md.setEpisodeNumber(TvUtils.getEpisodeNumber(episode.episode_number));
    md.setSeasonNumber(TvUtils.getSeasonNumber(episode.season_number));
    md.setId(TmdbMetadataProvider.providerInfo.getId(), episode.id);

    // external IDs
    if (episode.external_ids != null) {
      if (episode.external_ids.tvdb_id != null && episode.external_ids.tvdb_id > 0) {
        md.setId(TVDB, episode.external_ids.tvdb_id);
      }
      if (StringUtils.isNotBlank(episode.external_ids.imdb_id)) {
        md.setId(IMDB, episode.external_ids.imdb_id);
      }
      if (episode.external_ids.tvrage_id != null && episode.external_ids.tvrage_id > 0) {
        md.setId("tvrage", episode.external_ids.tvrage_id);
      }
    }

    md.setTitle(episode.name);
    md.setPlot(episode.overview);

    MediaRating rating = new MediaRating("tmdb");
    rating.setRating(episode.vote_average);
    rating.setVoteCount(episode.vote_count);
    rating.setMaxValue(10);
    md.addRating(rating);

    md.setReleaseDate(episode.air_date);

    for (CastMember castMember : ListUtils.nullSafe(episode.guest_stars)) {
      MediaCastMember cm = new MediaCastMember(MediaCastMember.CastType.ACTOR);
      cm.setName(castMember.name);
      cm.setCharacter(castMember.character);
      md.addCastMember(cm);
    }

    // Thumb
    if (StringUtils.isNotBlank(episode.still_path)
        && (options.getArtworkType() == MediaArtwork.MediaArtworkType.ALL || options.getArtworkType() == MediaArtwork.MediaArtworkType.THUMB)) {
      MediaArtwork ma = new MediaArtwork(TmdbMetadataProvider.providerInfo.getId(), MediaArtworkType.THUMB);
      ma.setPreviewUrl(TmdbMetadataProvider.configuration.images.base_url + "original" + episode.still_path);
      ma.setDefaultUrl(TmdbMetadataProvider.configuration.images.base_url + "original" + episode.still_path);
      md.addMediaArt(ma);
    }

    return md;
  }

  /**
   * Language Fallback Mechanism - For TV Show Search Results
   *
   * @param query
   *          the query options
   * @param original
   *          the original tv show list
   * @param resultList
   *          the list that tv shows will be added.
   */
  private Boolean verifyTvShowLanguageTitle(MediaSearchOptions query, List<BaseTvShow> original, List<MediaSearchResult> resultList) {

    if (providerInfo.getConfig().getValueAsBool("titleFallback")) {

      Locale fallbackLanguage = new Locale(MediaLanguages.get(providerInfo.getConfig().getValue("titleFallbackLanguage")).getLanguage());

      for (BaseTvShow tvShow : original) {

        // tmdb provides title = originalTitle if no title in the requested language has been found,
        // so get the title in a alternative language
        if (tvShow.name.equals(tvShow.original_name) && !tvShow.original_language.equals(query.getLanguage().getLanguage())
            && !query.getLanguage().equals(fallbackLanguage)) {
          LOGGER.debug("Fallback: Title Inconsistency Found. Bypassing default functionality and Initiating Fallback Mechanism.");

          try {
            String lang = MediaLanguages.get(providerInfo.getConfig().getValue("titleFallbackLanguage")).name().replace("_", "-");

            TvShowResultsPage fallbackResultsPage = api.searchService().tv(query.getQuery(), 1, lang, null, "phrase").execute().body();

            if (fallbackResultsPage == null || fallbackResultsPage.results == null) {
              return false;
            }

            List<BaseTvShow> fallback = fallbackResultsPage.results;

            resultList.clear();

            for (int i = 0; i < original.size(); i++) {
              BaseTvShow originalTvShow = original.get(i);
              BaseTvShow fallbackTvShow = fallback.get(i);

              if (originalTvShow.name.equals(originalTvShow.original_name) && !originalTvShow.name.equals(fallbackTvShow.name)
                  && !originalTvShow.original_language.equals(query.getLanguage().getLanguage()) && !StringUtils.isBlank(fallbackTvShow.name)) {
                LOGGER
                    .debug(String.format("Fallback: TV Show Replaced          ([%-32.32s] -> [%-32.32s])", originalTvShow.name, fallbackTvShow.name));
                resultList.add(morphTvShowToSearchResult(fallbackTvShow, query));
              }
              else {
                LOGGER.debug(String.format("Fallback: TV Show Remained the Same ([%-32.32s])", originalTvShow.name));
                resultList.add(morphTvShowToSearchResult(originalTvShow, query));
              }
            }
            return true;

          }
          catch (Exception exc) {
            return false;
          }
        }

      }
    }
    return false;
  }

  /**
   * Language Fallback Mechanism - For TV Season Episode List
   *
   * @param query
   *          the query options
   * @param original
   *          the original tv show list
   * @param episodeList
   *          the list that tv show episodes will be added.
   * @param season
   *          the season tv episodes belong to.
   */
  private boolean verifyTvEpisodeTitleLanguage(MediaScrapeOptions query, TvSeason season, List<TvEpisode> original, List<MediaMetadata> episodeList) {

    if (original != null && providerInfo.getConfig().getValueAsBool("titleFallback")) {

      for (TvEpisode episode : original) {
        if (episode != null && (StringUtils.isAnyBlank(episode.name, episode.overview) || isEpisodesNameDefault(episode, episode.episode_number))
            && providerInfo.getConfig().getValueAsBool("titleFallback")) {
          try {

            episodeList.clear();

            String languageFallback = MediaLanguages.get(providerInfo.getConfig().getValue("titleFallbackLanguage")).name().replace("_", "-");

            TvSeason fullSeasonFallback = api.tvSeasonsService()
                .season(query.getResult() != null ? Integer.parseInt(query.getResult().getId()) : query.getTmdbId(), season.season_number,
                    languageFallback)
                .execute().body();

            if (fullSeasonFallback == null || fullSeasonFallback.episodes == null) {
              return false;
            }

            List<TvEpisode> fallback = fullSeasonFallback.episodes;

            for (TvEpisode originalEpisode : original) {
              if (!StringUtils.isAnyBlank(originalEpisode.name, originalEpisode.overview)
                  && !isEpisodesNameDefault(originalEpisode, originalEpisode.episode_number)) {
                episodeList.add(morphTvEpisodeToMediaMetadata(originalEpisode));
                continue;
              }
              for (TvEpisode fallbackEpisode : fallback) {
                if (originalEpisode.episode_number.equals(fallbackEpisode.episode_number)
                    && originalEpisode.season_number.equals(fallbackEpisode.season_number)) {

                  if (StringUtils.isBlank(originalEpisode.name) || (isEpisodesNameDefault(originalEpisode, originalEpisode.episode_number)
                      && !isEpisodesNameDefault(fallbackEpisode, fallbackEpisode.episode_number))) {
                    originalEpisode.name = fallbackEpisode.name;
                  }
                  if (StringUtils.isBlank(originalEpisode.overview)) {
                    originalEpisode.overview = fallbackEpisode.overview;
                  }

                  episodeList.add(morphTvEpisodeToMediaMetadata(originalEpisode));

                  break;
                }
              }
            }
            return true;
          }
          catch (Exception exc) {
            return false;
          }
        }
      }
    }
    return false;
  }

  /**
   * Language Fallback Mechanism - For TV Episode
   *
   * @param query
   *          the query options
   * @param episode
   *          the original tv episode
   */
  private void verifyTvEpisodeTitleLanguage(BaseTvEpisode episode, MediaScrapeOptions query) {
    int seasonNr = query.getIdAsInt(MediaMetadata.SEASON_NR);
    int episodeNr = query.getIdAsInt(MediaMetadata.EPISODE_NR);

    if (episode != null && (StringUtils.isAnyBlank(episode.name, episode.overview) || isEpisodesNameDefault(episode, episodeNr))
        && providerInfo.getConfig().getValueAsBool("titleFallback")) {

      String languageFallback = MediaLanguages.get(providerInfo.getConfig().getValue("titleFallbackLanguage")).name().replace("_", "-");

      try {
        TvEpisode ep = api.tvEpisodesService().episode(query.getResult() != null ? Integer.parseInt(query.getResult().getId()) : query.getTmdbId(),
            episode.season_number, episode.episode_number, languageFallback).execute().body();
        if (ep != null) {
          if ((ep.season_number == seasonNr || ep.episode_number.equals(episode.season_number))
              && (ep.episode_number == episodeNr || ep.episode_number.equals(episode.episode_number))) {

            if (StringUtils.isBlank(episode.name) || (isEpisodesNameDefault(episode, episodeNr) && !isEpisodesNameDefault(ep, episodeNr))) {
              episode.name = ep.name;
            }
            if (StringUtils.isBlank(episode.overview)) {
              episode.overview = ep.overview;
            }
          }
        }
      }
      catch (Exception ignored) {

      }
    }
  }

  private Integer toInteger(String str) {
    try {
      return Integer.parseInt(str);
    }
    catch (Exception exc) {
      return null;
    }
  }

  private Boolean isEpisodesNameDefault(BaseTvEpisode episode, Integer episodeNr) {
    Integer potentialEpisodeNumber;
    String[] originalEpisodeName;
    return (originalEpisodeName = episode.name.split(" ")).length == 2 && (potentialEpisodeNumber = toInteger(originalEpisodeName[1])) != null
        && (potentialEpisodeNumber.equals(episode.episode_number) || potentialEpisodeNumber.equals(episodeNr));
  }

  private MediaMetadata morphTvEpisodeToMediaMetadata(BaseTvEpisode episode) {
    MediaMetadata ep = new MediaMetadata(TmdbMetadataProvider.providerInfo.getId());
    ep.setId(providerInfo.getId(), episode.id);
    ep.setEpisodeNumber(episode.episode_number);
    ep.setSeasonNumber(episode.season_number);
    ep.setTitle(episode.name);
    ep.setPlot(episode.overview);

    if (episode.vote_average != null && episode.vote_count != null) {
      MediaRating rating = new MediaRating(providerInfo.getId());
      rating.setRating(episode.vote_average);
      rating.setVoteCount(episode.vote_count);
      rating.setMaxValue(10);
      ep.addRating(rating);
    }
    if (episode.air_date != null) {
      ep.setReleaseDate(episode.air_date);
    }

    return ep;
  }

  private MediaSearchResult morphTvShowToSearchResult(BaseTvShow tvShow, MediaSearchOptions query) {

    MediaSearchResult result = new MediaSearchResult(TmdbMetadataProvider.providerInfo.getId(), MediaType.TV_SHOW);
    result.setId(Integer.toString(tvShow.id));
    result.setTitle(tvShow.name);
    result.setOriginalTitle(tvShow.original_name);
    result.setPosterUrl(TmdbMetadataProvider.configuration.images.base_url + "w342" + tvShow.poster_path);

    // parse release date to year
    if (tvShow.first_air_date != null) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(tvShow.first_air_date);
      result.setYear(calendar.get(Calendar.YEAR));
    }

    // calculate score
    result.setScore(MetadataUtil.calculateScore(query.getQuery(), result.getTitle()));
    return result;
  }

}
