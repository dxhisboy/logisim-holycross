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

package com.cburch.logisim.gui.main;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.Collection;
import java.util.Collections;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitTransaction;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.file.LoadCanceledByUser;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.XmlClipReader;
import com.cburch.logisim.file.XmlWriter;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.hdl.VhdlContent;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.util.DragDrop;
import com.cburch.logisim.util.PropertyChangeWeakSupport;

public class ExternalClipboard<T>
  implements ClipboardOwner, FlavorListener, PropertyChangeWeakSupport.Producer {

  public static final String contentsProperty = "contents";

  public static final Clipboard sysclip = Toolkit.getDefaultToolkit().getSystemClipboard();

  public static final DataFlavor stringFlavor = DataFlavor.stringFlavor;
  public static final DataFlavor imageFlavor = DataFlavor.imageFlavor;

  // private interface ClipExtractor<T> {
  //   T extractSelection(Transferable xfer) throws Exception;
  // }

  public static final ExternalClipboard<String> forString = new ExternalClipboard<>(stringFlavor);
  //, xfer -> extractString(xfer)

  public static final ExternalClipboard<Image> forImage = new ExternalClipboard<>(imageFlavor);
  // xfer -> extractImage(xfer)

  private DataFlavor dataFlavor;
  // ClipExtractor<T> extractor;
  private T current; // the owned system clip, if any, for this flavor
  private boolean external; // not owned, but system clip is compatible with this flavor
  private boolean available; // current != null || external
  private DragDrop dnd;

  private ExternalClipboard(DataFlavor baseFlavor /*, ClipExtractor<T> extractor */) {
    this.dataFlavor = baseFlavor;
    this.dnd = new DragDrop(LayoutClipboard.mimeTypeComponentsClip, baseFlavor);
    // this.extractor = extractor;
    sysclip.addFlavorListener(this);
    flavorsChanged(null);
  }

  public void flavorsChanged(FlavorEvent e) {
    boolean oldAvail = available;
    // wait a small amount of time until the clipboard is ready (avoid exception "cannot open system clipboard)
    // see https://stackoverflow.com/questions/51797673/in-java-why-do-i-get-java-lang-illegalstateexception-cannot-open-system-clipboa
    try { Thread.sleep(10); } catch (InterruptedException ex) { };
    external = sysclip.isDataFlavorAvailable(dataFlavor);
    available = current != null || external;
    if (oldAvail != available)
      ExternalClipboard.this.firePropertyChange(contentsProperty, oldAvail, available);
  }

  public void lostOwnership(Clipboard clipboard, Transferable contents) {
    current = null;
    flavorsChanged(null);
  }

  public boolean isEmpty() {
    return current == null && !external;
  }

  /*
  public void set(T value) {
    if (value != null && value.length() == 0)
      value = null;
    current = value;
    if (current != null)
      sysclip.setContents(new StringSelection(current), this); 
  }
  */
  
  public void set(Project proj, Component comp, T plain) {
    String xml = XmlWriter.encodeSelection(proj.getLogisimFile(), proj, Collections.singletonList(comp));
    Transferable xfer = null;
    if (xml == null)
      xfer = plainSelection(plain);
    else
      xfer = new XmlAndPlainData(plain, xml);
    if (xfer != null) {
      current = plain;
      sysclip.setContents(xfer, this); 
    }
  }

  private static Transferable plainSelection(Object obj) {
    if (obj instanceof String) {
      String plain = (String)obj;
      if (plain == null || plain.length() == 0)
        return null;
      return new StringSelection(plain);
    } else if (obj instanceof Image) {
      Image plain = (Image)obj;
      if (plain == null)
        return null;
      return new ImageSelection(plain);
    }
    return null;
  }

  private class XmlAndPlainData<T> implements DragDrop.Support {
    T plain;
    String xml;
    XmlAndPlainData(T plain, String xml) { this.plain = plain; this.xml = xml; }

    public DragDrop getDragDrop() { return dnd; }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
      if (flavor.equals(dataFlavor))
        return plain;
      else if (flavor.equals(dnd.dataFlavor))
        return xml;
      else
        throw new UnsupportedFlavorException(flavor);
    }
  }

  private static class ImageSelection implements Transferable {
    private Image img;
    private DataFlavor[] flavors = { DataFlavor.imageFlavor };

    public ImageSelection(Image i) { img = i; }

    public Object getTransferData(DataFlavor flavor)
        throws UnsupportedFlavorException {
        if (flavor.equals(DataFlavor.imageFlavor))
          return img;
        else
          throw new UnsupportedFlavorException(flavor);
    }

    public DataFlavor[] getTransferDataFlavors() { return flavors; }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
      return flavor != null && flavor.equals(DataFlavor.imageFlavor);
    }
  }

  public T get(Project proj) {
    if (current != null)
      return current;
    try {
      Transferable xfer = sysclip.getContents(null);
      if (xfer != null && xfer.isDataFlavorSupported(dataFlavor))
        return (T)xfer.getTransferData(dataFlavor);
        // return extractor.extractSelection(xfer);
    } catch (Exception e) {
      proj.showError("Error parsing clipboard data", e);
    }
    return null;
  }

  // Note: These two handlers are inlined, since they are identical modulo T
  // private static String extractString(Transferable xfer) throws Exception {
  //   return (String)xfer.getTransferData(stringFlavor);
  // }
  // private static Image extractImage(Transferable xfer) throws Exception {
  //   return (Image)xfer.getTransferData(imageFlavor);
  // }

  PropertyChangeWeakSupport propListeners = new PropertyChangeWeakSupport(ExternalClipboard.class);
  public PropertyChangeWeakSupport getPropertyChangeListeners() { return propListeners; }
}
