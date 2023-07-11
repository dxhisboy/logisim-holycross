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

package com.cburch.logisim.analyze.gui;
import static com.cburch.logisim.analyze.model.Strings.S;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.SwingWorker;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Dialog.ModalityType;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.JLabel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.ArrayList;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.analyze.model.Parser;

public class Analyzer extends LFrame.SubWindow {
  private AnalyzerMenuListener menuListener;

  private class MyChangeListener implements ChangeListener {
    public void stateChanged(ChangeEvent e) {
      // enableItems(menubar);

      Object selected = tabbedPane.getSelectedComponent();
      if (selected instanceof JScrollPane) {
        selected = ((JScrollPane) selected).getViewport().getView();
      }
      if (selected instanceof JPanel) {
        ((JPanel) selected).requestFocus();
      }
      if (selected instanceof AnalyzerTab) {
        AnalyzerTab tab = (AnalyzerTab)selected;
        menuListener.setEditHandler(tab.getEditHandler());
        menuListener.setPrintHandler(tab.getPrintHandler());
        model.getOutputExpressions().enableUpdates();
        tab.updateTab();
      } else {
        model.getOutputExpressions().disableUpdates();
      }
    }
  }

  private class MyLocaleListener implements LocaleListener {
    public void localeChanged() {
      Analyzer.this.setTitle(S.get("analyzerWindowTitle"));
      tabbedPane.setTitleAt(IO_TAB, S.get("inputsOutputsTab"));
      tabbedPane.setTitleAt(TABLE_TAB, S.get("tableTab"));
      tabbedPane.setTitleAt(EXPRESSION_TAB, S.get("expressionTab"));
      tabbedPane.setTitleAt(MINIMIZED_TAB, S.get("minimizedTab"));
      tabbedPane .setToolTipTextAt(IO_TAB, S.get("inputsOutputsTabTip"));
      tabbedPane.setToolTipTextAt(TABLE_TAB, S.get("tableTabTip"));
      tabbedPane.setToolTipTextAt(EXPRESSION_TAB, S.get("expressionTabTip"));
      tabbedPane.setToolTipTextAt(MINIMIZED_TAB, S.get("minimizedTabTip"));
      importTable.setText(S.get("importTableButton"));
      buildCircuit.setText(S.get("buildCircuitButton"));
      exportTable.setText(S.get("exportTableButton"));
      ioPanel.localeChanged();
      truthTablePanel.localeChanged();
      expressionPanel.localeChanged();
      minimizedPanel.localeChanged();
      importTable.localeChanged();
      buildCircuit.localeChanged();
      exportTable.localeChanged();
    }
  }

  public static void main(String[] args) throws Exception {
    Analyzer frame = new Analyzer();
    AnalyzerModel model = frame.getModel();

    if (args.length >= 2) {
      ArrayList<Var> inputs = new ArrayList<>();
      ArrayList<Var> outputs = new ArrayList<>();
      for (String s: args[0].split(","))
        inputs.add(Var.parse(s));
      for (String s: args[1].split(","))
        outputs.add(Var.parse(s));
      model.setVariables(inputs, outputs);
    }
    for (int i = 2; i < args.length; i++) {
      String s = args[i];
      int idx = s.indexOf('=');
      if (idx < 0) {
        Parser.parse(s, model); // for testing Parser.parse
        continue;
      } else {
        String name = s.substring(0, idx);
        String exprString = s.substring(idx+1);
        Expression expr = Parser.parse(exprString, model);
        model.getOutputExpressions().setExpression(name, expr, exprString);
      }
    }
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setVisible(true);
  }

  private static final long serialVersionUID = 1L;
  // used by circuit analysis to select the relevant tab automatically.
  public static final int IO_TAB = 0;
  public static final int TABLE_TAB = 1;
  public static final int EXPRESSION_TAB = 2;
  public static final int MINIMIZED_TAB = 3;

  private MyLocaleListener myLocaleListener = new MyLocaleListener();
  private MyChangeListener myChangeListener = new MyChangeListener();
  private AnalyzerModel model = new AnalyzerModel();

  private JTabbedPane tabbedPane = new JTabbedPane();
  private VariableTab ioPanel;
  private TableTab truthTablePanel;
  private ExpressionTab expressionPanel;
  private MinimizedTab minimizedPanel;

  private BuildCircuitButton buildCircuit;
  private ImportTableButton importTable;
  private ExportTableButton exportTable;

  Analyzer() {
    super(null); // not associated with a project

    menuListener = new AnalyzerMenuListener(menubar);

    ioPanel = new VariableTab(model.getInputs(), model.getOutputs(), menubar);
    truthTablePanel = new TableTab(model.getTruthTable());
    expressionPanel = new ExpressionTab(model, menubar);
    minimizedPanel = new MinimizedTab(model, menubar);
    importTable = new ImportTableButton(this, model);
    buildCircuit = new BuildCircuitButton(this, model);
    exportTable = new ExportTableButton(this, model);

    tabbedPane = new JTabbedPane();
    addTab(IO_TAB, ioPanel);
    addTab(TABLE_TAB, truthTablePanel);
    addTab(EXPRESSION_TAB, expressionPanel);
    addTab(MINIMIZED_TAB, minimizedPanel);

    Container contents = getContentPane();
    JPanel vertStrut = new JPanel(null);
    vertStrut.setPreferredSize(new Dimension(0, 300));
    JPanel horzStrut = new JPanel(null);
    horzStrut.setPreferredSize(new Dimension(450, 0));
    JPanel buttonPanel = new JPanel();
    buttonPanel.add(importTable);
    buttonPanel.add(buildCircuit);
    buttonPanel.add(exportTable);
    contents.add(vertStrut, BorderLayout.WEST);
    contents.add(horzStrut, BorderLayout.NORTH);
    contents.add(tabbedPane, BorderLayout.CENTER);
    contents.add(buttonPanel, BorderLayout.SOUTH);

    LocaleManager.addLocaleListener(myLocaleListener);
    myLocaleListener.localeChanged();
    tabbedPane.addChangeListener(myChangeListener);
    setSelectedTab(0);
    myChangeListener.stateChanged(null);
  }

  private void addTab(int index, final JComponent comp) {
    tabbedPane.insertTab("Untitled", null, comp, null, index);
  }

  public AnalyzerModel getModel() {
    return model;
  }

  public void setSelectedTab(int index) {
    Object found = tabbedPane.getComponentAt(index);
    if (found instanceof AnalyzerTab) {
      model.getOutputExpressions().enableUpdates();
      ((AnalyzerTab) found).updateTab();
    } else {
      model.getOutputExpressions().disableUpdates();
    }
    tabbedPane.setSelectedIndex(index);
  }

  public abstract static class PleaseWait<T> extends JDialog {
    private SwingWorker<T, Void> worker;
    private java.awt.Component parent;
    public abstract T doInBackground() throws Exception;
    private boolean alreadyFinished = false;

    public PleaseWait(String title, java.awt.Component parent) {
      super(null, title, ModalityType.APPLICATION_MODAL);
      this.parent = parent;
      worker = new SwingWorker<T, Void>() {
        @Override
        protected T doInBackground() throws Exception {
          return PleaseWait.this.doInBackground();
        }
        @Override
        protected void done() {
          if (PleaseWait.this.isVisible())
            PleaseWait.this.dispose();
          else
            PleaseWait.this.alreadyFinished = true;
        }
      };
    }

    public T get() {
      worker.execute();
      JProgressBar progressBar = new JProgressBar();
      progressBar.setIndeterminate(true);
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(progressBar, BorderLayout.CENTER);
      panel.add(new JLabel("Please wait......."), BorderLayout.PAGE_START);
      // JButton cancel = new JButton(new AbstractAction("Cancel") {
      //   @Override
      //   public void actionPerformed(ActionEvent evt) {
      //   }
      // });
      // panel.add(cancel, BorderLayout.PAGE_END);
      add(panel);
      setPreferredSize(new Dimension(300, 70));
      setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      // setUndecorated(true);
      pack();
      setLocationRelativeTo(parent);
      try {
        try { return worker.get(300, TimeUnit.MILLISECONDS); }
        catch (TimeoutException e) { }
        if (!alreadyFinished)
          setVisible(true);
        return worker.get();
      } catch (Exception e) {
        return null;
      }
    }
  }
}

