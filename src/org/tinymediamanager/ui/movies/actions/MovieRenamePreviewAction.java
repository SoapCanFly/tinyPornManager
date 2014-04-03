/*
 * Copyright 2012 - 2014 Manuel Laggner
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
package org.tinymediamanager.ui.movies.actions;

import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import org.tinymediamanager.ui.UTF8Control;
import org.tinymediamanager.ui.movies.MovieUIModule;
import org.tinymediamanager.ui.movies.dialogs.MovieRenamerPreviewDialog;

/**
 * The class MovieRenamePreviewAction. This action is for creating a preview of the renamer
 * 
 * @author Manuel Laggner
 */
public class MovieRenamePreviewAction extends AbstractAction {
  private static final long           serialVersionUID = 5158514686702295145L;
  private static final ResourceBundle BUNDLE           = ResourceBundle.getBundle("messages", new UTF8Control()); //$NON-NLS-1$

  public MovieRenamePreviewAction() {
    putValue(NAME, BUNDLE.getString("movie.renamepreview")); //$NON-NLS-1$
    putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource("/org/tinymediamanager/ui/images/rename-icon.png")));
    putValue(SMALL_ICON, new ImageIcon(getClass().getResource("/org/tinymediamanager/ui/images/rename-icon.png")));
    putValue(SHORT_DESCRIPTION, BUNDLE.getString("movie.renamepreview.hint")); //$NON-NLS-1$
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    MovieRenamerPreviewDialog dialog = new MovieRenamerPreviewDialog(MovieUIModule.getInstance().getSelectionModel().getSelectedMovies());
    dialog.setVisible(true);
  }

}