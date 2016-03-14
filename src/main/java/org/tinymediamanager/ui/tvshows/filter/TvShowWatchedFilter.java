/*
 * Copyright 2012 - 2015 Manuel Laggner
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
package org.tinymediamanager.ui.tvshows.filter;

import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.ui.tvshows.AbstractTvShowUIFilter;

/**
 * This class implements a watchted filter for the TV show tree
 * 
 * @author Manuel Laggner
 */
public class TvShowWatchedFilter extends AbstractTvShowUIFilter {
  private enum WatchedFlag {
    WATCHED(BUNDLE.getString("metatag.watched")), //$NON-NLS-1$ ,
    NOT_WATCHED(BUNDLE.getString("metatag.notwatched")); //$NON-NLS-1$ ,

    private String title;

    private WatchedFlag(String title) {
      this.title = title;
    }

    @Override
    public String toString() {
      return title;
    }
  }

  private JComboBox<WatchedFlag> comboBox;

  @Override
  protected JLabel createLabel() {
    return new JLabel(BUNDLE.getString("metatag.watched")); //$NON-NLS-1$
  }

  @Override
  protected JComponent createFilterComponent() {
    comboBox = new JComboBox<>(WatchedFlag.values());
    return comboBox;
  }

  @Override
  protected boolean accept(TvShow tvShow, List<TvShowEpisode> episodes) {
    for (TvShowEpisode episode : episodes) {
      if (episode.isWatched() == (comboBox.getSelectedItem() == WatchedFlag.WATCHED)) {
        return true;
      }
    }

    return false;
  }
}