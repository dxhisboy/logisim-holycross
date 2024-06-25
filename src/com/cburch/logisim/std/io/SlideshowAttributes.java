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

import java.util.ArrayList;
import java.util.List;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.std.base.Image;

class SlideshowAttributes extends AbstractAttributeSet {
  private final /*public*/ static SlideshowAttributes instance = new SlideshowAttributes(); // why?
      
  BitWidth addrWidth = BitWidth.create(16);
  int count = 4;
  int width = 320;
  int height = 240;
  Image.ImageContent[] slides = new Image.ImageContent[Slideshow.MAX_SLIDES];
  AttributeOption fit = Slideshow.SCALE;

  public SlideshowAttributes() { }

  @Override
  protected void copyInto(AbstractAttributeSet destObj) { /* nothing to do */ }

  @Override
  public List<Attribute<?>> getAttributes() {
    ArrayList<Attribute<?>> attrs = new ArrayList<>();
    attrs.add(Slideshow.ATTR_WIDTH);
    attrs.add(Slideshow.ATTR_COUNT);
    attrs.add(Slideshow.ATTR_IMG_WIDTH);
    attrs.add(Slideshow.ATTR_IMG_HEIGHT);
    attrs.add(Slideshow.ATTR_FIT);
    for (int i = 0; i < count; i++)
      attrs.add(Slideshow.ATTR_SLIDE.get(i));
    return attrs;
  }

  @Override
  public <E> E getValue(Attribute<E> attr) {
    if (attr == Slideshow.ATTR_WIDTH)
      return (E) addrWidth;
    if (attr == Slideshow.ATTR_COUNT)
      return (E) Integer.valueOf(count);
    if (attr == Slideshow.ATTR_IMG_WIDTH)
      return (E) Integer.valueOf(width);
    if (attr == Slideshow.ATTR_IMG_HEIGHT)
      return (E) Integer.valueOf(height);
    if (attr == Slideshow.ATTR_FIT)
      return (E) fit;
    for (int i = 0; i < count; i++) {
      if (attr == Slideshow.ATTR_SLIDE.get(i))
        return (E) slides[i];
    }
    return null;
  }

  @Override
  public <V> void updateAttr(Attribute<V> attr, V value) {
    if (attr == Slideshow.ATTR_WIDTH)
      addrWidth = (BitWidth) value;
    else if (attr == Slideshow.ATTR_COUNT) {
      int newCount = (Integer) value;
      if (count != newCount) {
        fireAttributeListChanged(); // why before changing value?
        count = newCount;
      }
    }
    else if (attr == Slideshow.ATTR_IMG_WIDTH)
      width = (Integer) value;
    else if (attr == Slideshow.ATTR_IMG_HEIGHT)
      height = (Integer) value;
    else if (attr == Slideshow.ATTR_FIT)
      fit = (AttributeOption) value;
    else {
      for (int i = 0; i < count; i++) {
        if (attr == Slideshow.ATTR_SLIDE.get(i)) {
          slides[i] = (Image.ImageContent) value;
          break;
        }
      }
    }
  }

}
