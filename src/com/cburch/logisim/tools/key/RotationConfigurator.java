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

package com.cburch.logisim.tools.key;

import java.awt.event.KeyEvent;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Direction;

public class RotationConfigurator implements KeyConfigurator, Cloneable {
  private Attribute<Direction> attr;
  private int key;

  public RotationConfigurator(Attribute<? extends Object> attr, int key) {
    this.attr = (Attribute<Direction>)attr;
    this.key = key;
  }

  public RotationConfigurator(Attribute<? extends Object> attr) {
    this(attr, KeyEvent.VK_SPACE);
  }

  @Override
  public RotationConfigurator clone() {
    try {
      return (RotationConfigurator) super.clone();
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
      return null;
    }
  }

  public KeyConfigurationResult keyEventReceived(KeyConfigurationEvent event) {
    if (event.getType() == KeyConfigurationEvent.KEY_PRESSED) {
      KeyEvent e = event.getKeyEvent();
      // if (e.getModifiersEx() == modsEx) {
        Direction value = null;
        if (e.getKeyCode() == key) {
          value = event.getAttributeSet().getValue(attr).getLeft();
          event.consume();
          return new KeyConfigurationResult(event, attr, value);
        // case KeyEvent.VK_UP:
        //   value = Direction.NORTH;
        //   break;
        // case KeyEvent.VK_DOWN:
        //   value = Direction.SOUTH;
        //   break;
        // case KeyEvent.VK_LEFT:
        //   value = Direction.WEST;
        //   break;
        // case KeyEvent.VK_RIGHT:
        //   value = Direction.EAST;
        //   break;
        }
        // if (value != null) {
        //   event.consume();
        //   return new KeyConfigurationResult(event, attr, value);
        // }
      // }
    }
    return null;
  }
}
