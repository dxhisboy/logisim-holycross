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

package com.cburch.logisim.std.io;
import static com.cburch.logisim.std.Strings.S;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import com.bfh.logisim.hdlgenerator.HDLSupport;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.DirectionConfigurator;

public class Keyboard extends InstanceFactory {
  public static class Poker extends InstancePoker {
    @Override
    public void paint(InstancePainter painter) {
      KeyboardData data = getKeyboardState(painter);
      Bounds bds = painter.getInstance().getNominalBounds();
      Graphics g = painter.getGraphics();
      FontMetrics fm = g.getFontMetrics(DEFAULT_FONT);

      String str;
      int cursor;
      int dispStart;
      synchronized (data) {
        str = data.toString();
        cursor = data.getCursorPosition();
        if (!data.isDisplayValid())
          data.updateDisplay(fm);
        dispStart = data.getDisplayStart();
      }

      int asc = fm.getAscent();
      int x = bds.getX() + 8;
      if (dispStart > 0) {
        x += fm.stringWidth(str.charAt(0) + "m");
        x += fm.stringWidth(str.substring(dispStart, cursor));
      } else if (cursor >= str.length()) {
        x += fm.stringWidth(str);
      } else {
        x += fm.stringWidth(str.substring(0, cursor));
      }
      int y = bds.getY() + (bds.getHeight() + asc) / 2;
      g.drawLine(x, y - asc, x, y);
    }

    @Override
    public void keyPressed(InstanceState state, KeyEvent e) {
      KeyboardData data = getKeyboardState(state);
      boolean changed = false;
      boolean used = true;
      synchronized (data) {
        switch (e.getKeyCode()) {
        case KeyEvent.VK_BACK_SPACE:
          changed = data.backspace();
          break;
        case KeyEvent.VK_DELETE:
          changed = data.delete();
          break;
        case KeyEvent.VK_UP:
        case KeyEvent.VK_DOWN:
          // do nothing, but consume event
          break;
        case KeyEvent.VK_LEFT:
          data.moveCursorBy(-1);
          break;
        case KeyEvent.VK_RIGHT:
          data.moveCursorBy(1);
          break;
        case KeyEvent.VK_HOME:
          data.setCursor(0);
          break;
        case KeyEvent.VK_END:
          data.setCursor(Integer.MAX_VALUE);
          break;
        default:
          used = false;
        }
      }
      if (used)
        e.consume();
      if (changed)
        state.getInstance().fireInvalidated();
    }

    @Override
    public void keyTyped(InstanceState state, KeyEvent e) {
      KeyboardData data = getKeyboardState(state);
      char ch = e.getKeyChar();
      boolean changed = false;
      if (ch != KeyEvent.CHAR_UNDEFINED) {
        if (!Character.isISOControl(ch) || /* ch == '\b' || */ ch == '\n'
            || ch == FORM_FEED) {
          synchronized (data) {
            changed = data.insert(ch);
          }
          e.consume();
        }
      }
      if (changed)
        state.getInstance().fireInvalidated();
    }
  }

  public static void addToBuffer(InstanceState state, char[] newChars) {
    KeyboardData keyboardData = getKeyboardState(state);
    synchronized(keyboardData) {
      for (int i = 0; i < newChars.length; i++)
        keyboardData.insert(newChars[i]);
    }
  }

  public static int getBufferLength(Object bufferAttr) {
    if (bufferAttr instanceof Integer)
      return ((Integer) bufferAttr).intValue();
    else
      return 32;
  }

  public static int getWidth(Object widthAttr) {
    if (widthAttr instanceof Integer)
      return ((Integer) widthAttr).intValue();
    else
      return 7;
  }

  private static KeyboardData getKeyboardState(InstanceState state) {
    int bufLen = getBufferLength(state.getAttributeValue(ATTR_BUFFER));
    KeyboardData ret = (KeyboardData) state.getData();
    if (ret == null) {
      ret = new KeyboardData(bufLen);
      state.setData(ret);
    } else {
      ret.updateBufferLength(bufLen);
    }
    return ret;
  }

  static final int CLR = 0;
  static final int CK = 1;
  static final int RE = 2;
  static final int AVL = 3;
  static final int OUT = 4;

  private static final int DELAY0 = 9; // 1; 1;
  private static final int DELAY1 = 11; // 2; 1;

  static final int WIDTH = 145;
  static final int HEIGHT = 25;

  private static final Font DEFAULT_FONT = new Font("monospaced", Font.PLAIN,
      12);

  private static final char FORM_FEED = '\u000c'; // control-L

  public static final Attribute<Integer> ATTR_BUFFER = Attributes
      .forIntegerRange("buflen", S.getter("keybBufferLengthAttr"), 1, 256);

  public static final Attribute<Integer> ATTR_WIDTH = Attributes
      .forIntegerRange("asciiwidth", S.getter("asciiWidthAttr"),
          7, 8);

  public Keyboard() {
    super("Keyboard", S.getter("keyboardComponent"));
    setAttributes(new Attribute[] { ATTR_BUFFER, ATTR_WIDTH, StdAttr.EDGE_TRIGGER, StdAttr.LABEL, StdAttr.LABEL_LOC, StdAttr.LABEL_FONT},
        new Object[] { Integer.valueOf(32), Integer.valueOf(7), StdAttr.TRIG_RISING, "", Direction.NORTH, StdAttr.DEFAULT_LABEL_FONT});
    setOffsetBounds(Bounds.create(0, -15, WIDTH, HEIGHT));
    setIconName("keyboard.gif");
    setKeyConfigurator(new DirectionConfigurator(StdAttr.LABEL_LOC));
    setInstancePoker(Poker.class);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
    instance.computeLabelTextField(Instance.AVOID_LEFT);
  }

  private void updatePorts(Instance instance) {
    int asciiWidth = getWidth(instance.getAttributeValue(ATTR_WIDTH));
    Port[] ps = new Port[5];
    ps[CLR] = new Port(20, 10, Port.INPUT, 1);
    ps[CK] = new Port(0, 0, Port.INPUT, 1);
    ps[RE] = new Port(10, 10, Port.INPUT, 1);
    ps[AVL] = new Port(130, 10, Port.OUTPUT, 1);
    ps[OUT] = new Port(140, 10, Port.OUTPUT, asciiWidth);
    ps[CLR].setToolTip(S.getter("keybClearTip"));
    ps[CK].setToolTip(S.getter("keybClockTip"));
    ps[RE].setToolTip(S.getter("keybEnableTip"));
    ps[AVL].setToolTip(S.getter("keybAvailTip"));
    ps[OUT].setToolTip(S.getter("keybOutputTip"));
    instance.setPorts(ps);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == ATTR_WIDTH) {
      updatePorts(instance);
    } else if (attr == StdAttr.LABEL_LOC) {
      instance.computeLabelTextField(Instance.AVOID_LEFT);
    }
  }

  private void drawBuffer(Graphics g, FontMetrics fm, String str,
      int dispStart, int dispEnd, ArrayList<Integer> specials, Bounds bds) {
    int x = bds.getX();
    int y = bds.getY();

    g.setFont(DEFAULT_FONT);
    if (fm == null)
      fm = g.getFontMetrics();
    int asc = fm.getAscent();
    int x0 = x + 8;
    int ys = y + (HEIGHT + asc) / 2;
    int dotsWidth = fm.stringWidth("m");
    int xs;
    if (dispStart > 0) {
      g.drawString(str.substring(0, 1), x0, ys);
      xs = x0 + fm.stringWidth(str.charAt(0) + "m");
      drawDots(g, xs - dotsWidth, ys, dotsWidth, asc);
      String sub = str.substring(dispStart, dispEnd);
      g.drawString(sub, xs, ys);
      if (dispEnd < str.length()) {
        drawDots(g, xs + fm.stringWidth(sub), ys, dotsWidth, asc);
      }
    } else if (dispEnd < str.length()) {
      String sub = str.substring(dispStart, dispEnd);
      xs = x0;
      g.drawString(sub, xs, ys);
      drawDots(g, xs + fm.stringWidth(sub), ys, dotsWidth, asc);
    } else {
      xs = x0;
      g.drawString(str, xs, ys);
    }

    if (specials.size() > 0) {
      drawSpecials(specials, x0, xs, ys, asc, g, fm, str, dispStart,
          dispEnd);
    }
  }

  private void drawDots(Graphics g, int x, int y, int width, int ascent) {
    int r = width / 10;
    if (r < 1)
      r = 1;
    int d = 2 * r;
    if (2 * r + 1 * d <= width)
      g.fillOval(x + r, y - d, d, d);
    if (3 * r + 2 * d <= width)
      g.fillOval(x + 2 * r + d, y - d, d, d);
    if (5 * r + 3 * d <= width)
      g.fillOval(x + 3 * r + 2 * d, y - d, d, d);
  }

  private void drawSpecials(ArrayList<Integer> specials, int x0, int xs,
      int ys, int asc, Graphics g, FontMetrics fm, String str,
      int dispStart, int dispEnd) {
    int[] px = new int[3];
    int[] py = new int[3];
    for (Integer special : specials) {
      int code = special.intValue();
      int pos = code & 0xFF;
      int w0;
      int w1;
      if (pos == 0) {
        w0 = x0;
        w1 = x0 + fm.stringWidth(str.substring(0, 1));
      } else if (pos >= dispStart && pos < dispEnd) {
        w0 = xs + fm.stringWidth(str.substring(dispStart, pos));
        w1 = xs + fm.stringWidth(str.substring(dispStart, pos + 1));
      } else {
        continue; // this character is not in current view
      }
      w0++;
      w1--;

      int key = code >> 16;
      if (key == '\b') {
        int y1 = ys - asc / 2;
        g.drawLine(w0, y1, w1, y1);
        px[0] = w0 + 3;
        py[0] = y1 - 3;
        px[1] = w0;
        py[1] = y1;
        px[2] = w0 + 3;
        py[2] = y1 + 3;
        g.drawPolyline(px, py, 3);
      } else if (key == '\n') {
        int y1 = ys - 3;
        px[0] = w1;
        py[0] = ys - asc;
        px[1] = w1;
        py[1] = y1;
        px[2] = w0;
        py[2] = y1;
        g.drawPolyline(px, py, 3);
        px[0] = w0 + 3;
        py[0] = y1 - 3;
        px[1] = w0;
        py[1] = y1;
        px[2] = w0 + 3;
        py[2] = y1 + 3;
        g.drawPolyline(px, py, 3);
      } else if (key == FORM_FEED) {
        g.drawRect(w0, ys - asc, w1 - w0, asc);
      }
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    boolean showState = painter.getShowState();
    Graphics g = painter.getGraphics();
    Bounds bds = painter.getNominalBounds();
    painter.drawClock(CK, Direction.EAST);
    painter.drawBounds();
    painter.drawPort(CLR);
    painter.drawPort(RE);
    painter.drawPort(AVL);
    painter.drawPort(OUT);
    painter.drawLabel();

    if (showState) {
      String str;
      int dispStart;
      int dispEnd;
      ArrayList<Integer> specials = new ArrayList<Integer>();
      FontMetrics fm = null;
      KeyboardData state = getKeyboardState(painter);
      synchronized (state) {
        str = state.toString();
        specials.addAll(state.getSpecials());
        if (!state.isDisplayValid()) {
          fm = g.getFontMetrics(DEFAULT_FONT);
          state.updateDisplay(fm);
        }
        dispStart = state.getDisplayStart();
        dispEnd = state.getDisplayEnd();
      }
      if (str.length() > 0)
        drawBuffer(g, fm, str, dispStart, dispEnd, specials, bds);
    } else {
      int len = getBufferLength(painter.getAttributeValue(ATTR_BUFFER));
      String str = S.fmt("keybDesc", "" + len);
      FontMetrics fm = g.getFontMetrics();
      int x = bds.getX() + (WIDTH - fm.stringWidth(str)) / 2;
      int y = bds.getY() + (HEIGHT + fm.getAscent()) / 2;
      g.drawString(str, x, y);
    }
  }

  @Override
  public void propagate(InstanceState circState) {
    Object trigger = circState.getAttributeValue(StdAttr.EDGE_TRIGGER);
    KeyboardData state = getKeyboardState(circState);
    Value clear = circState.getPortValue(CLR);
    Value clock = circState.getPortValue(CK);
    Value enable = circState.getPortValue(RE);
    char c;

    synchronized (state) {
      Value lastClock = state.setLastClock(clock);
      if (clear == Value.TRUE) {
        state.clear();
      } else if (enable != Value.FALSE) {
        boolean go;
        if (trigger == StdAttr.TRIG_FALLING) {
          go = lastClock == Value.TRUE && clock == Value.FALSE;
        } else {
          go = lastClock == Value.FALSE && clock == Value.TRUE;
        }
        if (go) {
          char d = state.dequeue();
        }
      }

      c = state.getCurrentChar();
    }
    int w = getWidth(circState.getAttributeValue(ATTR_WIDTH));
    Value out = Value.createKnown(BitWidth.create(w), c & (w == 7 ? 0x7F : 0xFF)); // always 7 bit clean instead?
    circState.setPort(OUT, out, DELAY0);
    circState.setPort(AVL, c != '\0' ? Value.TRUE : Value.FALSE, DELAY1);
  }

  @Override
  public String getHDLNamePrefix(Component comp) { return "Keyboard"; }

  @Override
  public HDLSupport getHDLSupport(HDLSupport.ComponentContext ctx) {
    if (ctx.lang.equals("VHDL"))
      return new KeyboardHDLGenerator(ctx);
    else
      return null;
  }

}
