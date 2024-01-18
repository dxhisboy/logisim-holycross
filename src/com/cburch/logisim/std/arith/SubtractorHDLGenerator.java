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
package com.cburch.logisim.std.arith;

import com.bfh.logisim.hdlgenerator.HDLGenerator;
import com.cburch.logisim.hdl.Hdl;

public class SubtractorHDLGenerator extends HDLGenerator {

  public SubtractorHDLGenerator(ComponentContext ctx) {
    super(ctx, "arithmetic", "${BUS}Subtractor", "i_Sub");
    int w = stdWidth();
    if (w > 1) {
      // Generic n-bit version
      parameters.add("BitWidth", w);
      inPorts.add("DataA", "BitWidth", Subtractor.IN0, false);
      inPorts.add("DataB", "BitWidth", Subtractor.IN1, false);
      outPorts.add("Result", "BitWidth", Subtractor.OUT, null);
      inPorts.add("BorrowIn", 1, Subtractor.B_IN, false);
      outPorts.add("BorrowOut", 1, Subtractor.B_OUT, null);
      if (_hdl.isVhdl) {
        wires.add("s_A", "BitWidth+1");
        wires.add("s_B", "BitWidth+1");
        wires.add("s_C", 1);
        wires.add("s_R", "BitWidth+1");
      } else {
        wires.add("s_C", 1);
      }
    } else {
      // 1-bit version
      inPorts.add("DataA", 1, Subtractor.IN0, false);
      inPorts.add("DataB", 1, Subtractor.IN1, false);
      outPorts.add("Result", 1, Subtractor.OUT, null);
      inPorts.add("BorrowIn", 1, Subtractor.B_IN, false);
      outPorts.add("BorrowOut", 1, Subtractor.B_OUT, null);
      if (_hdl.isVhdl) {
        wires.add("s_A", 2);
        wires.add("s_B", 2);
        wires.add("s_C", 1);
        wires.add("s_R", 2);
      } else {
        wires.add("s_C", 1);
      }
    }
  }

  @Override
  protected void generateBehavior(Hdl out) {
    if (out.isVhdl) {
      out.stmt("s_A <= \"0\" & DataA;");
      out.stmt("s_B <= \"0\" & not(DataB);");
      out.stmt("s_C <= not(BorrowIn);");
      out.stmt("s_R <= std_logic_vector(unsigned(s_A) + unsigned(s_B)+ (\"\" & s_C));");
      if (isBus()) {
        out.stmt("Result <= s_R(BitWidth-1 downto 0);");
        out.stmt("BorrowOut <= not(s_R(BitWidth));");
      } else {
        out.stmt("Result <= s_R(0);");
        out.stmt("BorrowOut <= not(s_R(1));");
      }
    } else {
      out.stmt("assign {s_C, Result} = DataA + ~(DataB) + ~(BorrowIn);");
      out.stmt("assign BorrowOut = ~s_C;");
    }
  }

}
