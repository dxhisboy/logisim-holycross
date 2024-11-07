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

import java.awt.Graphics;
import java.awt.Color;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.cburch.draw.shapes.DrawAttr;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.instance.InstanceDataSingleton;

public class LedVectorShape extends DynamicElement {
  static final int DEFAULT_LENGTH = 5;

  public LedVectorShape(int x, int y, DynamicElement.Path p) {
    super(p, Bounds.create(x, y, DEFAULT_LENGTH * p.elt[0].getInstance().getAttributeValue(LedVector.ATTR_ARRAY_WIDTH),
        DEFAULT_LENGTH));
  }

  @Override
  public boolean contains(Location loc, boolean assumeFilled) {
    int x = bounds.getX();
    int y = bounds.getY();
    int w = bounds.getWidth();
    int h = bounds.getHeight();
    int qx = loc.getX();
    int qy = loc.getY();
    double dx = qx - (x + 0.5 * w);
    double dy = qy - (y + 0.5 * h);
    double sum = (dx * dx) / (w * w) + (dy * dy) / (h * h);
    return sum <= 0.25;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return UnmodifiableList.create(new Attribute<?>[] {
        DrawAttr.STROKE_WIDTH, ATTR_LABEL, StdAttr.LABEL_FONT, StdAttr.LABEL_COLOR });
  }

  @Override
  public void paintDynamic(Graphics g, CircuitState state) {
    Color offColor = path.leaf().getAttributeSet().getValue(Io.ATTR_OFF_COLOR);
    Color onColor = path.leaf().getAttributeSet().getValue(Io.ATTR_ON_COLOR);
    int x = bounds.getX();
    int y = bounds.getY();
    int w = bounds.getWidth();
    int h = bounds.getHeight();
    int width = path.leaf().getAttributeSet().getValue(LedVector.ATTR_ARRAY_WIDTH);
    GraphicsUtil.switchToWidth(g, strokeWidth);
    if (state == null) {
      g.setColor(offColor);
      for (int i = 0; i < width; i++) {
        g.fillRect(x + DEFAULT_LENGTH * i, y, DEFAULT_LENGTH, h);
      }
      g.setColor(DynamicElement.COLOR);
      for (int i = 0; i < width; i++) {
        g.drawRect(x + DEFAULT_LENGTH * i, y, DEFAULT_LENGTH, h);
      }
      g.drawRect(x, y, w, h);
    } else {
      InstanceDataSingleton data = (InstanceDataSingleton) getData(state);
      int summ = data == null ? 0 : (Integer) data.getValue();
      for (int i = 0; i < width; i++) {
        g.setColor((summ >> i & 1) == 1 ? onColor : offColor);
        g.fillRect(x + DEFAULT_LENGTH * i, y, DEFAULT_LENGTH, h);
      }
      g.setColor(Color.darkGray);
      for (int i = 0; i < width; i++) {
        g.drawRect(x + DEFAULT_LENGTH * i, y, DEFAULT_LENGTH, h);
      }
      g.drawRect(x, y, w, h);
    }
    drawLabel(g);
  }

  @Override
  public Element toSvgElement(Document doc) {
    return toSvgElement(doc.createElement("visible-ledvector"));
  }

  @Override
  public String getDisplayName() {
    return S.get("ledVectorComponent");
  }

  @Override
  public String toString() {
    return "LedVector:" + getBounds();
  }
}
