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
package com.cburch.logisim.gui.generic;
import static com.cburch.logisim.gui.main.Strings.S;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

import com.cburch.logisim.util.Errors;
import com.cburch.logisim.util.JDialogOk;
import com.cburch.logisim.util.JInputComponent;
import com.cburch.logisim.util.JInputDialog;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;

@SuppressWarnings("serial")
public class AttrTable extends JPanel implements LocaleListener {

  private static final Object SENTINEL = new Object();

  public static interface PopupActor {
    public Object doPopup(); // returns null if cancelled
  }

  public static class PopupEditor extends JLabel {
    TableCellEditor editor;
    PopupActor pop;
    Object result;
    public PopupEditor(String text, TableCellEditor editor, PopupActor pop) {
      super(text);
      this.editor = editor;
      this.pop = pop;
    }
    public void trigger() {
      SwingUtilities.invokeLater(() -> {
        result = pop.doPopup();
        if (result != null)
          editor.stopCellEditing();
        else
          editor.cancelCellEditing();
      });
    }
    public Object getResult() {
      return result;
    }
  }

  private class CellEditor
    implements TableCellEditor, FocusListener, ActionListener {

    LinkedList<CellEditorListener> listeners = new LinkedList<CellEditorListener>();
    AttrTableModelRow currentRow;
    AttrTableModelRow[] currentRows;
    int[] currentRowIndexes;
    Component currentEditor;
    boolean multiEditActive = false;

    @Override
    public void actionPerformed(ActionEvent e) {
      if (e.getModifiers() != 0)
        stopCellEditing(); // selection via mouse button
    }

    @Override
    public void addCellEditorListener(CellEditorListener l) {
      listeners.add(l);
    }

    @Override
    public void cancelCellEditing() {
      if (currentEditor != null) {
        currentEditor = null;
        fireEditingCanceled();
      }
    }

    public void fireEditingCanceled() {
      ChangeEvent e = new ChangeEvent(AttrTable.this);
      for (CellEditorListener l : new ArrayList<CellEditorListener>(listeners))
        l.editingCanceled(e);
    }

    public void fireEditingStopped() {
      ChangeEvent e = new ChangeEvent(AttrTable.this);
      for (CellEditorListener l : new ArrayList<CellEditorListener>(listeners))
        l.editingStopped(e);
    }

    @Override
    public void focusGained(FocusEvent e) { }

    @Override
    public void focusLost(FocusEvent e) {
      Object dst = e.getOppositeComponent();
      // If focus is shifting from the cell editor to somewhere else within the
      // same JTable (or AttrTable panel), the JTable may have caused the loss
      // of focus (e.g. when user presses escape), or at least the JTable maybe
      // handles this case properly. But if focus leaves the JTable (or Attr
      // panel) entirely and goes somwehere else in the application, we want to
      // stop editing and accept any partial input. If we leave the application
      // entirely, we can leave the editor open.
      // null
      if (dst instanceof Component) {
        Component p = (Component) dst;
        while (p != null && !(p instanceof Window)) {
          if (p == AttrTable.this) {
            // Focus stayed within JTable, allow JTable to handle this.
            return;
          }
          p = p.getParent();
        }
        // Focus transferred outside JTable, but stayed in application.
        editor.stopCellEditing();
      } else {
        // Focus left application.
      }
    }

    @Override
    public Object getCellEditorValue() {
      // Return a bogus object here, because stopCellEditing() takes care of the
      // setAttr() before JTable has a chance to do it.
      return SENTINEL;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Component getTableCellEditorComponent(JTable table,
        Object value, boolean isSelected, int rowIndex, int columnIndex) {

      if (currentEditor != null) {
        // cleanup old editor before creating a new one
        Component comp = currentEditor;
        currentEditor = null;
        comp.transferFocus();
      }

      AttrTableModel attrModel = tableModel.attrModel;
      AttrTableModelRow row = attrModel.getRow(rowIndex);

      if (columnIndex == 0)
        return new JLabel(row.getLabel());

      AttrTableModelRow[] rows = null;
      int rowIndexes[] = null;
      multiEditActive = false;

      Component editor = row.getEditor(parent);
      if (editor instanceof JComboBox) {
        ((JComboBox) editor).addActionListener(this);
        editor.addFocusListener(this);
        rowIndexes = table.getSelectedRows();
        if (isSelected && rowIndexes.length > 1) {
          multiEditActive = true;
          rows = new AttrTableModelRow[rowIndexes.length];
          for (int i = 0; i < rowIndexes.length; i++) {
            rows[i] = attrModel.getRow(rowIndexes[i]);
            if (!row.multiEditCompatible(rows[i])) {
              multiEditActive = false;
              rowIndexes = null;
              rows = null;
              break;
            }
          }
        } else {
          rowIndexes = null;
        }
      } else if (editor instanceof JInputDialog) {
        JInputDialog dlg = (JInputDialog) editor;
        String text = row.getDisplayString();
        editor = new PopupEditor(text, this, 
            () -> { dlg.setVisible(true); return dlg.getValue(); });
      } else if (editor instanceof JInputComponent) {
        JInputComponent input = (JInputComponent) editor;
        MyDialog dlg = new MyDialog(input);
        String text = row.getDisplayString();
        editor = new PopupEditor(text, this, dlg);
      } else {
        editor.addFocusListener(this);
      }

      currentRow = row;
      currentRows = rows;
      currentRowIndexes = rowIndexes;
      currentEditor = editor;

      if (editor instanceof PopupEditor)
        ((PopupEditor)editor).trigger();

      return editor;
    }

    public boolean isEditing(AttrTableModelRow row) {
      if (currentRow == row)
        return true;
      if (currentRows == null)
        return false;
      for (AttrTableModelRow r : currentRows)
        if (r == row)
          return true;
      return false;
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) { return true; }

    @Override
    public void removeCellEditorListener(CellEditorListener l) {
      listeners.remove(l);
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
      return !multiEditActive;
    }

    @Override
    public boolean stopCellEditing() {
      // Try to accept new value from editor.
      int row = table.convertRowIndexToModel(table.getEditingRow());
      if (row < 0 || currentEditor == null)
        return true; // false stop, nothing is being edited right now

      Component comp = currentEditor;
      currentEditor = null;

      Object value = null;
      if (comp instanceof JTextField) {
        value = ((JTextField) comp).getText();
      } else if (comp instanceof JComboBox) {
        value = ((JComboBox) comp).getSelectedItem();
      } else if (comp instanceof PopupEditor) {
        value = ((PopupEditor) comp).getResult();
      } else {
        JOptionPane.showMessageDialog(parent, "internal error: "
            + " unknown value in editor: " + (comp == null ? null : comp.getClass()),
            S.get("attributeChangeInvalidTitle"),
            JOptionPane.WARNING_MESSAGE);
      }

      if (value == null) {
        // must have been cancelled...
        fireEditingCanceled();
        return true;
      }
      
      // Normally, we could just fireEditingStopped(), because JTable has a
      // listener for editingStopped() that will call setValueAt(row, col, val).
      // We do it here instead, for two reasons:
      //  (1) For multiEditActive, we need to set multiple rows, not just the
      //      current row.
      //  (2) In cases where the value is invalid we want to cancelEditing()
      //      instead. If we were to just call editingStopped(), JTable would
      //      ultimately call TableModelAdapter.setValueAt(), which would catch
      //      and swallow an exception thrown somewhere down the line by
      //      setValue(). But when that error handler it tries to display the
      //      error, the editor gets a focus-lost event, causing another attempt
      //      to stop editing, and an immediate second error message. So we need
      //      to setValueAt() here instead. The one in JTable.editingStopped()
      //      will be redundant.
      try {
        getAttrTableModel().getRow(row).setValue(parent, value);
        fireEditingStopped();
        // If the above setValue() didn't throw an exception, then we assume none
        // of these next setValue() calls will either. So no rollback needed.
        if (multiEditActive) {
          try {
            for (int r : currentRowIndexes)
              if (r != row)
                getAttrTableModel().getRow(r).setValue(parent, value);
          } catch (AttrTableSetException e) {
            Errors.title(S.get("attributeChangeInvalidTitle")).show(
                "Could not change multiple attributes.", e);
          }
          tableModel.fireTableChanged(); // repaints the other changed rows
        }
      } catch (AttrTableSetException e) {
        fireEditingCanceled();
        JOptionPane.showMessageDialog(parent, e.getMessage(),
            S.get("attributeChangeInvalidTitle"),
            JOptionPane.WARNING_MESSAGE);
      }
      return true;
    }
  }

  private static class MyDialog extends JDialogOk
    implements PopupActor {

    JInputComponent input;
    Object value;

    public MyDialog(JInputComponent input) {
      super(S.get("attributeDialogTitle"));
      configure(input);
    }

    private void configure(JInputComponent input) {
      this.input = input;

      // Thanks to Christophe Jacquet, who contributed a fix to this
      // so that when the dialog is resized, the component within it
      // is resized as well. (Tracker #2024479)
      JPanel p = new JPanel(new BorderLayout());
      p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      // Hide the JFileChooser buttons, since we already have the
      // MyDialog ones
      if (input instanceof JFileChooser)
        ((JFileChooser) input).setControlButtonsAreShown(false);
      p.add((JComponent) input, BorderLayout.CENTER);
      getContentPane().add(p, BorderLayout.CENTER);

      pack();
    }

    @Override
    public void okClicked() {
      value = input.getValue();
    }

    @Override
    public void cancelClicked() {
      value = null;
    }

    @Override
    public Object doPopup() {
      setVisible(true);
      return value;
    }
  }

  private static class NullAttrModel implements AttrTableModel {

    @Override
    public void addAttrTableModelListener(AttrTableModelListener listener) {
    }

    @Override
    public AttrTableModelRow getRow(int rowIndex) {
      return null;
    }

    @Override
    public int getRowCount() {
      return 0;
    }

    @Override
    public boolean isRowValueEditable(int rowIndex) {
      return false;
    }

    @Override
    public String getTitle() {
      return null;
    }

    @Override
    public void removeAttrTableModelListener(AttrTableModelListener listener) {
    }
  }

  private class TableModelAdapter
    implements TableModel, AttrTableModelListener {

    Window parent;
    LinkedList<TableModelListener> listeners;
    AttrTableModel attrModel;

    TableModelAdapter(Window parent, AttrTableModel attrModel) {
      this.parent = parent;
      this.listeners = new LinkedList<TableModelListener>();
      this.attrModel = attrModel;
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
      listeners.add(l);
    }

    @Override
    public void attrStructureChanged(AttrTableModelEvent e) {
      if (e.getSource() != attrModel) {
        attrModel.removeAttrTableModelListener(this);
        return;
      }
      TableCellEditor ed = table.getCellEditor();
      if (ed != null) {
        ed.cancelCellEditing();
      }
      fireTableChanged();
    }

    @Override
    public void attrTitleChanged(AttrTableModelEvent e) {
      if (e.getSource() != attrModel) {
        attrModel.removeAttrTableModelListener(this);
        return;
      }
      updateTitle();
    }

    @Override
    public void attrValueChanged(AttrTableModelEvent e) {
      if (e.getSource() != attrModel) {
        attrModel.removeAttrTableModelListener(this);
        return;
      }
      int row = e.getRowIndex();

      TableCellEditor ed = table.getCellEditor();
      if (row >= 0 && ed instanceof CellEditor
          && ((CellEditor)ed).isEditing(attrModel.getRow(row)))
        ed.cancelCellEditing();

      fireTableChanged();
    }

    void fireTableChanged() {
      TableModelEvent e = new TableModelEvent(this);
      for (TableModelListener l : new ArrayList<TableModelListener>(listeners))
        l.tableChanged(e);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return String.class;
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
      if (columnIndex == 0) {
        return "Attribute";
      } else {
        return "Value";
      }
    }

    @Override
    public int getRowCount() {
      return attrModel.getRowCount();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      if (columnIndex == 0) {
        return attrModel.getRow(rowIndex).getLabel();
      } else {
        return attrModel.getRow(rowIndex).getDisplayString();
      }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex > 0
          && attrModel.isRowValueEditable(rowIndex);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
      listeners.remove(l);
    }

    void setAttrTableModel(AttrTableModel value) {
      if (attrModel != value) {
        attrModel.removeAttrTableModelListener(this);
        attrModel = value;
        attrModel.addAttrTableModelListener(this);
        fireTableChanged();
      }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
      if (columnIndex <= 0 || value == SENTINEL)
        return;
      throw new IllegalArgumentException("AttrTable.TableModelAdapter.setValueAt");
    }
  }

  private static class TitleLabel extends JLabel {
    @Override
    public Dimension getMinimumSize() {
      Dimension ret = super.getMinimumSize();
      return new Dimension(1, ret.height);
    }
  }

  private static final AttrTableModel NULL_ATTR_MODEL = new NullAttrModel();
  private Window parent;
  private boolean titleEnabled;
  private JLabel title;
  private JTable table;
  private TableModelAdapter tableModel;
  private CellEditor editor = new CellEditor();

  public AttrTable(Window parent) {
    super(new BorderLayout());
    this.parent = parent;

    titleEnabled = true;
    title = new TitleLabel();
    title.setHorizontalAlignment(SwingConstants.CENTER);
    title.setVerticalAlignment(SwingConstants.CENTER);
    tableModel = new TableModelAdapter(parent, NULL_ATTR_MODEL);
    table = new JTable(tableModel);
    table.setDefaultEditor(Object.class, editor);
    table.setTableHeader(null);
    table.setRowHeight(20);

    Font baseFont = title.getFont();
    int titleSize = Math.round(baseFont.getSize() * 1.2f);
    Font titleFont = baseFont.deriveFont((float) titleSize).deriveFont(
        Font.BOLD);
    title.setFont(titleFont);
    Color bgColor = new Color(240, 240, 240);
    setBackground(bgColor);
    table.setBackground(bgColor);
    Object renderer = table.getDefaultRenderer(String.class);
    if (renderer instanceof JComponent) {
      ((JComponent) renderer).setBackground(Color.WHITE);
    }

    JPanel propPanel = new JPanel(new BorderLayout(0, 0));
    JScrollPane tableScroll = new JScrollPane(table);

    propPanel.add(title, BorderLayout.PAGE_START);
    propPanel.add(tableScroll, BorderLayout.CENTER);

    this.add(propPanel, BorderLayout.CENTER);

    LocaleManager.addLocaleListener(this);
    localeChanged();
  }

  public AttrTableModel getAttrTableModel() {
    return tableModel.attrModel;
  }

  public boolean getTitleEnabled() {
    return titleEnabled;
  }

  @Override
  public void localeChanged() {
    updateTitle();
    tableModel.fireTableChanged();
  }

  public void setAttrTableModel(AttrTableModel value) {

    TableCellEditor editor = table.getCellEditor();

    if (editor != null)
      table.getCellEditor().cancelCellEditing();

    tableModel.setAttrTableModel(value == null ? NULL_ATTR_MODEL : value);
    updateTitle();
  }

  public void setTitleEnabled(boolean value) {
    titleEnabled = value;
    updateTitle();
  }

  private void updateTitle() {
    if (titleEnabled) {
      String text = tableModel.attrModel.getTitle();
      if (text == null) {
        title.setVisible(false);
      } else {
        title.setText(text);
        title.setVisible(true);
      }
    } else {
      title.setVisible(false);
    }
  }
}
