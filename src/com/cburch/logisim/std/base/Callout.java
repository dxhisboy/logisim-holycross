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

package com.cburch.logisim.std.base;
import static com.cburch.logisim.std.Strings.S;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Collections;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.CustomHandles;
import com.cburch.logisim.tools.Reshapable;
import com.cburch.logisim.tools.SetAttributeAction;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;

public class Callout extends Text {

  public static final Attribute<Integer> ATTR_DX =
      Attributes.forInteger("dx", S.getter("calloutDeltaX"));
  public static final Attribute<Integer> ATTR_DY =
      Attributes.forInteger("dy", S.getter("calloutDeltaY"));

  public static final Callout FACTORY = new Callout();

  private Callout() {
    super("Callout", S.getter("calloutComponent"));
    setIconName("callout.png");
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new CalloutAttributes();
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrsBase, Graphics g) {
    CalloutAttributes attrs = (CalloutAttributes) attrsBase;
    String text = attrs.getText();
    if (text == null || text.equals("")) {
      return Bounds.EMPTY_BOUNDS;
    } else {
      Bounds bds = attrs.getOffsetBounds();
      if (bds == null) {
        int halign = attrs.getHorizontalAlign();
        int valign = attrs.getVerticalAlign();
        Font font = attrs.getFont();
        if (g == null) {
          // This should not happen in most (any) cases...
          Bounds tbds = StringUtil.estimateBounds(text, font, halign, valign);
          bds = tbds.add(attrs.getDx(), attrs.getDy());
        } else {
          String lines[] = text.split("\n");
          Rectangle r = GraphicsUtil.getTextBounds(g, font, lines, 0, 0, halign, valign);
          Bounds tbds = Bounds.create(r).expand(4);
          bds = tbds.add(attrs.getDx(), attrs.getDy());
          attrs.setOffsetBounds(bds);
        }
      }
      return bds;
    }
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == ATTR_HALIGN || attr == ATTR_VALIGN) {
      configureLabel(instance);
    }
  }
  
  @Override
  public void paint(InstancePainter painter, boolean border) {
    CalloutAttributes attrs = (CalloutAttributes) painter.getAttributeSet();
    int dx = attrs.getDx();
    int dy = attrs.getDy();
    Location loc = painter.getLocation();
    Location focus = loc.translate(dx, dy);
    paint(painter, border, focus);
  }

  // @Override public
  void drawReshaping(InstancePainter painter, Location handle, int rdx, int rdy) {
    CalloutAttributes attrs = (CalloutAttributes) painter.getAttributeSet();
    int dx = attrs.getDx();
    int dy = attrs.getDy();
    Location loc = painter.getLocation();
    Location focus = loc.translate(dx + rdx, dy + rdy);
    paint(painter, false, focus);
  }

  void paint(InstancePainter painter, boolean border, Location focus) {
    CalloutAttributes attrs = (CalloutAttributes) painter.getAttributeSet();
    String text = attrs.getText();
    if (text == null || text.equals(""))
      return;
    int halign = attrs.getHorizontalAlign();
    int valign = attrs.getVerticalAlign();
    Font font = attrs.getFont();

    Graphics g = painter.getGraphics();
    Location loc = painter.getLocation();

    String lines[] = text.split("\n");
    Rectangle r = GraphicsUtil.getTextBounds(g, font, lines, 0, 0, halign, valign);
    Bounds tbds = Bounds.create(r).expand(4);
    Bounds bds = tbds.add(focus.getX() - loc.getX(), focus.getY() - loc.getY());
    if (attrs.setOffsetBounds(bds)) {
      Instance instance = painter.getInstance();
      if (instance != null)
        instance.recomputeBounds();
    }

    g.setColor(Color.RED);
    //      o---------o---------o  V_TOP
    //      |         |         |
    //      |         |         |
    //      o---------o---------o  V_CENTER
    //      |         |         |
    //      o---------X---------o  V_BASELINE
    //      |         |         |
    //      o---------o---------o  V_BOTTOM
    //   H_LEFT   H_CENTER   H_RIGHT

    int wid = tbds.getWidth();
    int hgt = tbds.getHeight();
    int left = loc.getX() + tbds.getX();
    int right = left + wid;
    int top = loc.getY() + tbds.getY();
    int bot = top + hgt;
    if (valign == GraphicsUtil.V_TOP && halign == GraphicsUtil.H_LEFT) {
      // top bar, left pivot
      g.drawLine(left, top, right, top);
      g.drawLine(left, top, focus.getX(), focus.getY());
    } else if (valign == GraphicsUtil.V_TOP && halign == GraphicsUtil.H_RIGHT) {
      // top bar, left pivot
      g.drawLine(left, top, right, top);
      g.drawLine(left+wid, top, focus.getX(), focus.getY());
    } else if (valign == GraphicsUtil.V_TOP && halign == GraphicsUtil.H_CENTER) {
      // top bar, center pivot
      g.drawLine(left, top, right, top);
      g.drawLine(left+wid/2, top, focus.getX(), focus.getY());
    } else if (valign == GraphicsUtil.V_BOTTOM && halign == GraphicsUtil.H_LEFT) {
      // bottom bar, left pivot
      g.drawLine(left, bot, right, bot);
      g.drawLine(left, bot, focus.getX(), focus.getY());
    } else if (valign == GraphicsUtil.V_BOTTOM && halign == GraphicsUtil.H_RIGHT) {
      // bottom bar, left pivot
      g.drawLine(left, bot, right, bot);
      g.drawLine(left+wid, bot, focus.getX(), focus.getY());
    } else if (valign == GraphicsUtil.V_BOTTOM && halign == GraphicsUtil.H_CENTER) {
      // bottom bar, center pivot
      g.drawLine(left, bot, right, bot);
      g.drawLine(left+wid/2, bot, focus.getX(), focus.getY());
    } else if (valign == GraphicsUtil.V_BASELINE && halign == GraphicsUtil.H_LEFT) {
      // left bar, auto pivot
      g.drawLine(left, top, left, bot);
      if (focus.getX() >= left && focus.getY() >= top + hgt/2)
        g.drawLine(left, top, focus.getX(), focus.getY());
      else if (focus.getX() >= left)
        g.drawLine(left, bot, focus.getX(), focus.getY());
      else if (top - focus.getY() >= left - focus.getX())
        g.drawLine(left, top, focus.getX(), focus.getY());
      else if (focus.getY() - bot >= left - focus.getX())
        g.drawLine(left, bot, focus.getX(), focus.getY());
    } else if (valign == GraphicsUtil.V_BASELINE && halign == GraphicsUtil.H_RIGHT) {
      // right bar, auto pivot
    } else if (valign == GraphicsUtil.V_BASELINE && halign == GraphicsUtil.H_CENTER) {
      // bottom bar, auto pivot
    } else if (valign == GraphicsUtil.V_CENTER && halign == GraphicsUtil.H_LEFT) {
      // left bar, center pivot
      g.drawLine(left, top, left, bot);
      g.drawLine(left, top+hgt/2, focus.getX(), focus.getY());
    } else if (valign == GraphicsUtil.V_CENTER && halign == GraphicsUtil.H_RIGHT) {
      // right bar, center pivot
      g.drawLine(left, top, left, bot);
      g.drawLine(left, top+hgt/2, focus.getX(), focus.getY());
    } else if (valign == GraphicsUtil.V_CENTER && halign == GraphicsUtil.H_CENTER) {
      // auto bar, auto pivot
    }

    if (border) {
      g.setColor(Color.LIGHT_GRAY);
      g.setColor(Color.GREEN);
      g.drawRect(left, top, wid, hgt);
    }
    g.setColor(Color.BLACK);
    GraphicsUtil.drawText(g, font, lines, loc.getX(), loc.getY(), halign, valign);
  }

  /* FIXME: use text instead */
  @Override
  public void drawHandles(ComponentDrawContext context) {
    Graphics g = context.getGraphics();
    g.setColor(Color.GRAY);
    InstancePainter painter = context.getInstancePainter();
    CalloutAttributes attrs = (CalloutAttributes) painter.getAttributeSet();
    String text = attrs.getText();
    int halign = attrs.getHorizontalAlign();
    int valign = attrs.getVerticalAlign();
    Font font = attrs.getFont();
    String lines[] = text.split("\n");
    Rectangle r = GraphicsUtil.getTextBounds(g, font, lines, 0, 0, halign, valign);
    Bounds tbds = Bounds.create(r).expand(4);
    Location loc = painter.getLocation();
    Bounds box = tbds.translate(loc.getX(), loc.getY());
    g.drawRect(box.getX(), box.getY(), box.getWidth(), box.getHeight());
    painter.drawHandles();
    // int dx = attrs.getDx();
    // int dy = attrs.getDy();
    // Location focus = loc.translate(dx, dy);
    // context.drawHandle(focus);
  }

  private /*static*/ class CalloutReshaper implements Reshapable {
    Instance instance;
    CalloutReshaper(Instance i) { instance = i; }

    @Override
    public Collection<Location> getReshapeHandles() {
      CalloutAttributes attrs = (CalloutAttributes) instance.getAttributeSet();
      int dx = attrs.getDx();
      int dy = attrs.getDy();
      Location loc = instance.getLocation();
      Location focus = loc.translate(dx, dy);
      return Collections.singletonList(focus);
    }
    @Override
    public void drawReshaping(InstancePainter painter, 
        Location handle, int rdx, int rdy) {
      Callout.this.drawReshaping(painter, handle, rdx, rdy);
    }
    public void doReshapeAction(Project proj, Circuit circ, Component comp,
        Location handle, int rdx, int rdy) {
      CalloutAttributes attrs = (CalloutAttributes)comp.getAttributeSet();
      int dx = attrs.getDx();
      int dy = attrs.getDy();
      SetAttributeAction act = new SetAttributeAction(circ, S.getter("calloutReshape"));
      act.set(comp, ATTR_DX, dx + rdx);
      act.set(comp, ATTR_DY, dy + rdy);
      proj.doAction(act);
    }
  }

  @Override
  public Object getInstanceFeature(Instance instance, Object key) {
    if (key == Reshapable.class)
      return new CalloutReshaper(instance);
    else
      return super.getInstanceFeature(instance, key);
  }
}
