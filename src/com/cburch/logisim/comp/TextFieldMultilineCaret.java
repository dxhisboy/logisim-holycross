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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

import com.cburch.draw.util.TextMetrics;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.util.GraphicsUtil;

class TextFieldMultilineCaret extends TextFieldCaret {
  private TextFieldMultiline field;

  public TextFieldMultilineCaret(Canvas canvas, TextFieldMultiline field, Graphics g, int pos) {
    super(canvas, field, g, pos);
    this.field = field;
  }

  public TextFieldMultilineCaret(Canvas canvas, TextFieldMultiline field, Graphics g, int x, int y) {
    this(canvas, field, g, 0);
    cursor = anchor = findCaret(x, y);
  }

  public void draw(Graphics g) {
    String lines[] = curText.split("\n", -1); // keep blank lines at end
    int x = field.getX();
    int y = field.getY();
    int halign = field.getHAlign();
    int valign = field.getVAlign();
    Font font = field.getFont();
    if (font != null)
      g.setFont(font);

    // draw boundary
    Bounds box = getBounds(g);
    g.setColor(EDIT_BACKGROUND);
    g.fillRect(box.getX(), box.getY(), box.getWidth(), box.getHeight());
    g.setColor(EDIT_BORDER);
    g.drawRect(box.getX(), box.getY(), box.getWidth(), box.getHeight());

    // draw selection
    if (cursor != anchor) {
      g.setColor(SELECTION_BACKGROUND);
      Rectangle p = GraphicsUtil.getTextCursor(g, font, lines, x, y, cursor < anchor ? cursor : anchor, halign, valign);
      Rectangle e = GraphicsUtil.getTextCursor(g, font, lines, x, y, cursor < anchor ? anchor : cursor, halign, valign);
      if (p.y == e.y) {
        g.fillRect(p.x, p.y - 1, e.x - p.x + 1, e.height + 2);
      } else {
        int lx = box.getX()+3;
        int rx = box.getX()+box.getWidth()-3;
        g.fillRect(p.x, p.y - 1, rx - p.x + 1, (e.y - p.y) + 1);
        g.fillRect(lx, p.y + e.height, rx - lx + 1, (e.y - p.y) - e.height);
        g.fillRect(lx, p.y + e.height, e.x - lx + 1, (e.y - p.y) + 1);
      }
    }

    // draw text
    g.setColor(Color.BLACK);
    GraphicsUtil.drawText(g, lines, x, y, halign, valign);

    // draw cursor
    if (cursor == anchor) {
      Rectangle p = GraphicsUtil.getTextCursor(g, font, lines, x, y, cursor, halign, valign);
      if (p != null)
        g.drawLine(p.x, p.y, p.x, p.y + p.height);
    }
  }

  public Bounds getBounds(Graphics g) {
    String lines[] = curText.split("\n", -1); // keep blank lines at end
    int x = field.getX();
    int y = field.getY();
    int halign = field.getHAlign();
    int valign = field.getVAlign();
    Font font = field.getFont();
    Bounds bds = Bounds.create(GraphicsUtil.getTextBounds(g, font, lines, x, y, halign, valign));
    Bounds box = bds.add(field.getBounds(g)).expand(3);
    return box;
  }

  protected void moveCaret(int move, boolean shift) {
    if (!shift)
      normalizeSelection();

    if (move == -3 || move == +3) { // start/end of line
      if (!shift && cursor != anchor)
        cancelSelection(move);
      if (move < 0) {
        while (cursor > 0 && curText.charAt(cursor-1) != '\n')
          cursor--;
      } else {
        while (cursor < curText.length() && curText.charAt(cursor) != '\n')
          cursor++;
      }
    } else if (move == -4 || move == +4) { // down/up a line
      if (!shift && cursor != anchor)
        cancelSelection(move);
      int dy = move < 0 ? -1 : +1;
      String lines[] = curText.split("\n", -1); // keep blank lines at end
      TextMetrics tm = new TextMetrics(g);
      int halign = field.getHAlign();
      int valign = field.getVAlign();
      Rectangle r = GraphicsUtil.getTextCursor(g, field.getFont(), lines, 0, 0, cursor, halign, valign);
      int newpos = cursor;
      if (r != null) {
        newpos = GraphicsUtil.getTextPosition(g, field.getFont(), lines,
            r.x, r.y + tm.ascent/2 + dy * tm.height, halign, valign);
      }
      if (newpos != cursor)
        cursor = newpos;
      else
        cursor = dy < 0 ? 0 : curText.length();
    } else {
      super.moveCaret(move, shift);
      return;
    }

    if (!shift)
      anchor = cursor;
  }

  protected void normalKeyPressed(KeyEvent e, boolean shift) {
    if (e.getKeyCode() != KeyEvent.VK_ENTER)
      super.normalKeyPressed(e, shift);
  }

  protected boolean allowedCharacter(char c) {
    return (c != KeyEvent.CHAR_UNDEFINED)
        && (c == '\n' || c == '\t' || !Character.isISOControl(c));
  }

  protected int findCaret(int x, int y) {
    x -= field.getX();
    y -= field.getY();
    int halign = field.getHAlign();
    int valign = field.getVAlign();
    String lines[] = curText.split("\n", -1); // keep blank lines at end
    return GraphicsUtil.getTextPosition(g, field.getFont(), lines, x, y, halign, valign);
  }

}
