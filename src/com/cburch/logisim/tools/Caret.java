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

package com.cburch.logisim.tools;

import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.gui.menu.EditHandler;

public interface Caret {

  public void addCaretListener(CaretListener e);

  public default EditHandler getEditHandler() { return null; }

  public void cancelEditing();

  public void commitText(String text);

  public void draw(Graphics g);

  public Bounds getBounds(Graphics g);

  public String getText();

  public void keyPressed(KeyEvent e);

  public void keyReleased(KeyEvent e);

  public void keyTyped(KeyEvent e);

  public void mouseDragged(MouseEvent e);

  public void mousePressed(MouseEvent e);

  public void mouseReleased(MouseEvent e);

  public void removeCaretListener(CaretListener e);

  public void stopEditing();
}
