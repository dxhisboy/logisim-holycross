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

import java.awt.Graphics2D;

import com.bfh.logisim.hdlgenerator.HDLSupport;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
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
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.RotationConfigurator;
import com.cburch.logisim.util.GraphicsUtil;

public class Power extends InstanceFactory {

  public Power() {
    super("Power", S.getter("powerComponent"));
    setIconName("power.gif");
    setAttributes(new Attribute[] { StdAttr.FACING, StdAttr.WIDTH },
        new Object[] { Direction.NORTH, BitWidth.ONE });
    setFacingAttribute(StdAttr.FACING);
    setKeyConfigurators(
      new BitWidthConfigurator(StdAttr.WIDTH),
      new RotationConfigurator(StdAttr.FACING)
    );
    setPorts(new Port[] { new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH) });
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
  }

  private void drawInstance(InstancePainter painter, boolean isGhost) {
    Graphics2D g = (Graphics2D) painter.getGraphics().create();
    Location loc = painter.getLocation();
    g.translate(loc.getX(), loc.getY());

    Direction from = painter.getAttributeValue(StdAttr.FACING);
    int degrees = Direction.EAST.toDegrees() - from.toDegrees();
    double radians = Math.toRadians((degrees + 360) % 360);
    g.rotate(radians);

    GraphicsUtil.switchToWidth(g, Wire.WIDTH);
    if (!isGhost && painter.getShowState()) {
      g.setColor(painter.getPortValue(0).getColor());
    }
    g.drawLine(0, 0, 5, 0);

    GraphicsUtil.switchToWidth(g, 1);
    if (!isGhost && painter.shouldDrawColor()) {
      BitWidth width = painter.getAttributeValue(StdAttr.WIDTH);
      g.setColor(Value.repeat(Value.TRUE, width.getWidth()).getColor());
    }
    g.drawPolygon(new int[] { 6, 14, 6 }, new int[] { -8, 0, 8 }, 3);

    g.dispose();
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    return Bounds.create(0, -8, 15, 16).rotate(Direction.EAST,
        attrs.getValue(StdAttr.FACING), 0, 0);
  }

  @Override
  public HDLSupport getHDLSupport(HDLSupport.ComponentContext ctx) {
    return new ConstantHDLGenerator(ctx, -1); // vector of ones
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
    }
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    drawInstance(painter, true);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    drawInstance(painter, false);
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
    state.setPort(0, Value.repeat(Value.TRUE, width.getWidth()), 1);
  }

}
