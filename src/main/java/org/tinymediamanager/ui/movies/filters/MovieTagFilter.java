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
package org.tinymediamanager.ui.movies.filters;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.SwingWorker;

import org.tinymediamanager.core.Constants;
import org.tinymediamanager.core.movie.MovieList;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.components.TmmLabel;
import org.tinymediamanager.ui.components.table.TmmTableFormat;

/**
 * This class implements a tag filter for movies
 * 
 * @author Manuel Laggner
 */
public class MovieTagFilter extends AbstractCheckComboBoxMovieUIFilter<String> {
  private TmmTableFormat.StringComparator comparator;
  private MovieList                       movieList = MovieList.getInstance();
  private Set<String>                     oldTags   = new HashSet<>();

  public MovieTagFilter() {
    super();
    comparator = new TmmTableFormat.StringComparator();
    buildAndInstallTagsArray();
    PropertyChangeListener propertyChangeListener = evt -> buildAndInstallTagsArray();
    movieList.addPropertyChangeListener(Constants.TAG, propertyChangeListener);
  }

  @Override
  public String getId() {
    return "movieTag";
  }

  @Override
  public boolean accept(Movie movie) {
    List<String> tags = checkComboBox.getSelectedItems();

    // check for explicit empty search
    if (tags.isEmpty() && movie.getTags().isEmpty()) {
      return true;
    }

    // check for all values
    for (String tag : movie.getTags()) {
      if (tags.contains(tag)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected JLabel createLabel() {
    return new TmmLabel(BUNDLE.getString("movieextendedsearch.tag")); //$NON-NLS-1$
  }

  private void buildAndInstallTagsArray() {
    // do it lazy because otherwise there is too much UI overhead
    // also use a set for faster lookups
    boolean dirty = false;
    Set<String> tags = new HashSet<>(movieList.getTagsInMovies());

    if (oldTags.size() != tags.size()) {
      dirty = true;
    }

    if (!oldTags.containsAll(tags) || !tags.containsAll(oldTags)) {
      dirty = true;
    }

    if (dirty) {
      oldTags.clear();
      oldTags.addAll(tags);

      List<String> newTags = new ArrayList<>(tags);
      newTags.sort(comparator);
      setValues(newTags);
    }
  }

  @Override
  protected String parseTypeToString(String type) throws Exception {
    return type;
  }

  @Override
  protected String parseStringToType(String string) throws Exception {
    return string;
  }

  private class TagsWorker extends SwingWorker<Void, Void> {

    @Override
    protected Void doInBackground() throws Exception {
      return null;
    }

    @Override
    protected void done() {
      super.done();
    }
  }
}
