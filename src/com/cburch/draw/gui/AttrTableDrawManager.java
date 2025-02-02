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

package com.cburch.draw.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.tools.AbstractTool;
import com.cburch.draw.tools.DrawingAttributeSet;
import com.cburch.draw.tools.SelectTool;
import com.cburch.logisim.gui.appear.AppearanceCanvas;
import com.cburch.logisim.gui.generic.AttrTable;

public class AttrTableDrawManager implements PropertyChangeListener {
	private AppearanceCanvas canvas;
	private AttrTable table;
	private AttrTableSelectionModel selectionModel;
	private AttrTableToolModel toolModel;

	public AttrTableDrawManager(AppearanceCanvas canvas, AttrTable table,
			DrawingAttributeSet attrs) {
		this.canvas = canvas;
		this.table = table;
		this.selectionModel = new AttrTableSelectionModel(canvas);
		this.toolModel = new AttrTableToolModel(canvas, attrs, null);

		canvas.addPropertyChangeListener(Canvas.TOOL_PROPERTY, this);
		updateToolAttributes();
	}

	public void attributesSelected() {
		updateToolAttributes();
	}

	//
	// PropertyChangeListener method
	//
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		if (prop.equals(Canvas.TOOL_PROPERTY)) {
			updateToolAttributes();
		}
	}

	private void updateToolAttributes() {
		Object tool = canvas.getTool();
		if (tool instanceof SelectTool) {
			table.setAttrTableModel(selectionModel);
		} else if (tool instanceof AbstractTool) {
			toolModel.setTool((AbstractTool) tool);
			table.setAttrTableModel(toolModel);
		} else {
			table.setAttrTableModel(null);
		}
	}
}
