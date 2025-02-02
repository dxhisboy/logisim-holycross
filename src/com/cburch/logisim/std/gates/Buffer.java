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

package com.cburch.logisim.std.gates;

import static com.cburch.logisim.std.Strings.S;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import com.bfh.logisim.hdlgenerator.HDLSupport;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.circuit.ExpressionComputer;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.RotationConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;

class Buffer extends InstanceFactory {

  static Value repair(InstanceState state, Value v) {
    AttributeSet opts = state.getProject().getOptions().getAttributeSet();
    Object onUndefined = opts.getValue(Options.ATTR_GATE_UNDEFINED);
    boolean errorIfUndefined = onUndefined
        .equals(Options.GATE_UNDEFINED_ERROR);
    Value repaired;
    if (errorIfUndefined) {
      int vw = v.getWidth();
      BitWidth w = state.getAttributeValue(StdAttr.WIDTH);
      int ww = w.getWidth();
      if (vw == ww && v.isFullyDefined())
        return v;
      Value[] vs = new Value[w.getWidth()];
      for (int i = 0; i < vs.length; i++) {
        Value ini = i < vw ? v.get(i) : Value.ERROR;
        vs[i] = ini.isFullyDefined() ? ini : Value.ERROR;
      }
      repaired = Value.create(vs);
    } else {
      repaired = v;
    }

    Object outType = state.getAttributeValue(GateAttributes.ATTR_OUTPUT);
    return AbstractGate.pullOutput(repaired, outType);
  }

  public static InstanceFactory FACTORY = new Buffer();

  private Buffer() {
    super("Buffer", S.getter("bufferComponent"));
    setAttributes(
        new Attribute[] { StdAttr.FACING, StdAttr.WIDTH,
            GateAttributes.ATTR_OUTPUT, StdAttr.LABEL,
            StdAttr.LABEL_FONT },
        new Object[] { Direction.EAST,
            BitWidth.ONE, GateAttributes.OUTPUT_01, "",
            StdAttr.DEFAULT_LABEL_FONT });
    setIcon(Icons.getIcon("bufferGate.gif"));
    setFacingAttribute(StdAttr.FACING);
    setKeyConfigurators(
        new BitWidthConfigurator(StdAttr.WIDTH),
        new RotationConfigurator(StdAttr.FACING));
    setPorts(new Port[] { new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH),
        new Port(0, -20, Port.INPUT, StdAttr.WIDTH), });
  }

  //
  // methods for instances
  //
  @Override
  protected void configureNewInstance(Instance instance) {
    configurePorts(instance);
    instance.addAttributeListener();
    NotGate.configureLabel(instance, false, null);
  }

  private void configurePorts(Instance instance) {
    Direction facing = instance.getAttributeValue(StdAttr.FACING);

    Port[] ports = new Port[2];
    ports[0] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
    Location out = Location.create(0, 0).translate(facing, -20);
    ports[1] = new Port(out.getX(), out.getY(), Port.INPUT, StdAttr.WIDTH);
    instance.setPorts(ports);
  }

  @Override
  public Object getInstanceFeature(final Instance instance, Object key) {
    if (key == ExpressionComputer.class) {
      return new ExpressionComputer() {
        public void computeExpression(ExpressionComputer.Map expressionMap) {
          int width = instance.getAttributeValue(StdAttr.WIDTH).getWidth();
          for (int b = 0; b < width; b++) {
            Expression e = expressionMap.get(instance
                .getPortLocation(1), b);
            if (e != null) {
              expressionMap.put(instance.getPortLocation(0), b, e);
            }
          }
        }
      };
    }
    return super.getInstanceFeature(instance, key);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Direction facing = attrs.getValue(StdAttr.FACING);
    if (facing == Direction.SOUTH)
      return Bounds.create(-9, -20, 18, 20);
    if (facing == Direction.NORTH)
      return Bounds.create(-9, 0, 18, 20);
    if (facing == Direction.WEST)
      return Bounds.create(0, -9, 20, 18);
    return Bounds.create(-20, -9, 20, 18);
  }

  @Override
  public boolean HasThreeStateDrivers(AttributeSet attrs) {
    if (attrs.containsAttribute(GateAttributes.ATTR_OUTPUT))
      return !(attrs.getValue(GateAttributes.ATTR_OUTPUT) == GateAttributes.OUTPUT_01);
    else
      return false;
  }

  @Override
  public HDLSupport getHDLSupport(HDLSupport.ComponentContext ctx) {
    if (ctx.lang.equals("VHDL"))
      return GateVhdlGenerator.forBuffer(ctx);
    else
      return GateVerilogGenerator.forBuffer(ctx);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
      configurePorts(instance);
      NotGate.configureLabel(instance, false, null);
    }
  }

  private void paintBase(InstancePainter painter) {
    Direction facing = painter.getAttributeValue(StdAttr.FACING);
    Location loc = painter.getLocation();
    int x = loc.getX();
    int y = loc.getY();
    Graphics g = painter.getGraphics();
    g.translate(x, y);
    double rotate = 0.0;
    if (facing != Direction.EAST) {
      rotate = -facing.toRadians();
      ((Graphics2D) g).rotate(rotate);
    }

    GraphicsUtil.switchToWidth(g, 2);
    int[] xp = new int[4];
    int[] yp = new int[4];
    xp[0] = 0;
    yp[0] = 0;
    xp[1] = -19;
    yp[1] = -7;
    xp[2] = -19;
    yp[2] = 7;
    xp[3] = 0;
    yp[3] = 0;
    g.drawPolyline(xp, yp, 4);

    if (rotate != 0.0) {
      ((Graphics2D) g).rotate(-rotate);
    }
    g.translate(-x, -y);
  }

  //
  // painting methods
  //
  @Override
  public void paintGhost(InstancePainter painter) {
    paintBase(painter);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Graphics g = painter.getGraphics();
    g.setColor(Color.BLACK);
    paintBase(painter);
    painter.drawPorts();
    painter.drawLabel();
  }

  @Override
  public void propagate(InstanceState state) {
    Value in = state.getPortValue(1);
    in = Buffer.repair(state, in);
    state.setPort(0, in, GateAttributes.DELAY);
  }
}
