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

import java.awt.Color;
import java.awt.Graphics;

import com.bfh.logisim.hdlgenerator.HDLSupport;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.circuit.appear.DynamicElementProvider;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.std.wiring.DurationAttribute;
import com.cburch.logisim.util.GraphicsUtil;

public class LedVector extends InstanceFactory implements DynamicElementProvider {
  static class State implements InstanceData, Cloneable {
    private int width;
    private Value values[];
    public State(int width) {
      this.width = width;
      values = new Value[width];
    }
    public Value getValue(int i){
      return values[i];
    }
    public void setValue(int i, Value value){
      values[i] = value;
    }
    public void setWidth(int width){
      this.width = width;
      values = new Value[width];
    }
    @Override
    public Object clone() {
      try {
        State ret = (State) super.clone();
        return ret;
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }
  }

  static final AttributeOption SHAPE_CIRCLE = new AttributeOption("circle",
      S.getter("ioShapeCircle"));

  static final AttributeOption SHAPE_SQUARE = new AttributeOption("square",
      S.getter("ioShapeSquare"));

  static final Attribute<Integer> ATTR_ARRAY_WIDTH = Attributes
      .forIntegerRange("arraywidth", S.getter("ioArrayWidth"), 1,
          Value.MAX_WIDTH);

  static final Attribute<AttributeOption> ATTR_DOT_SHAPE = Attributes
      .forOption("dotshape", S.getter("ioMatrixShape"),
          new AttributeOption[] { SHAPE_CIRCLE, SHAPE_SQUARE });

  static final Attribute<Integer> ATTR_PERSIST = new DurationAttribute(
      "persist", S.getter("ioMatrixPersistenceAttr"), 0,
      Integer.MAX_VALUE);

  public LedVector() {
    super("LedVector", S.getter("LedVectorComponent"));
    setAttributes(new Attribute<?>[] {
      ATTR_ARRAY_WIDTH, Io.ATTR_ON_COLOR, Io.ATTR_OFF_COLOR,
      ATTR_PERSIST, ATTR_DOT_SHAPE }, new Object[] {
        Integer.valueOf(8), Color.GREEN,
        Color.DARK_GRAY, Integer.valueOf(0), SHAPE_SQUARE });
    setIconName("dotmat.gif");
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    int width = attrs.getValue(ATTR_ARRAY_WIDTH).intValue();
    return Bounds.create(-5, -10, 10 * width, 10);
  }

  // private State getState(InstanceState state) {
  //   int width = state.getAttributeValue(ATTR_ARRAY_WIDTH).intValue();
  //   State data = (State) state.getData();
  //   if (data == null) {
  //     data = new State(width);
  //     state.setData(data);
  //   } else {
  //     data.width = width;
  //   }
  //   return data;
  // }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == ATTR_ARRAY_WIDTH) {
      instance.recomputeBounds();
      updatePorts(instance);
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Color onColor = painter.getAttributeValue(Io.ATTR_ON_COLOR);
    Color offColor = painter.getAttributeValue(Io.ATTR_OFF_COLOR);
    boolean drawSquare = painter.getAttributeValue(ATTR_DOT_SHAPE) == SHAPE_SQUARE;

    // State data = getState(painter);
    Bounds bds = painter.getBounds();
    boolean showState = painter.getShowState();
    Graphics g = painter.getGraphics();
    int rows = 1;
    int cols = painter.getAttributeValue(ATTR_ARRAY_WIDTH);;
    for (int j = 0; j < rows; j++) {
      for (int i = 0; i < cols; i++) {
        int x = bds.getX() + 10 * i;
        int y = bds.getY() + 10 * j;
        if (showState) {
          Value val = painter.getPortValue(i);
          Color c;
          if (val == Value.TRUE)
            c = onColor;
          else if (val == Value.FALSE)
            c = offColor;
          else
            c = Value.ERROR_COLOR;
          g.setColor(c);

          if (drawSquare)
            g.fillRect(x, y, 10, 10);
          else
            g.fillOval(x + 1, y + 1, 8, 8);
        } else {
          g.setColor(Color.GRAY);
          g.fillOval(x + 1, y + 1, 8, 8);
        }
      }
    }
    g.setColor(Color.BLACK);
    GraphicsUtil.switchToWidth(g, 2);
    g.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    GraphicsUtil.switchToWidth(g, 1);
    painter.drawPorts();
  }

  @Override
  public HDLSupport getHDLSupport(HDLSupport.ComponentContext ctx) {
    System.out.println("get HDL support for LedVec");
    return LightsHDLGenerator.forLedVector(ctx);
  }

  @Override
  public String getHDLNamePrefix(Component comp) { return "LED"; }

  @Override
  public void propagate(InstanceState state) {
    int width = state.getInstance().getAttributeValue(ATTR_ARRAY_WIDTH).intValue();
    InstanceDataSingleton data = (InstanceDataSingleton)state.getData();
    int summ = 0;
    for (int i = 0; i < width; i ++) {
      if (state.getPortValue(i).toIntValue() != 0)
        summ |= 1 << i;
    }
    if (data == null) {
      state.setData(new InstanceDataSingleton(summ));
    } else {
      data.setValue(summ);
    }

  }

  private void updatePorts(Instance instance) {
    int cols = instance.getAttributeValue(ATTR_ARRAY_WIDTH).intValue();
    Port[] ps;
    ps = new Port[cols];
    for (int i = 0; i < cols; i++) {
      ps[i] = new Port(10 * i, 0, Port.INPUT, 1);
    }
    instance.setPorts(ps);
  }

  public DynamicElement createDynamicElement(int x, int y, DynamicElement.Path path) {
    return new LedVectorShape(x, y, path);
  }
}
