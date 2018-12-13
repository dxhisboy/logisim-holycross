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

package com.cburch.logisim.file;
import static com.cburch.logisim.file.Strings.S;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.swing.JOptionPane;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitTransaction;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.std.hdl.VhdlContent;
import com.cburch.logisim.std.hdl.VhdlContent;
import com.cburch.logisim.std.hdl.VhdlEntity;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class XmlClipReader extends XmlReader {

  private static <K, T> Collection<T> values(HashMap<K, T> map) {
    ArrayList<T> vals = new ArrayList<>();
    for (K k : map.keySet())
      vals.add((T)map.get(k));
    return vals;
  }

  public class ReadClipContext extends ReadContext {

    HashSet<Component> components = new HashSet<>(); // the selection, or null if cancelled
    HashSet<Component> badComponents = new HashSet<>(); // components with missing dependencies

    HashMap<String, Circuit> circuits = new HashMap<>(); // circuit dependencies
    HashMap<String, VhdlContent> vhdl = new HashMap<>(); // vhdl dependencies
    HashMap<String, Library> libraries = new HashMap<>(); // library dependencies
    HashMap<String, Tool> tools = new HashMap<>(); // tools for circuit and vhdl dependencies
    List<CircuitData> circuitsData = new ArrayList<>(); // data for circuits to be constructed 

    HashMap<String, Element> cDep = new HashMap<>(); // name -> circuit dependencies
    HashMap<String, Element> vDep = new HashMap<>(); // name -> vhdl dependencies
    HashMap<String, Element> lDep = new HashMap<>(); // name -> library dependencies
    HashMap<String, String> lDesc = new HashMap<>(); // name -> library description dependencies

    ReadClipContext(LogisimFile f) { super(f, null); }

    public Collection<Component> getComponents() { return components; }
    public Collection<Circuit> getCircuits() { return values(circuits); }
    public Collection<VhdlContent> getVhdl() { return values(vhdl); }
    public Collection<Library> getLibraries() { return values(libraries); }
    public CircuitTransaction getCircuitTransaction() {
      return new XmlCircuitReader(this, circuitsData);
    }

    Library findLibrary(String name) throws XmlReaderException {
      System.out.println("finding library " + name);
      if (name == null || name.equals(""))
        return file;
      String desc = lDesc.get(name);
      System.out.println("desc library " + desc);
      if (desc == null)
        throw new XmlReaderException(S.fmt("libMissingError", name));
      for (Library lib : file.getLibraries()) {
        System.out.printf("file contains lib: %s\n", LibraryManager.instance.getDescriptor(loader, lib));
        if (LibraryManager.instance.getDescriptor(loader, lib).equals(desc))
          return lib;
      }
      return null;
    }

    Library addMissingLibrary(String name) throws XmlReaderException, LoadCanceledByUser {
      Library lib = libraries.get(name);
      if (lib != null)
        return lib;
      String desc = lDesc.get(name);
      if (desc == null)
        throw new XmlReaderException(S.fmt("libMissingError", name));
      Element elt = lDep.get(name);
      if (elt != null) {
        lib = loader.loadLibrary(desc);
        if (lib != null) {
          for (Element e : XmlIterator.forChildElements(elt, "tool")) {
            if (!e.hasAttribute("name")) {
              loader.showError(S.get("toolNameMissingError"));
            } else {
              String toolName = e.getAttribute("name");
              Tool tool = lib.getTool(toolName);
              if (tool != null) {
                try {
                  initAttributeSet(e, tool.getAttributeSet(), tool);
                } catch (XmlReaderException ex) {
                  addErrors(ex, "lib." + name + "." + toolName);
                }
              }
            }
          }
          libraries.put(name, lib);
          return lib;
        }
      }
      throw new XmlReaderException(S.fmt("libMissingError", name));
    }

    Tool addMissingTool(String name) throws XmlReaderException {
      Tool tool = tools.get(name);
      if (tool != null)
        return tool;
      Element elt;
      elt = cDep.get(name);
      if (elt != null)
        return addMissingCircuit(name, elt);
      elt = vDep.get(name);
      if (elt != null)
        return addMissingVhdl(name, elt);
      throw new XmlReaderException(S.fmt("compUnknownError", name));
    }

    Tool addMissingCircuit(String name, Element elt) throws XmlReaderException {
      Circuit circ = new Circuit(name, file);
      // this part is in a transaction that gets executed later
      CircuitData circData = new CircuitData(this, elt, circ);
      circuitsData.add(circData);
      Tool tool = new AddTool(null, circ.getSubcircuitFactory());
      tools.put(name, tool);
      return tool;
    }

    Tool addMissingVhdl(String name, Element elt) throws XmlReaderException {
      String vhdl = elt.getTextContent();
      VhdlContent contents = VhdlContent.parse(name, vhdl, file);
      Tool tool = new AddTool(null, new VhdlEntity(contents));
      tools.put(name, tool);
      return tool;
    }

    private Component parseComponent(Element elt, int depth)
    throws XmlReaderException, LoadCanceledByUser {
      boolean isBad = false;
      String name = elt.getAttribute("name");
      if (name == null || name.equals(""))
        throw new XmlReaderException(S.get("compNameMissingError"));
      String libName = elt.getAttribute("lib");
      Library lib = findLibrary(libName);
      if (lib == null) {
        isBad = true;
        lib = addMissingLibrary(libName);
      }
      Tool circTool = lib.getTool(name);
      if (circTool == null) {
        if (libName != null && !libName.equals(""))
          throw new XmlReaderException(S.fmt("compAbsentError", name, libName));
        isBad = true;
        circTool = addMissingTool(name);
      }
      if (!(circTool instanceof AddTool))
        throw new XmlReaderException(S.fmt("compInvalidError", name));
      ComponentFactory source = ((AddTool)circTool).getFactory();
      AttributeSet attrs = source.createAttributeSet();
      initAttributeSet(elt, attrs, source);
      Location loc = XmlCircuitReader.parseComponentLoc(elt, name); // source.getName()
      Component comp = source.createComponent(loc, attrs);
      if (depth == 0 && isBad)
        badComponents.add(comp);
      return comp;
    }

    private void parseSelection(Element elt)
        throws XmlReaderException, LoadCanceledByUser {
      
      if (!"clipdata".equals(elt.getTagName()))
        throw new XmlReaderException("unexpected xml data: " + elt.getTagName());

      // Determine the version producing this clipboard selection.
      // Note: clipdata can only come from 4.0.0-HC or above.
      sourceVersion = LogisimVersion.parse(elt.getAttribute("source"));
      System.out.printf("src version is %s\n", sourceVersion);
      
      // first, record clipboard library, circuit, and vhdl names
      for (Element o : XmlIterator.forChildElements(elt, "lib")) {
        lDep.put(o.getAttribute("name"), o);
        lDesc.put(o.getAttribute("name"), o.getAttribute("desc"));
      }
      for (Element o : XmlIterator.forChildElements(elt, "circuit"))
        cDep.put(o.getAttribute("name"), o);
      for (Element o : XmlIterator.forChildElements(elt, "vhdl"))
        vDep.put(o.getAttribute("name"), o);

      // second, reconstruct selection wires and components
      for (Element s : XmlIterator.forChildElements(elt, "selection")) {
        for (Element c : XmlIterator.forChildElements(s)) {
          switch (c.getTagName()) {
          case "wire":
      System.out.println("wire");
            components.add(XmlCircuitReader.parseWire(c));
            break;
          case "comp":
      System.out.println("comp");
            components.add(parseComponent(c, 0));
            break;
          default:
            // do nothing
          }
        }
      }

      if (badComponents.isEmpty())
        return; // done

      String msg = "Some pasted components depend on items not " +
          "included in this project.\n";
      if (!circuits.isEmpty()) {
        msg += String.format("%d missing circuits: ", circuits.size());
        int i = 0;
        for (String circName : circuits.keySet())
          msg += (i++ > 0 ? ", " : "") + circName;
        msg += "\n";
      }
      if (!vhdl.isEmpty()) {
        msg += String.format("%d missing VHDL: ", vhdl.size());
        int i = 0;
        for (String vhdlName : vhdl.keySet())
          msg += (i++ > 0 ? ", " : "") + vhdlName;
        msg += "\n";
      }
      if (!libraries.isEmpty()) {
        msg += String.format("%d missing libraries: ", libraries.size());
        int i = 0;
        for (String libName : libraries.keySet())
          msg += (i++ > 0 ? ", " : "") + libraries.get(libName).getDisplayName();
        msg += "\n";
      }
      String opts[] = new String[] { "Add Missing Dependencies",
        "Skip Affected Components", "Cancel" };
      int ans = JOptionPane.showOptionDialog(
          null/*parent*/, msg, "Missing Dependencies"/*title*/,
          JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null/*icon*/,
          opts, opts[0]);
      if (ans == JOptionPane.YES_OPTION || ans == JOptionPane.OK_OPTION)
        return; // done
      circuits.clear();
      vhdl.clear();
      libraries.clear();
      tools.clear();
      circuitsData.clear();
      cDep.clear();
      vDep.clear();
      lDep.clear();
      lDesc.clear();
      if (ans == JOptionPane.NO_OPTION) {
        components.removeAll(badComponents);
        badComponents.clear();
        return; // done
      } else {
        components.clear();
        badComponents.clear();
        throw new LoadCanceledByUser();
      }
    }

  }

  private ReadClipContext readSelection(InputStream is)
      throws IOException, XmlReaderException, SAXException, LoadCanceledByUser {
    Document doc = loadXmlFrom(is);
    Element elt = doc.getDocumentElement();
    // elt = ensureLogisimCompatibility(elt);
    // considerRepairs(doc, elt);
   
    System.out.println("dst file is " + dstFile);
    ReadClipContext context = new ReadClipContext(dstFile);

    context.parseSelection(elt);

    if (context.messages.size() > 0) {
      StringBuilder all = new StringBuilder();
      for (String msg : context.messages) {
        all.append(msg);
        all.append("\n");
      }
      loader.showError(all.substring(0, all.length() - 1));
    }
    return context;
  }

  private LogisimFile dstFile;
  private Loader loader;

  XmlClipReader(LogisimFile dstFile, Loader loader) {
    this.dstFile = dstFile;
    this.loader = loader;
  }

  public static ReadClipContext parseSelection(LogisimFile dstFile, Loader loader, String xml)
      throws IOException, XmlReaderException, SAXException, LoadCanceledByUser {
    InputStream in = new ByteArrayInputStream(xml.getBytes("UTF-8"));
    XmlClipReader reader = new XmlClipReader(dstFile, loader);
    return reader.readSelection(in);
  }

}