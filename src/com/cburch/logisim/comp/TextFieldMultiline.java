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

package com.cburch.logisim.comp;

import java.awt.Font;
import java.awt.Graphics;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.util.GraphicsUtil;

public class TextFieldMultiline extends TextField {

  private String lines[] = new String[0];

  public TextFieldMultiline(int x, int y, int halign, int valign) {
    this(x, y, halign, valign, null);
  }

  public TextFieldMultiline(int x, int y, int halign, int valign, Font font) {
    super(x, y, halign, valign, font);
  }

  public void draw(Graphics g) {
    GraphicsUtil.drawText(g, font, lines, x, y, halign, valign);
  }

  public Bounds getBounds(Graphics g) {
    return Bounds.create(GraphicsUtil.getTextBounds(g, font, lines, x, y, halign, valign));
  }

  public TextFieldCaret getCaret(Canvas canvas, Graphics g, int pos) {
    return new TextFieldMultilineCaret(canvas, this, g, pos);
  }

  public TextFieldCaret getCaret(Canvas canvas, Graphics g, int x, int y) {
    return new TextFieldMultilineCaret(canvas, this, g, x, y);
  }

  public void setText(String text) {
    if (!text.equals(this.text)) {
      lines = text.split("\n");
      super.setText(text);
    }
  }

}
