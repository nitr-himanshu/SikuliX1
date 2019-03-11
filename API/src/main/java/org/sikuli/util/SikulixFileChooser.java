/*
 * Copyright (c) 2010-2018, sikuli.org, sikulix.com - MIT license
 */

package org.sikuli.util;

import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
//import java.io.FilenameFilter;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.sikuli.basics.Debug;
import org.sikuli.basics.PreferencesUser;
import org.sikuli.basics.Settings;

public class SikulixFileChooser {
  static final int FILES = JFileChooser.FILES_ONLY;
  static final int DIRS = JFileChooser.DIRECTORIES_ONLY;
  static final int DIRSANDFILES = JFileChooser.FILES_AND_DIRECTORIES;
  static final int SAVE = FileDialog.SAVE;
  static final int LOAD = FileDialog.LOAD;
  Frame _parent;
  boolean accessingAsFile = false;
  boolean loadingImage = false;
  String theLastDir = PreferencesUser.getInstance().get("LAST_OPEN_DIR", "");

  public SikulixFileChooser(Frame parent) {
    _parent = parent;
  }

  public SikulixFileChooser(Frame parent, boolean accessingAsFile) {
    _parent = parent;
    this.accessingAsFile = accessingAsFile;
  }

  private boolean isPython = false;

  public void setPython() {
    isPython = true;
  }

  private boolean isUntitled = false;

  public void setUntitled() {
    isUntitled = true;
  }

  private boolean isGeneric() {
    if (_parent == null) {
      return false;
    }
    return (_parent.getWidth() < 3 && _parent.getHeight() < 3);
  }

  public File show(String title) {
    File ret = show(title, LOAD, DIRSANDFILES);
    return ret;
  }

  SikulixFileFilter sikuliFilterO = new SikulixFileFilter("Sikuli Script (*.sikuli, *.skl)", "o");
  SikulixFileFilter pythonFilterO = new SikulixFileFilter("Python script (*.py)", "op");
  SikulixFileFilter sikuliFilterS = new SikulixFileFilter("Sikuli Script (*.sikuli, *.skl)", "s");
  SikulixFileFilter pythonFilterS = new SikulixFileFilter("Python script (*.py)", "sp");

  public File load() {
    String title = "Open a Sikuli or Python Script";
    String lastUsedFilter = PreferencesUser.getInstance().get("LAST_USED_FILTER", "");
    File ret;
    if ("op".equals(lastUsedFilter) || "sp".equals(lastUsedFilter)) {
      ret = show(title, LOAD, DIRSANDFILES, pythonFilterO, sikuliFilterO);
    } else {
      ret = show(title, LOAD, DIRSANDFILES, sikuliFilterO, pythonFilterO);
    }
    return ret;
  }

  public File save() {
    File ret;
    File selectedFile;
    if (isUntitled) {
      String lastUsedFilter = PreferencesUser.getInstance().get("LAST_USED_FILTER", "");
      String title = "Save as Sikuli or Python Script";
      if ("op".equals(lastUsedFilter) || "sp".equals(lastUsedFilter)) {
        selectedFile = show(title, SAVE, DIRSANDFILES, pythonFilterS, sikuliFilterS);
      } else {
        selectedFile = show(title, SAVE, DIRSANDFILES, sikuliFilterS, pythonFilterS);
      }
      ret = selectedFile;
    } else if (isPython) {
      selectedFile = show("Save a Python script", SAVE, FILES,
              new SikulixFileFilter("Python script (*.py)", "sp"));
      ret = selectedFile;
    } else {
      String type = "Sikuli Script (*.sikuli)";
      String title = "Save a Sikuli Script";
      selectedFile = show(title, SAVE, DIRSANDFILES, new SikulixFileFilter(type, "s"));
      ret = selectedFile;
    }
    return ret;
  }

  public File export() {
    String type = "Sikuli packed Script (*.skl)";
    String title = "Export as Sikuli packed Script";
    File ret = show(title, SAVE, FILES, new SikulixFileFilter(type, "e"));
    return ret;
  }

  public File loadImage() {
    loadingImage = true;
    File ret = show("Load Image File", LOAD, FILES,
            new FileNameExtensionFilter("Image files (jpg, png)", "jpg", "jpeg", "png"));
    return ret;
  }

  private File show(final String title, final int mode, final int theSelectionMode, Object... filters) {
    Debug.log(3, "showFileChooser: %s at %s", title.split(" ")[0], theLastDir);
    File fileChoosen = null;
    Object filterChosen = null;
    final Object[] genericFilters = filters;
    final Object[] result = new Object[]{null, null};
    while (true) {
      if (filterChosen != null) {
        filters = new Object[]{filterChosen};
      }
      boolean tryAgain = false;
      if (isGeneric()) {
        try {
          EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
              processDialog(theSelectionMode, theLastDir, title, mode, genericFilters, result);
            }
          });
        } catch (Exception e) {
        }
      } else {
        processDialog(theSelectionMode, theLastDir, title, mode, filters, result);
      }
      if (null != result[0]) {
        fileChoosen = (File) result[0];
        String fileChoosenPath = fileChoosen.getAbsolutePath();
        if (fileChoosenPath.contains("###")) {
          tryAgain = true;
          fileChoosen = new File(fileChoosenPath.split("###")[0]);
        }
        theLastDir = fileChoosen.getParent();
        if (fileChoosen.isDirectory()) {
          theLastDir = fileChoosen.getAbsolutePath();
        }
        filterChosen = result[1];
        if (tryAgain) {
          continue;
        }
        PreferencesUser.getInstance().put("LAST_OPEN_DIR", theLastDir);
        if (filterChosen instanceof SikulixFileFilter) {
          PreferencesUser.getInstance().put("LAST_USED_FILTER", ((SikulixFileFilter) filterChosen)._type);
        }
        return fileChoosen;
      } else {
        return null;
      }
    }
  }

  private void processDialog(int selectionMode, String last_dir, String title, int mode, Object[] filters,
                             Object[] result) {
    JFileChooser fchooser = new JFileChooser();
    File fileChoosen = null;
    FileFilter filterChoosen = null;
    if (!last_dir.isEmpty()) {
      fchooser.setCurrentDirectory(new File(last_dir));
    }
    fchooser.setSelectedFile(null);
    fchooser.setDialogTitle(title);
    boolean shouldTraverse = false;
    String btnApprove = "Select";
    if (isGeneric()) {
      fchooser.setFileSelectionMode(DIRSANDFILES);
      fchooser.setAcceptAllFileFilterUsed(true);
      shouldTraverse = true;
    } else {
      if (Settings.isMac() && Settings.isJava7() && selectionMode == DIRS) {
        selectionMode = DIRSANDFILES;
      }
      fchooser.setFileSelectionMode(selectionMode);
      if (mode == FileDialog.SAVE) {
        fchooser.setDialogType(JFileChooser.SAVE_DIALOG);
        btnApprove = "Save";
      }
      if (filters.length == 0) {
        fchooser.setAcceptAllFileFilterUsed(true);
        shouldTraverse = true;
      } else {
        fchooser.setAcceptAllFileFilterUsed(false);
        for (Object filter : filters) {
          if (filter instanceof SikulixFileFilter) {
            fchooser.addChoosableFileFilter((SikulixFileFilter) filter);
          } else {
            fchooser.setFileFilter((FileNameExtensionFilter) filter);
            shouldTraverse = true;
          }
        }
      }
    }
    if (shouldTraverse && Settings.isMac()) {
      fchooser.putClientProperty("JFileChooser.packageIsTraversable", "always");
    }
    int dialogResponse = fchooser.showDialog(_parent, btnApprove);
    if (dialogResponse != JFileChooser.APPROVE_OPTION) {
      fileChoosen = null;
    } else {
      fileChoosen = fchooser.getSelectedFile();
    }
    if (null != fileChoosen) {
      filterChoosen = fchooser.getFileFilter();
      if (filterChoosen instanceof SikulixFileFilter) {
        fileChoosen = new File(((SikulixFileFilter) filterChoosen).validateFile(fileChoosen));
      }
    }
    result[0] = fileChoosen;
    result[1] = filterChoosen;
  }

  private boolean isValidScript(File f) {
    String[] endings = new String[]{".py", ".rb", ".js"};
    String fName = f.getName();
    if (loadingImage || fName.endsWith(".skl")) {
      return true;
    }
    if (fName.endsWith(".sikuli")) {
      fName = fName.substring(0, fName.length() - 7);
    }
    boolean valid = false;
    for (String ending : endings) {
      if (new File(f, fName + ending).exists()) {
        return true;
      }
    }
    return false;
  }

  private static boolean isExt(String fName, String givenExt) {
    int i = fName.lastIndexOf('.');
    if (i > 0) {
      if (fName.substring(i + 1).toLowerCase().equals(givenExt)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isFolder(File file) {
    return file.isDirectory();
  }

  class SikulixFileFilter extends FileFilter {

    private String _type, _desc;

    public SikulixFileFilter(String desc, String type) {
      _type = type;
      _desc = desc;
    }

    public String validateFile(File selectedFile) {
      String validatedFile = selectedFile.getAbsolutePath();
      String errorTag = "###";
      String error = errorTag + "notPossible";
      if (_type == "sp") {
        if (!selectedFile.getName().endsWith(".py")) {
          if (selectedFile.isDirectory()) {
            validatedFile = selectedFile.getAbsolutePath() + error;
          } else {
            validatedFile = selectedFile.getAbsolutePath() + ".py";
          }
        }
      } else if (_type == "s") {
        if (!selectedFile.getName().endsWith(".sikuli")) {
          if (selectedFile.isDirectory()) {
            validatedFile = selectedFile.getAbsolutePath() + error;
          } else {
            validatedFile = selectedFile.getAbsolutePath() + ".sikuli";
          }
        }
      } else if (_type == "o") {
        if (!selectedFile.getName().endsWith(".sikuli") || !selectedFile.exists()) {
          validatedFile = selectedFile.getAbsolutePath() + error;
        }
      } else if (_type == "op") {
        if (!selectedFile.getName().endsWith(".py") || selectedFile.isDirectory() || !selectedFile.exists()) {
          validatedFile = selectedFile.getAbsolutePath() + error;
        }
      } else if (_type == "e") {
        if (!selectedFile.getName().endsWith(".skl")) {
          validatedFile = selectedFile.getAbsolutePath() + ".skl";
        }
      }
      if (validatedFile.contains(errorTag)) {
        Debug.log(3, "SikulixFileChooser: error: (%s) %s", _type, validatedFile);
      }
      return validatedFile;
    }

    @Override
    public boolean accept(File f) {
      if ("o".equals(_type) && (isExt(f.getName(), "sikuli") || isExt(f.getName(), "skl"))) {
        return true;
      }
      if ("op".equals(_type) && (isExt(f.getName(), "py"))) {
        return true;
      }
      if ("s".equals(_type) && isExt(f.getName(), "sikuli")) {
        return true;
      }
      if ("sp".equals(_type) && isExt(f.getName(), "py")) {
        return true;
      }
      if ("e".equals(_type)) {
        if (isExt(f.getName(), "skl")) {
          return true;
        }
        if (Settings.isMac() && isExt(f.getName(), "sikuli")) {
          return false;
        }
      }
      if (Settings.isWindows() && f.isDirectory()) {
        return true;
      }
      return false;
    }

    @Override
    public String getDescription() {
      return _desc;
    }
  }
}
