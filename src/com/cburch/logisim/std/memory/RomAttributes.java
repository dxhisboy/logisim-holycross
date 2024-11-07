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
package com.cburch.logisim.std.memory;

import java.awt.Font;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;

class RomAttributes extends AbstractAttributeSet {

  static HexFrame getHexFrame(MemContents value, Project proj, Instance instance) {
    synchronized (windowRegistry) {
      HexFrame ret = windowRegistry.get(value);
      if (ret == null) {
        ret = new HexFrame(proj, instance, value);
        windowRegistry.put(value, ret);
      }
      return ret;
    }
  }

  static void closeHexFrame(MemContents value) {
    HexFrame ret;
    synchronized (windowRegistry) {
       ret = windowRegistry.remove(value);
    }
    if (ret != null) {
      ret.closeAndDispose();
    }
  }

  static void register(MemContents value, Project proj) {
    if (proj == null || listenerRegistry.containsKey(value)) {
      return;
    }
    RomContentsListener l = new RomContentsListener(proj);
    value.addHexModelWeakListener(value, l);
    listenerRegistry.put(value, l);
  }

  private static List<Attribute<?>> ATTRIBUTES = Arrays
      .asList(new Attribute<?>[] { Mem.ADDR_ATTR, Mem.DATA_ATTR, Mem.LINE_ATTR,
        Rom.CONTENTS_ATTR, StdAttr.LABEL, StdAttr.LABEL_FONT,
        StdAttr.APPEARANCE});

  // fixme: this isn't necessary since EventSourceWeakSupport now has an owner
  // for each listener?
  private static WeakHashMap<MemContents, RomContentsListener> listenerRegistry = new WeakHashMap<MemContents, RomContentsListener>();

  private static WeakHashMap<MemContents, HexFrame> windowRegistry = new WeakHashMap<MemContents, HexFrame>();
  private BitWidth addrBits = BitWidth.create(8);
  private BitWidth dataBits = BitWidth.create(8);
  private MemContents contents;
  private AttributeOption lineSize = Mem.SINGLE;
  private String Label = "";
  private Font LabelFont = StdAttr.DEFAULT_LABEL_FONT;
  private AttributeOption Appearance = StdAttr.APPEAR_CLASSIC;

  RomAttributes() {
    contents = MemContents.create(addrBits.getWidth(), dataBits.getWidth());
  }

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    RomAttributes d = (RomAttributes) dest;
    d.addrBits = addrBits;
    d.dataBits = dataBits;
    d.contents = contents.clone();
    d.lineSize = lineSize;
    d.LabelFont = LabelFont;
    d.Appearance = Appearance;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return ATTRIBUTES;
  }

  @Override
  public <V> V getValue(Attribute<V> attr) {
    if (attr == Mem.ADDR_ATTR)
      return (V) addrBits;
    if (attr == Mem.DATA_ATTR)
      return (V) dataBits;
    if (attr == Rom.CONTENTS_ATTR)
      return (V) contents;
    if (attr == Mem.LINE_ATTR)
      return (V) lineSize;
    if (attr == StdAttr.LABEL)
      return (V) Label;
    if (attr == StdAttr.LABEL_FONT)
      return (V) LabelFont;
    if (attr == StdAttr.APPEARANCE)
      return (V) Appearance;
    return null;
  }

  void setProject(Project proj) {
    register(contents, proj);
  }

  @Override
  public <V> void updateAttr(Attribute<V> attr, V value) {
    if (attr == Mem.ADDR_ATTR) {
      addrBits = (BitWidth) value;
      contents.setDimensions(addrBits.getWidth(), dataBits.getWidth());
    } else if (attr == Mem.DATA_ATTR) {
      dataBits = (BitWidth) value;
      contents.setDimensions(addrBits.getWidth(), dataBits.getWidth());
    }
    else if (attr == Mem.LINE_ATTR)
      lineSize = (AttributeOption) value;
    else if (attr == Rom.CONTENTS_ATTR)
      contents = (MemContents) value;
    else if (attr == StdAttr.LABEL)
      Label = (String) value;
    else if (attr == StdAttr.LABEL_FONT)
      LabelFont = (Font) value;
    else if (attr == StdAttr.APPEARANCE)
      Appearance = (AttributeOption) value;
  }
}
