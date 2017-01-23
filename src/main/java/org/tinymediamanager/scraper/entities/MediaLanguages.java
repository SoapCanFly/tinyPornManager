/*
 * Copyright 2012 - 2016 Manuel Laggner
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
package org.tinymediamanager.scraper.entities;

/**
 * The Enum MediaLanguages. All languages we support for scraping
 * 
 * @author Manuel Laggner
 * @since 1.0
 * 
 */
public enum MediaLanguages {

  //@formatter:off
  cs("Český"),
  de("Deutsch"),
  da("Dansk"),
  en("English"),
  es("Español"),
  fa("Persian"),
  fi("Suomi"),
  fr("Française"),
  hu("Magyar"),
  it("Italiano"),
  nl("Nederlands"),
  no("Norsk"),
  pl("Język polski"),
  pt("Portuguese"),
  pt_BR("Portuguese (Brazil)"),
  ro("Română"),
  ru("русский язык"),
  sl("Slovenščina"),
  sk("Slovenčina"),
  sv("Svenska"),
  tr("Türkçe"),
  zh("Chinese");
  //@formatter:on

  private String title;

  MediaLanguages(String title) {
    this.title = title;
  }

  /**
   * return the first 2 letters which is the language part
   * 
   * @return the language
   */
  public String getLanguage() {
    return name().substring(0, 2);
  }

  @Override
  public String toString() {
    return title;
  }
}
