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

package com.cburch.draw.shapes;
import static com.cburch.draw.Strings.S;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

import com.cburch.draw.util.EditableLabel;
import com.cburch.logisim.circuit.appear.DynamicCondition;
import com.cburch.logisim.circuit.appear.DynamicConditionAttribute;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.util.UnmodifiableList;

public class DrawAttr {
	private static List<Attribute<?>> createAttributes(Attribute<?>[] values) {
		return UnmodifiableList.create(values);
	}

	public static List<Attribute<?>> getFillAttributes(AttributeOption paint) {
		if (paint.equals(PAINT_STROKE)) {
			return ATTRS_FILL_STROKE;
		} else if (paint.equals(PAINT_FILL)) {
			return ATTRS_FILL_FILL;
		} else {
			return ATTRS_FILL_BOTH;
		}
	}

	public static List<Attribute<?>> getRoundRectAttributes(
			AttributeOption paint) {
		if (paint.equals(PAINT_STROKE)) {
			return ATTRS_RRECT_STROKE;
		} else if (paint.equals(PAINT_FILL)) {
			return ATTRS_RRECT_FILL;
		} else {
			return ATTRS_RRECT_BOTH;
		}
	}

	public static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN,
			12);

	public static final AttributeOption HALIGN_LEFT = new AttributeOption(
			Integer.valueOf(EditableLabel.LEFT), S.getter("alignLeft"));
	public static final AttributeOption HALIGN_CENTER = new AttributeOption(
			Integer.valueOf(EditableLabel.CENTER), S.getter("alignCenter"));
	public static final AttributeOption HALIGN_RIGHT = new AttributeOption(
			Integer.valueOf(EditableLabel.RIGHT), S.getter("alignRight"));

	public static final AttributeOption VALIGN_TOP = new AttributeOption(
			Integer.valueOf(EditableLabel.TOP), S.getter("alignTop"));
	public static final AttributeOption VALIGN_MIDDLE = new AttributeOption(
			Integer.valueOf(EditableLabel.MIDDLE), S.getter("alignMiddle"));
	public static final AttributeOption VALIGN_BASELINE = new AttributeOption(
			Integer.valueOf(EditableLabel.BASELINE), S.getter("alignBaseline"));
	public static final AttributeOption VALIGN_BOTTOM = new AttributeOption(
			Integer.valueOf(EditableLabel.BOTTOM), S.getter("alignBottom"));

	public static final AttributeOption PAINT_STROKE = new AttributeOption(
			"stroke", S.getter("paintStroke"));
	public static final AttributeOption PAINT_FILL = new AttributeOption(
			"fill", S.getter("paintFill"));
	public static final AttributeOption PAINT_STROKE_FILL = new AttributeOption(
			"both", S.getter("paintBoth"));
	public static final Attribute<Font> FONT = Attributes.forFont("font",
			S.getter("attrFont"));
	public static final Attribute<AttributeOption> HALIGNMENT = Attributes
			.forOption("halign", S.getter("attrHAlign"),
					new AttributeOption[] { HALIGN_LEFT, HALIGN_CENTER, HALIGN_RIGHT });
	public static final Attribute<AttributeOption> VALIGNMENT = Attributes
			.forOption("valign", S.getter("attrVAlign"),
					new AttributeOption[] { VALIGN_TOP, VALIGN_MIDDLE, VALIGN_BASELINE, VALIGN_BOTTOM });
	public static final Attribute<AttributeOption> PAINT_TYPE = Attributes
			.forOption("paintType", S.getter("attrPaint"),
					new AttributeOption[] { PAINT_STROKE, PAINT_FILL,
							PAINT_STROKE_FILL });
	public static final Attribute<Integer> STROKE_WIDTH = Attributes
			.forIntegerRange("stroke-width", S.getter("attrStrokeWidth"),
					1, 8);
	public static final Attribute<Color> STROKE_COLOR = Attributes.forColor(
			"stroke", S.getter("attrStroke"));

	public static final Attribute<Color> FILL_COLOR = Attributes.forColor(
			"fill", S.getter("attrFill"));
	public static final Attribute<Color> TEXT_DEFAULT_FILL = Attributes
			.forColor("fill", S.getter("attrFill"));
	public static final Attribute<Integer> CORNER_RADIUS = Attributes
			.forIntegerRange("rx", S.getter("attrRx"), 1, 1000);

	public static final Attribute<DynamicCondition> DYNAMIC_CONDITION =
      new DynamicConditionAttribute("dynamic-condition", S.getter("attrDynamicCondition"));

	public static final List<Attribute<?>> ATTRS_TEXT // for text
	= createAttributes(new Attribute[] { FONT, HALIGNMENT, VALIGNMENT, FILL_COLOR, DYNAMIC_CONDITION });
	public static final List<Attribute<?>> ATTRS_TEXT_TOOL // for text tool
	= createAttributes(new Attribute[] { FONT, HALIGNMENT, VALIGNMENT, TEXT_DEFAULT_FILL, DYNAMIC_CONDITION });
	public static final List<Attribute<?>> ATTRS_STROKE // for line, polyline
	= createAttributes(new Attribute[] { STROKE_WIDTH, STROKE_COLOR, DYNAMIC_CONDITION });

	// attribute lists for rectangle, oval, polygon
	private static final List<Attribute<?>> ATTRS_FILL_STROKE = createAttributes(new Attribute[] {
			PAINT_TYPE, STROKE_WIDTH, STROKE_COLOR, DYNAMIC_CONDITION });
	private static final List<Attribute<?>> ATTRS_FILL_FILL = createAttributes(new Attribute[] {
			PAINT_TYPE, FILL_COLOR , DYNAMIC_CONDITION});
	private static final List<Attribute<?>> ATTRS_FILL_BOTH = createAttributes(new Attribute[] {
			PAINT_TYPE, STROKE_WIDTH, STROKE_COLOR, FILL_COLOR, DYNAMIC_CONDITION });

	// attribute lists for rounded rectangle
	private static final List<Attribute<?>> ATTRS_RRECT_STROKE = createAttributes(new Attribute[] {
			PAINT_TYPE, STROKE_WIDTH, STROKE_COLOR, CORNER_RADIUS, DYNAMIC_CONDITION });

	private static final List<Attribute<?>> ATTRS_RRECT_FILL = createAttributes(new Attribute[] {
			PAINT_TYPE, FILL_COLOR, CORNER_RADIUS, DYNAMIC_CONDITION });

	private static final List<Attribute<?>> ATTRS_RRECT_BOTH = createAttributes(new Attribute[] {
			PAINT_TYPE, STROKE_WIDTH, STROKE_COLOR, FILL_COLOR, CORNER_RADIUS, DYNAMIC_CONDITION });
}
