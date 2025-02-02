/**
 * This file is part of Logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + Haute École Spécialisée Bernoise
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 *   + REDS Institute - HEIG-VD, Yverdon-les-Bains, Switzerland
 *     http://reds.heig-vd.ch
 * This version of the project is currently maintained by:
 *   + Kevin Walsh (kwalsh@holycross.edu, http://mathcs.holycross.edu/~kwalsh)
 */

package com.cburch.logisim.std.hdl;
import static com.cburch.logisim.std.Strings.S;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.cburch.hdl.HdlFile;
import com.cburch.hdl.HdlModel;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.FileUtil;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.draw.toolbar.ToolbarModel;
import com.cburch.logisim.proj.Action;
import com.cburch.hdl.HdlModelListener;

import com.kwalsh.logisim.vhdl.VhdlSyntax;

public class HdlContentView extends JPanel implements DocumentListener, HdlModelListener {

  private class HdlEditAction extends Action {
    HdlModel model;
    String original, revised;
    boolean wasDirty;

    HdlEditAction(HdlModel model) {
      this.model = model;
      this.original = model.getContent();
      this.revised = editor.getText();
      this.wasDirty = toolbar.isDirty();
    }

    public void doIt(Project proj) {
      // note: editor will have already been updated before action
      model.setContent(revised);
      toolbar.setDirty(!model.isValid());
    }
    public void redo(Project proj) {
      model.setContent(revised);
      toolbar.setDirty(!model.isValid());
      if (HdlContentView.this.model != model)
        setHdlModel(model);
      else
        setEditorText(revised);
    }
    public String getName() { return "VHDL edits"; }
    public boolean shouldAppendTo(Action other) {
      return false;
      // return (other instanceof HdlEditAction)
      //     && ((HdlEditAction)other).model == model;
    }
    public void undo(Project proj) {
      model.setContent(original);
      toolbar.setDirty(!model.isValid());
      if (HdlContentView.this.model != model)
        setHdlModel(model);
      else
        setEditorText(original);
    }
    // public Action append(Action other) {
    //   return this;
    // }
  }

  @Override
  public void changedUpdate(DocumentEvent de) { }

  @Override
  public void insertUpdate(DocumentEvent de) { docChanged(); }

  @Override
  public void removeUpdate(DocumentEvent de) { docChanged(); }

  void docChanged() {
    if (model == null)
      return;
    project.doAction(new HdlEditAction(model));
  }

  void doExport() {
    JFileChooser chooser = JFileChoosers.createSelected(getDefaultExportFile(null));
    chooser.setDialogTitle(S.get("hdlSaveDialog"));
    int choice = chooser.showSaveDialog(HdlContentView.this);
    if (choice == JFileChooser.APPROVE_OPTION) {
      File f = chooser.getSelectedFile();
      try {
        HdlFile.save(f, editor.getText());
      } catch (IOException e) {
        JOptionPane.showMessageDialog(HdlContentView.this,
            e.getMessage(),
            S.get("hexSaveErrorTitle"),
            JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  void doImport() {
    if (!editor.getText().equals(model.getContent()))
      if (!confirmImport(HdlContentView.this))
        return;
    String vhdl = project.getLogisimFile().getLoader().vhdlImportChooser(HdlContentView.this);
    if (vhdl != null)
      setEditorText(vhdl);
  }

  void doValidate() {
    model.setContent(editor.getText());
    toolbar.setDirty(!model.isValid());
    editor.forceReparsing(parser);
    if (!model.isValid())
      model.showErrors();
  }

  public static boolean confirmImport(Component parent) {
    String[] options = { S.get("importOption"),
      S.get("cancelOption") };
    return JOptionPane.showOptionDialog(parent,
        S.get("importMessage"), S.get("importTitle"), 0,
        JOptionPane.QUESTION_MESSAGE, null, options, options[0]) == 0;
  }

  private static final long serialVersionUID = 1L;
  private static final int ROWS = 40;

  private static final int COLUMNS = 100;

  private static final String EXPORT_DIR = "hdl_export";

  private RSyntaxTextArea editor;
  private RTextScrollPane scrollpane;
  private HdlModel model;
  private Project project;
  private VhdlParser parser;

  private HdlToolbarModel toolbar;

  public HdlContentView(Project proj) {
    super(new BorderLayout());
    this.project = proj;
    this.model = null;
    this.toolbar = new HdlToolbarModel(proj, this);
    configure("vhdl");
  }

  private void configure(String lang) {
    JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

    editor = new RSyntaxTextArea(ROWS, COLUMNS);

    if (lang.equals("vhdl")) {
      ((RSyntaxDocument)editor.getDocument()).setSyntaxStyle(new VhdlSyntax());
      editor.setCodeFoldingEnabled(true);
    } else {
      throw new IllegalArgumentException("unrecognized language: " + lang);
    }

    editor.setAntiAliasingEnabled(true);
    editor.getDocument().addDocumentListener(this);

    scrollpane = new RTextScrollPane(editor);
    scrollpane.setFoldIndicatorEnabled(true);
    scrollpane.setIconRowHeaderEnabled(true);

    if (lang.equals("vhdl")) {
      parser = new VhdlParser(scrollpane.getGutter()); // disabled, initially
      parser.enabled = false;
      editor.addParser(parser);
    }

    add(scrollpane, BorderLayout.CENTER);
    add(buttonsPanel, BorderLayout.NORTH);

    // pack();

    Dimension size = getSize();
    Dimension screen = getToolkit().getScreenSize();
    if (size.width > screen.width || size.height > screen.height) {
      size.width = Math.min(size.width, screen.width);
      size.height = Math.min(size.height, screen.height);
      setSize(size);
    }
  }

  private File getDefaultExportFile(File defaultFile) {
    File projectFile = project.getLogisimFile().getLoader().getMainFile();
    if (projectFile == null) {
      if (defaultFile == null)
        return new File(model.getName() + ".vhd");
      return defaultFile;
    }

    File compFolder;
    try {
      compFolder = new File(FileUtil.correctPath(projectFile
            .getParentFile().getCanonicalPath()) + EXPORT_DIR);
      if (!compFolder.exists()
          || (compFolder.exists() && !compFolder.isDirectory()))
        compFolder.mkdir();
      return new File(FileUtil.correctPath(compFolder.getCanonicalPath())
          + model.getName() + ".vhd");
    } catch (IOException ex) {
      return defaultFile;
    }
  }

  private File getDefaultImportFile(File defaultFile) {
    File projectFile = project.getLogisimFile().getLoader().getMainFile();
    if (projectFile == null)
      return defaultFile;

    File compFolder;
    try {
      compFolder = new File(FileUtil.correctPath(projectFile
            .getParentFile().getCanonicalPath()) + EXPORT_DIR);
      if (!compFolder.exists()
          || (compFolder.exists() && !compFolder.isDirectory()))
        compFolder.mkdir();
      return new File(FileUtil.correctPath(compFolder.getCanonicalPath()));
    } catch (IOException ex) {
      return defaultFile;
    }
  }

  public HdlModel getHdlModel() {
    return model;
  }

  public ToolbarModel getToolbarModel() {
    return toolbar;
  }

  private void setEditorText(String content) {
    parser.enabled = false;
    scrollpane.getGutter().removeAllTrackingIcons();
    editor.setText(content == null ? "" : content);
    editor.discardAllEdits();
    editor.setCaretPosition(0);
    parser.enabled = (content != null);
    if (parser.enabled)
      editor.forceReparsing(parser);
  }

  private void clearHdlModel() {
    if (model == null)
      return;
    if (!editor.getText().equals(model.getContent()))
      model.setContent(editor.getText());
    model.removeHdlModelWeakListener(null, this);
    model = null;
    setEditorText(null);
  }

  @Override
  public void contentSet(HdlModel source) {
    if (model != source)
      return;
    if (!editor.getText().equals(model.getContent()))
      setEditorText(model.getContent());
    toolbar.setDirty(!model.isValid());
  }

  @Override
  public void aboutToSave(HdlModel source) {
    if (model != source)
      return;
    if (!editor.getText().equals(model.getContent())) {
      model.setContent(editor.getText());
      toolbar.setDirty(!model.isValid());
    }
  }
  @Override
  public void displayChanged(HdlModel source) { }
  @Override
  public void appearanceChanged(HdlModel source) { }

  public void setHdlModel(HdlModel model) {
    if (this.model == model)
      return;
    clearHdlModel();
    this.model = model;
    if (this.model != null) {
      model.addHdlModelWeakListener(null, toolbar);
      model.addHdlModelWeakListener(null, this);
      setEditorText(model.getContent());
      toolbar.setDirty(!model.isValid());
    }
  }

  public void setCaretPosition(int s) {
    editor.setCaretPosition(s);
  }

}
