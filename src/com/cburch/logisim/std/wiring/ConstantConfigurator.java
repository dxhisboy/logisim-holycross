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

package com.cburch.logisim.std.wiring;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.NumericConfigurator;

class ConstantConfigurator extends NumericConfigurator<Constant.ValueWithRadix> {
  public ConstantConfigurator() {
    super(Constant.ATTR_VALUE, 0, 0, 0, 16);
  }

  @Override
  public int getMaximumValue(AttributeSet attrs) {
    BitWidth width = attrs.getValue(StdAttr.WIDTH);
    int ret = width.getMask();
    if (ret >= 0) {
      return ret;
    } else {
      return Integer.MAX_VALUE;
    }
  }

  @Override
  public int getMinimumValue(AttributeSet attrs) {
    BitWidth width = attrs.getValue(StdAttr.WIDTH);
    if (width.getWidth() < 32) {
      return 0;
    } else {
      return Integer.MIN_VALUE;
    }
  }

  @Override
  protected Constant.ValueWithRadix createValue(int val) {
    return Constant.ValueWithRadix.fromValue(Value.createKnown(BitWidth.of(32), val));
  }
}
