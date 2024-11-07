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

import com.bfh.logisim.hdlgenerator.HDLInliner;
import com.bfh.logisim.hdlgenerator.HiddenPort;
import com.bfh.logisim.netlist.Net;
import com.bfh.logisim.netlist.NetlistComponent;
import com.cburch.logisim.hdl.Hdl;

public class LightsHDLGenerator extends HDLInliner {

  public LightsHDLGenerator(ComponentContext ctx) {
    super(ctx);
  }

  public static LightsHDLGenerator forLed(ComponentContext ctx) {
    LightsHDLGenerator g = new LightsHDLGenerator(ctx);
    g.hiddenPort = HiddenPort.makeOutport(1, HiddenPort.LED, HiddenPort.Pin);
    return g;
  }

  public static LightsHDLGenerator forLedVector(ComponentContext ctx) {
    LightsHDLGenerator g = new LightsHDLGenerator(ctx);
    g.hiddenPort = HiddenPort.makeOutport(ctx.attrs.getValue(LedVector.ATTR_ARRAY_WIDTH), HiddenPort.LEDVector, HiddenPort.LED, HiddenPort.Ribbon, HiddenPort.Pin);
    return g;
  }

  public static LightsHDLGenerator forRGBLed(ComponentContext ctx) {
    LightsHDLGenerator g = new LightsHDLGenerator(ctx);
    g.hiddenPort = HiddenPort.makeOutport(RGBLed.pinLabels(), HiddenPort.RGBLED, HiddenPort.LED, HiddenPort.Ribbon, HiddenPort.Pin);
    return g;
  }

  public static LightsHDLGenerator forSevenSegment(ComponentContext ctx) {
    LightsHDLGenerator g = new LightsHDLGenerator(ctx);
    g.hiddenPort = HiddenPort.makeOutport(SevenSegment.pinLabels(),
        HiddenPort.SevenSegment, HiddenPort.LED, HiddenPort.Pin);
    return g;
  }

  @Override
	protected void generateInlinedCode(Hdl out, NetlistComponent comp) {
    int b = comp.getLocalHiddenPortIndices().start.out;
    for (int i = 0; i < comp.portConnections.size(); i++) {
      Net net = comp.getConnection(i);
      if (net != null)
        out.assign("LOGISIM_HIDDEN_FPGA_OUTPUT", b + i, net.name);
    }
  }

}
