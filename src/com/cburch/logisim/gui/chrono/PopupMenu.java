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
package com.cburch.logisim.gui.chrono;
import static com.cburch.logisim.gui.chrono.Strings.S;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.gui.log.SelectionPanel;
import com.cburch.logisim.gui.log.Signal;
import com.cburch.logisim.gui.log.SignalInfo;

public class PopupMenu extends MouseAdapter {

  private class PopupContents extends JPopupMenu {
    public PopupContents() {
      super("Options");

      RadixOption radix = null;
      if (signals.size() > 0) {
        radix = signals.get(0).info.getRadix();
        for (int i = 1; i < signals.size(); i++)
          if (signals.get(i).info.getRadix() != radix)
            radix = null;
      }

      ButtonGroup g = new ButtonGroup();
      for (RadixOption r : RadixOption.OPTIONS) {
        JRadioButtonMenuItem m = new JRadioButtonMenuItem(r.toDisplayString());
        add(m);
        m.setEnabled(signals.size() > 0);
        g.add(m);
        if (r == radix)
          m.setSelected(true);
        m.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            for (Signal s : signals)
              s.info.setRadix(r);
          }
        });
      }

      // todo: option to expand/collapse bus signals
      JMenuItem m;

      addSeparator();
      m = new JMenuItem(com.cburch.logisim.gui.menu.Strings.S.get("editClearItem"));
      add(m);
      m.setEnabled(signals.size() > 0);
      m.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          SignalInfo.List items = new SignalInfo.List();
          for (Signal s : signals)
            items.add(s.info);
          chronoPanel.getModel().remove(items);
        }
      });

      addSeparator();
      m = new JMenuItem(com.cburch.logisim.gui.log.Strings.S.get("addRemoveSignals"));
      add(m);
      m.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          SelectionPanel.doDialog(chronoPanel.getLogFrame());
        }
      });

    }
  }

	private List<Signal> signals;
	private ChronoPanel chronoPanel;

	public PopupMenu(ChronoPanel p, List<Signal> s) {
    chronoPanel = p;
		signals = s;
	}

	public void doPop(MouseEvent e) {
		PopupContents menu = new PopupContents();
		menu.show(e.getComponent(), e.getX(), e.getY());
	}

	public void mousePressed(MouseEvent e) {
		if (e.isPopupTrigger())
			doPop(e);
	}

	public void mouseReleased(MouseEvent e) {
		if (e.isPopupTrigger())
			doPop(e);
	}
}
