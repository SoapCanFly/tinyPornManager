/*
 * Copyright 2012 Manuel Laggner
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
package org.tinymediamanager.ui;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URI;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.tinymediamanager.Globals;
import org.tinymediamanager.ui.movies.MoviePanel;
import org.tinymediamanager.ui.moviesets.MovieSetPanel;
import org.tinymediamanager.ui.settings.SettingsPanel;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * The Class MainWindow.
 */
public class MainWindow extends JFrame {
  /** The logger. */
  private final static Logger LOGGER           = Logger.getLogger(MainWindow.class);

  /** The Constant serialVersionUID. */
  private static final long   serialVersionUID = 1L;

  /** The action exit. */
  private final Action        actionExit       = new ExitAction();

  /** The action about. */
  private final Action        actionAbout      = new AboutAction();

  /** The action settings. */
  private final Action        actionSettings   = new SettingsAction();

  /** The action feedback. */
  private final Action        actionFeedback   = new FeedbackAction();

  /** The action bug report. */
  private final Action        actionBugReport  = new BugReportAction();

  /** The action donate. */
  private final Action        actionDonate     = new DonateAction();

  /** The instance. */
  private static MainWindow   instance;

  private JPanel              panelMovies;

  /**
   * Create the application.
   * 
   * @param name
   *          the name
   */
  public MainWindow(String name) {
    super(name);
    setName("mainWindow");

    instance = this;

    JMenuBar menuBar = new JMenuBar();
    setJMenuBar(menuBar);

    JMenu mnTmm = new JMenu("tinyMediaManager");
    menuBar.add(mnTmm);

    // JMenuItem mntmSettings = mnTmm.add(actionSettings);
    // mntmSettings.setText("Settings");

    JMenuItem mntmFeedback = mnTmm.add(actionFeedback);
    mntmFeedback.setText("Send feedback");

    JMenuItem mntmBugReport = mnTmm.add(actionBugReport);
    mntmBugReport.setText("Report a bug");

    mnTmm.addSeparator();

    JMenuItem mntmExit = mnTmm.add(actionExit);
    mntmExit.setText("Exit");
    initialize();

    // debug menu
    JMenu debug = new JMenu("Debug");
    JMenuItem clearDatabase = new JMenuItem("initialize database");
    debug.add(clearDatabase);
    clearDatabase.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        // delete the database
        try {
          Globals.shutdownDatabase();
          File db = new File("tmm.odb");
          if (db.exists()) {
            db.delete();
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
          JOptionPane.showMessageDialog(null, "Database initialized. Please restart tinyMediaManager");
        }
        catch (Exception e) {
          JOptionPane.showMessageDialog(null, "An error occured on database init. Please delete the file tmm.odb manually");
          // open the tmm folder
          try {
            File path = new File(".");
            // check whether this location exists
            if (path.exists()) {
              Desktop.getDesktop().open(path);
            }
          }
          catch (Exception ex) {
            LOGGER.warn(ex.getMessage());
          }
        }
        System.exit(0);
      }
    });
    JMenuItem clearCache = new JMenuItem("clear cache");
    debug.add(clearCache);
    clearCache.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        File cache = new File("cache");
        if (cache.exists()) {
          try {
            FileUtils.deleteDirectory(cache);
          }
          catch (Exception e) {
            LOGGER.warn(e.getMessage());
          }
        }
      }
    });

    menuBar.add(debug);

    mnTmm = new JMenu("?");
    menuBar.add(mnTmm);
    JMenuItem mntmDonate = mnTmm.add(actionDonate);
    mntmDonate.setText("Donate");
    mnTmm.addSeparator();
    JMenuItem mntmAbout = mnTmm.add(actionAbout);
    mntmAbout.setText("About");
    // setVisible(true);

  }

  /**
   * Initialize the contents of the frame.
   */
  private void initialize() {
    // set the logo
    setIconImage(Globals.logo);
    setBounds(5, 5, 1100, 727);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    getContentPane().setLayout(
        new FormLayout(new ColumnSpec[] { ColumnSpec.decode("default:grow"), ColumnSpec.decode("1dlu"), }, new RowSpec[] {
            FormFactory.RELATED_GAP_ROWSPEC, RowSpec.decode("fill:default:grow"), RowSpec.decode("fill:1dlu"), }));

    JTabbedPane tabbedPane = VerticalTextIcon.createTabbedPane(JTabbedPane.LEFT);
    tabbedPane.setTabPlacement(JTabbedPane.LEFT);
    getContentPane().add(tabbedPane, "1, 2, fill, fill");

    panelMovies = new MoviePanel();
    VerticalTextIcon.addTab(tabbedPane, "Movies", panelMovies);

    JPanel panelMovieSets = new MovieSetPanel();
    VerticalTextIcon.addTab(tabbedPane, "Movie sets", panelMovieSets);

    JPanel panelSettings = new SettingsPanel();
    VerticalTextIcon.addTab(tabbedPane, "Settings", panelSettings);

    // shutdown listener - to clean database connections safetly
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        try {
          // save unsaved settings
          Globals.settings.saveSettings();
          // close database connection
          Globals.shutdownDatabase();
          // clear cache directory
          if (Globals.settings.isClearCacheShutdown()) {
            File cache = new File("cache");
            if (cache.exists()) {
              FileUtils.deleteDirectory(cache);
            }
          }
        }
        catch (Exception ex) {
          LOGGER.warn(ex.getMessage());
        }
        Globals.executor.shutdownNow();
        dispose();
        System.exit(0); // calling the method is a must
      }
    });

  }

  /**
   * The Class ExitAction.
   */
  private class ExitAction extends AbstractAction {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new exit action.
     */
    public ExitAction() {
      // putValue(NAME, "SwingAction");
      // putValue(SHORT_DESCRIPTION, "Some short description");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
      instance.setVisible(false);
      instance.dispose();
    }
  }

  /**
   * Gets the movie panel.
   * 
   * @return the movie panel
   */
  public MoviePanel getMoviePanel() {
    return (MoviePanel) panelMovies;
  }

  /**
   * The Class AboutAction.
   */
  private class AboutAction extends AbstractAction {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new about action.
     */
    public AboutAction() {
      // putValue(NAME, "SwingAction");
      // putValue(SHORT_DESCRIPTION, "Some short description");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
      Dialog aboutDialog = new AboutDialog();
      aboutDialog.setVisible(true);
    }
  }

  /**
   * The Class SettingsAction.
   */
  private class SettingsAction extends AbstractAction {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new settings action.
     */
    public SettingsAction() {
      // putValue(NAME, "SwingAction");
      // putValue(SHORT_DESCRIPTION, "Some short description");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {

    }
  }

  /**
   * The Class FeedbackAction.
   */
  private class FeedbackAction extends AbstractAction {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new feedback action.
     */
    public FeedbackAction() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
      JDialog dialog = new FeedbackDialog();
      dialog.pack();
      dialog.setVisible(true);
    }
  }

  /**
   * Gets the frame.
   * 
   * @return the frame
   */
  public static JFrame getFrame() {
    return instance;
  }

  /**
   * The Class BugReportAction.
   */
  private class BugReportAction extends AbstractAction {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new feedback action.
     */
    public BugReportAction() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
      JDialog dialog = new BugReportDialog();
      dialog.pack();
      dialog.setVisible(true);
    }
  }

  /**
   * The Class DonateAction.
   */
  private class DonateAction extends AbstractAction {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new feedback action.
     */
    public DonateAction() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
      try {
        String url = StringEscapeUtils
            .unescapeHtml4("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&amp;business=manuel%2elaggner%40gmail%2ecom&amp;lc=GB&amp;item_name=tinyMediaManager&amp;currency_code=EUR&amp;bn=PP%2dDonationsBF%3abtn_donate_LG%2egif%3aNonHosted");
        Desktop.getDesktop().browse(new URI(url));
      }
      catch (Exception e1) {
        LOGGER.error("Donate", e1);
      }
    }
  }
}
