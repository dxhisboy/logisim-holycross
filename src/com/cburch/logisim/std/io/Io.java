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
import java.util.ArrayList;
import java.util.List;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class Io extends Library {
  static final Attribute<Color> ATTR_COLOR = Attributes.forColor("color",
      S.getter("ioColorAttr"));
  static final Attribute<Color> ATTR_ON_COLOR = Attributes.forColor("color",
      S.getter("ioOnColor"));
  static final Attribute<Color> ATTR_OFF_COLOR = Attributes.forColor(
      "offcolor", S.getter("ioOffColor"));
  static final Attribute<Color> ATTR_BACKGROUND = Attributes.forColor("bg",
      S.getter("ioBackgroundColor"));
  static final Attribute<Boolean> ATTR_ACTIVE = Attributes.forBoolean(
      "active", S.getter("ioActiveAttr"));

  static final Color DEFAULT_BACKGROUND = new Color(255, 255, 255, 0);

  private static FactoryDescription[] DESCRIPTIONS = {
    new FactoryDescription("Button", S.getter("buttonComponent"),
        "button.gif", "Button"),
    new FactoryDescription("DipSwitch",
        S.getter("dipswitchComponent"), "dipswitch.gif",
        "DipSwitch"),
    new FactoryDescription("Joystick",
        S.getter("joystickComponent"), "joystick.gif",
        "Joystick"),
    new FactoryDescription("Keyboard",
        S.getter("keyboardComponent"), "keyboard.gif",
        "Keyboard"),
    new FactoryDescription("LED", S.getter("ledComponent"),
        "led.gif", "Led"),
    new FactoryDescription("PortIO", S.getter("pioComponent"),
        "pio.gif", "PortIO"),
    new FactoryDescription("RGBLED", S.getter("RGBledComponent"),
        "rgbled.gif", "RGBLed"),
    new FactoryDescription("7-Segment Display",
        S.getter("sevenSegmentComponent"), "7seg.gif",
        "SevenSegment"),
    new FactoryDescription("Hex Digit Display",
        S.getter("hexDigitComponent"), "hexdig.gif",
        "HexDigit"),
    new FactoryDescription("DotMatrix",
        S.getter("dotMatrixComponent"), "dotmat.gif",
        "DotMatrix"),
    new FactoryDescription("LedVector",
        S.getter("ledVectorComponent"), "ledvector.gif",
        "LedVector"),
    // new FactoryDescription("7-Segment Array",
        // S.getter("SevenSegmentArrayComponent"), "7seg.gif",
        // "SevenSegmentArray"),
    new FactoryDescription("TTY", S.getter("ttyComponent"),
        "tty.gif", "Tty"),
    new FactoryDescription("FileViewer", S.getter("fileViewerComponent"),
        "fileviewer.gif", "FileViewer"),
    new FactoryDescription("Slideshow", S.getter("slideShowComponent"),
        "slideshow.gif", "Slideshow"),
  };

  private List<Tool> tools = null;

  public Io() {
  }

  @Override
  public String getDisplayName() {
    return S.get("ioLibrary");
  }

  @Override
  public String getName() {
    return "I/O";
  }

  @Override
  public List<Tool> getTools() {
    if (tools == null) {
      tools = new ArrayList<>();
      tools.addAll(FactoryDescription.getTools(Io.class, DESCRIPTIONS));
      tools.add(new AddTool(Io.class, Video.factory));
    }
    return tools;
  }

}
