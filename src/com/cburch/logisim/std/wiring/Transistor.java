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

/**
 * Based on PUCTools (v0.9 beta) by CRC - PUC - Minas (pucmg.crc at gmail.com)
 */

package com.cburch.logisim.std.wiring;
import static com.cburch.logisim.std.Strings.S;

import java.awt.Color;
import java.awt.Graphics2D;

import javax.swing.Icon;

import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.WireRepair;
import com.cburch.logisim.tools.WireRepairData;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.RotationConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;

public class Transistor extends InstanceFactory {
  static final AttributeOption TYPE_P = new AttributeOption("p",
      S.getter("transistorTypeP"));
  static final AttributeOption TYPE_N = new AttributeOption("n",
      S.getter("transistorTypeN"));
  static final Attribute<AttributeOption> ATTR_TYPE = Attributes.forOption(
      "type", S.getter("transistorTypeAttr"),
      new AttributeOption[] { TYPE_P, TYPE_N });

  static final int OUTPUT = 0;
  static final int INPUT = 1;
  static final int GATE = 2;

  private static final Icon ICON_N = Icons.getIcon("trans1.gif");
  private static final Icon ICON_P = Icons.getIcon("trans0.gif");

  public Transistor() {
    super("Transistor", S.getter("transistorComponent"));
    setAttributes(new Attribute[] { ATTR_TYPE, StdAttr.FACING,
      Analog.ATTR_GATE, StdAttr.WIDTH }, new Object[] { TYPE_P,
        Direction.EAST, Analog.GATE_TOP_LEFT, BitWidth.ONE });
    setFacingAttribute(StdAttr.FACING);
    setKeyConfigurators(
      new BitWidthConfigurator(StdAttr.WIDTH),
      new RotationConfigurator(StdAttr.FACING)
    );
  }

  private Value computeOutput(InstanceState state) {
    BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
    Value gate = state.getPortValue(GATE);
    Value input = state.getPortValue(INPUT);
    Object type = state.getAttributeValue(ATTR_TYPE);
    Value desired = type == TYPE_P ? Value.FALSE : Value.TRUE;
    Value masked = type == TYPE_P ? Value.FALSE : Value.TRUE;

    if (!gate.isFullyDefined()) {
      if (input.isFullyDefined()) {
        return Value.createError(width);
      } else {
        Value[] v = input.getAll();
        for (int i = 0; i < v.length; i++) {
          if (v[i] != Value.UNKNOWN) {
            v[i] = Value.ERROR;
          }
        }
        return Value.create(v);
      }
    } else if (gate != desired) {
      return Value.createUnknown(width);
    } else {
      // masked inputs become Z outputs
      // all other inputs pass through to output
      Value[] v = input.getAll();
      for (int i = 0; i < v.length; i++) {
        if (v[i] == masked)
          v[i] = Value.UNKNOWN;
      }
      return Value.create(v);
    }
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
  }

  @Override
  public boolean nominallyContains(Location loc, AttributeSet attrs) {
    if (super.nominallyContains(loc, attrs)) {
      Direction facing = attrs.getValue(StdAttr.FACING);
      Location center = Location.create(0, 0).translate(facing, -20);
      return center.manhattanDistanceTo(loc) < 24;
    } else {
      return false;
    }
  }

  private void drawInstance(InstancePainter painter, boolean isGhost) {
    Object type = painter.getAttributeValue(ATTR_TYPE);
    Object powerLoc = painter.getAttributeValue(Analog.ATTR_GATE);
    Direction from = painter.getAttributeValue(StdAttr.FACING);
    Direction facing = painter.getAttributeValue(StdAttr.FACING);
    boolean flip = (facing == Direction.SOUTH || facing == Direction.WEST) == (powerLoc == Analog.GATE_TOP_LEFT);

    int degrees = Direction.EAST.toDegrees() - from.toDegrees();
    double radians = Math.toRadians((degrees + 360) % 360);
    int m = flip ? 1 : -1;

    Graphics2D g = (Graphics2D) painter.getGraphics();
    Location loc = painter.getLocation();
    g.translate(loc.getX(), loc.getY());
    g.rotate(radians);

    Color gate;
    Color input;
    Color output;
    Color platform;
    if (!isGhost && painter.getShowState()) {
      gate = painter.getPortValue(GATE).getColor();
      input = painter.getPortValue(INPUT).getColor();
      output = painter.getPortValue(OUTPUT).getColor();
      Value out = computeOutput(painter);
      platform = out.isUnknown() ? Value.UNKNOWN.getColor() : out
          .getColor();
    } else {
      Color base = g.getColor();
      gate = base;
      input = base;
      output = base;
      platform = base;
    }

    // input and output lines
    GraphicsUtil.switchToWidth(g, Wire.WIDTH);
    g.setColor(output);
    g.drawLine(0, 0, -13, 0);
    g.drawLine(-13, m * 6, -13, 0);

    g.setColor(input);
    g.drawLine(-40, 0, -27, 0);
    g.drawLine(-27, m * 6, -27, 0);

    // gate line
    g.setColor(gate);
    if (type == TYPE_P) {
      g.drawLine(-20, m * 20, -20, m * 18);
      GraphicsUtil.switchToWidth(g, 2);
      g.drawOval(-20 - 3, m * 15 - 3, 6, 6);
    } else {
      g.drawLine(-20, m * 20, -20, m * 13);
      GraphicsUtil.switchToWidth(g, 2);
    }

    // draw platforms
    g.drawLine(-12, m * 12, -28, m * 12); // gate platform
    g.setColor(platform);
    g.drawLine(-9, m * 7, -31, m * 7); // input/output platform

    // arrow (same color as platform)
    GraphicsUtil.switchToWidth(g, 1);
    g.drawLine(-21, m * 4, -19, m * 2);
    g.drawLine(-21, 0, -19, m * 2);

    g.rotate(-radians);
    g.translate(-loc.getX(), -loc.getY());
  }

  @Override
  public Object getInstanceFeature(final Instance instance, Object key) {
    if (key == WireRepair.class) {
      return new WireRepair() {
        public boolean shouldRepairWire(WireRepairData data) {
          return true;
        }
      };
    }
    return super.getInstanceFeature(instance, key);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Direction facing = attrs.getValue(StdAttr.FACING);
    Object gateLoc = attrs.getValue(Analog.ATTR_GATE);
    int delta = gateLoc == Analog.GATE_TOP_LEFT ? -20 : 0;
    if (facing == Direction.NORTH) {
      return Bounds.create(delta, 0, 20, 40);
    } else if (facing == Direction.SOUTH) {
      return Bounds.create(delta, -40, 20, 40);
    } else if (facing == Direction.WEST) {
      return Bounds.create(0, delta, 40, 20);
    } else { // facing == Direction.EAST
      return Bounds.create(-40, delta, 40, 20);
    }
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING || attr == Analog.ATTR_GATE) {
      instance.recomputeBounds();
      updatePorts(instance);
    } else if (attr == StdAttr.WIDTH || attr == ATTR_TYPE) {
      updatePorts(instance);
    } else if (attr == ATTR_TYPE) {
      instance.fireInvalidated();
    }
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    drawInstance(painter, true);
  }

  @Override
  public void paintIcon(InstancePainter painter) {
    Object type = painter.getAttributeValue(ATTR_TYPE);
    Icon icon = type == TYPE_N ? ICON_N : ICON_P;
    icon.paintIcon(painter.getDestination(), painter.getGraphics(), 2, 2);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    drawInstance(painter, false);
    // painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    state.setPort(OUTPUT, computeOutput(state), 1);
  }

  private void updatePorts(Instance instance) {
    Direction facing = instance.getAttributeValue(StdAttr.FACING);
    int dx = 0;
    int dy = 0;
    if (facing == Direction.NORTH) {
      dy = 1;
    } else if (facing == Direction.EAST) {
      dx = -1;
    } else if (facing == Direction.SOUTH) {
      dy = -1;
    } else if (facing == Direction.WEST) {
      dx = 1;
    }

    Object powerLoc = instance.getAttributeValue(Analog.ATTR_GATE);
    boolean flip = (facing == Direction.SOUTH || facing == Direction.WEST) == (powerLoc == Analog.GATE_TOP_LEFT);

    Port[] ports = new Port[3];
    ports[OUTPUT] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
    ports[INPUT] = new Port(40 * dx, 40 * dy, Port.INPUT, StdAttr.WIDTH);
    if (flip) {
      ports[GATE] = new Port(20 * (dx + dy), 20 * (-dx + dy), Port.INPUT, 1);
    } else {
      ports[GATE] = new Port(20 * (dx - dy), 20 * (dx + dy), Port.INPUT,
          1);
    }
    if (instance.getAttributeValue(ATTR_TYPE) == TYPE_P) {
      ports[GATE].setToolTip(S.getter("transistorPGate"));
      ports[INPUT].setToolTip(S.getter("transistorPSource"));
      ports[OUTPUT].setToolTip(S.getter("transistorPDrain"));
    } else {
      ports[GATE].setToolTip(S.getter("transistorNGate"));
      ports[INPUT].setToolTip(S.getter("transistorNSource"));
      ports[OUTPUT].setToolTip(S.getter("transistorNDrain"));
    }
    instance.setPorts(ports);
  }
}
