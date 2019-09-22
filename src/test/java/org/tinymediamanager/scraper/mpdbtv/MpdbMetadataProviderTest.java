/*
 * Copyright 2012 - 2019 Manuel Laggner
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
package org.tinymediamanager.scraper.mpdbtv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.tinymediamanager.scraper.MediaMetadata;
import org.tinymediamanager.scraper.MediaProviderInfo;
import org.tinymediamanager.scraper.MediaScrapeOptions;
import org.tinymediamanager.scraper.MediaSearchOptions;
import org.tinymediamanager.scraper.MediaSearchResult;
import org.tinymediamanager.scraper.entities.MediaType;
import org.tinymediamanager.scraper.exceptions.ScrapeException;

public class MpdbMetadataProviderTest {

  /**
   * Testing ProviderInfo
   */
  @Test
  public void testProviderInfo() {
    try {
      MpdbMetadataProvider mp = new MpdbMetadataProvider();
      MediaProviderInfo providerInfo = mp.getProviderInfo();

      assertThat(providerInfo.getDescription()).isNotNull();
      assertThat(providerInfo.getId()).isNotNull();
      assertThat(providerInfo.getName()).isNotNull();
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void testSearch() throws ScrapeException {
    MpdbMetadataProvider mp = new MpdbMetadataProvider();
    mp.getProviderInfo().getConfig().setValue("mpdb_aboKey", System.getProperty("aboKey"));
    mp.getProviderInfo().getConfig().setValue("mpdb_username", System.getProperty("username"));
    mp.getProviderInfo().getConfig().save();

    MediaSearchOptions options = new MediaSearchOptions(MediaType.MOVIE);
    options.setQuery("Batman");
    options.setLanguage(new Locale("us"));
    List<MediaSearchResult> result = mp.search(options);

    assertThat(result).isNotNull();
    assertThat(result.size()).isGreaterThan(40);

  }

  @Test
  public void testScrape() throws ScrapeException {
    MpdbMetadataProvider mp = new MpdbMetadataProvider();
    mp.getProviderInfo().getConfig().setValue("aboKey",System.getProperty("aboKey"));
    mp.getProviderInfo().getConfig().setValue("username",System.getProperty("username"));
    mp.getProviderInfo().getConfig().save();

    MediaScrapeOptions options = new MediaScrapeOptions(MediaType.MOVIE);
    options.setId("mpdbtv","3193");
    options.setLanguage(new Locale("fr"));

    MediaMetadata result = mp.getMetadata(options);
    assertThat(result).isNotNull();
    assertThat(result.getOriginalTitle()).isEqualTo("Star Wars: Clone Wars ");
    assertThat(result.getId("allocine")).isEqualTo("55310");
    assertThat(result.getId("imdb")).isEqualTo("tt0361243");
    assertThat(result.getRuntime()).isEqualTo(0);
    assertThat(result.getTitle()).isEqualTo("Star Wars : La Guerre des Clones");


  }


}
