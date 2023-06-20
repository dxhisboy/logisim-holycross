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

package com.cburch.logisim.gui.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;

class MenuItemCheckImpl extends JCheckBoxMenuItem implements MenuItem {

  private static final long serialVersionUID = 1L;
  private MenuItemHelper helper;

  public MenuItemCheckImpl(Menu menu, LogisimMenuItem menuItem) {
    helper = new MenuItemHelper(this, menu, menuItem);
    super.addActionListener(e -> actionPerformed(e));
    setEnabled(true);
  }

  public void actionPerformed(ActionEvent event) {
    helper.actionPerformed(event);
  }

  @Override
  public void addActionListener(ActionListener l) {
    helper.addActionListener(l);
  }

  public boolean hasListeners() {
    return helper.hasListeners();
  }

  @Override
  public void removeActionListener(ActionListener l) {
    helper.removeActionListener(l);
  }

  @Override
  public void setEnabled(boolean value) {
    helper.setEnabled(value);
    super.setEnabled(value && helper.hasListeners());
  }
}
