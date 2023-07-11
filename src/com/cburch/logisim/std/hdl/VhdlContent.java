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

package com.cburch.logisim.std.hdl;
import static com.cburch.logisim.std.Strings.S;

import java.awt.Dimension;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.cburch.hdl.HdlModel;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.util.Softwares;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.instance.Port;
import com.bfh.logisim.netlist.CorrectLabel;

public class VhdlContent extends HdlContent {

  public static VhdlContent create(String name, LogisimFile file) {
    VhdlContent content = new VhdlContent(name, file);
    String s = TEMPLATE;
    s = s.replaceAll("%project%", file.getName());
    s = s.replaceAll("%file%", name + ".vhdl");
    s = s.replaceAll("%entityname%", name);
    if (!content.setContent(s))
      content.showErrors();
    return content;
  }

  public static VhdlContent parse(String name, String vhdl, LogisimFile file) {
    VhdlContent content = new VhdlContent(name, file);
    if (!content.setContent(vhdl))
      content.showErrors();
    return content;
  }

  private static String loadTemplate() {
    InputStream input = VhdlContent.class.getResourceAsStream(RESOURCE);
    BufferedReader in = new BufferedReader(new InputStreamReader(input));

    StringBuilder tmp = new StringBuilder();
    String line;

    try {
      while ((line = in.readLine()) != null) {
        tmp.append(line);
        tmp.append(System.getProperty("line.separator"));
      }
    } catch (IOException ex) {
      return "";
    } finally {
      try {
        if (input != null)
          input.close();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }

    return tmp.toString();
  }

  private static final String RESOURCE = "/resources/logisim/hdl/vhdl.templ";

  private static final String TEMPLATE = loadTemplate();

  protected AttributeSet staticAttrs;
  protected StringBuffer content;
  protected boolean valid;
  protected List<VhdlParser.PortDescription> ports;
  protected Generic[] generics;
  protected List<Attribute<Integer>> genericAttrs;
  protected String name;
  protected AttributeOption appearance = StdAttr.APPEAR_FPGA;
  protected String libraries;
  protected String architecture;
  private LogisimFile logiFile;
  private VhdlEntity entityFactory;

  protected VhdlContent(String name, LogisimFile file) {
    logiFile = file;
    this.name = name;
    ports = new ArrayList<VhdlParser.PortDescription>();
    entityFactory = new VhdlEntity(this);
  }

  public VhdlEntity getEntityFactory() {
    return entityFactory;
  }

  // public VhdlContent clone() {
  //   try {
  //     VhdlContent ret = (VhdlContent) super.clone();
  //     ret.content = new StringBuffer(this.content);
  //     ret.valid = this.valid;
  //     return ret;
  //   } catch (CloneNotSupportedException ex) {
  //     return this;
  //   }
  // }

  @Override
  public boolean compare(HdlModel model) {
    return compare(model.getContent());
  }

  @Override
  public boolean compare(String value) {
    return content.toString().replaceAll("\\r\\n|\\r|\\n", " ")
        .equals(value.replaceAll("\\r\\n|\\r|\\n", " "));
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  public String getArchitecture() {
    if (architecture == null)
      return "";

    return architecture;
  }

  @Override
  public String getContent() {
    return content.toString();
  }

  public Generic[] getGenerics() {
    return generics == null ? new Generic[0] : generics;
  }

  public List<Attribute<Integer>> getGenericAttributes() {
    if (genericAttrs == null) {
      genericAttrs = new ArrayList<Attribute<Integer>>();
      for (Generic g : getGenerics())
        genericAttrs.add(VhdlEntityAttributes.forGeneric(g));
    }
    return genericAttrs;
  }

  public String getLibraries() {
    if (libraries == null)
      return "";

    return libraries;
  }

  @Override
  public String getName() {
    if (name == null)
      return "";
    return name;
  }

  public AttributeOption getAppearance() {
    return appearance;
  }

  public void setAppearance(AttributeOption a) {
    appearance = a;
    fireAppearanceChanged();
  }

  public List<VhdlParser.PortDescription> getPorts() {
    return ports;
  }

  public AttributeSet getStaticAttributes() {
    return staticAttrs;
  }

  public void aboutToSave() {
    fireAboutToSave();
  }

  static final String ENTITY_PATTERN = "(\\s*\\bentity\\s+)%entityname%(\\s+is)\\b";
  static final String ARCH_PATTERN = "(\\s*\\barchitecture\\s+\\w+\\s+of\\s+)%entityname%\\b";
  static final String END_PATTERN = "(\\s*\\bend\\s+)%entityname%(\\s*;)";

  /**
   * Check if a given label could be a valid VHDL variable name
   *
   * @param label
   *            candidate VHDL variable name
   * @return true if the label is NOT a valid name, false otherwise
   */
  public static boolean labelVHDLInvalid(String label) {
    if (!label.matches("^[A-Za-z][A-Za-z0-9_]*") || label.endsWith("_")
        || label.matches(".*__.*"))
      return (true);
    if (CorrectLabel.VHDLKeywords.contains(label.toLowerCase()))
      return true;
    return (false);
  }

  public static boolean labelVHDLInvalidNotify(String label, LogisimFile file) {
    String err = null;
    if (!label.matches("^[A-Za-z][A-Za-z0-9_]*") || label.endsWith("_") || label.matches(".*__.*")) {
      err = S.get("vhdlInvalidNameError");
    } else if (CorrectLabel.VHDLKeywords.contains(label.toLowerCase())) {
      err = S.get("vhdlKeywordNameError");
    } else if (file != null && file.containsFactory(label)) {
      err = S.get("vhdlDuplicateNameError");
    } else {
      return false;
    }
    JOptionPane.showMessageDialog(null, label + ": " + err, S.get("validationParseError"),
        JOptionPane.ERROR_MESSAGE);
    return true;
  }

  public boolean setName(String name) {
    // System.out.println("vhdl set name " + name);
    if (name == null)
      return false;
    if (labelVHDLInvalidNotify(name, logiFile))
      return false;
    String entPat = ENTITY_PATTERN.replaceAll("%entityname%", this.name);
    String archPat = ARCH_PATTERN.replaceAll("%entityname%", this.name);
    String endPat = END_PATTERN.replaceAll("%entityname%", this.name);
    String s = content.toString();
    s = s.replaceAll("(?is)" + entPat, "$1"+name+"$2"); // entity NAME is
    s = s.replaceAll("(?is)" + archPat, "$1"+name); // architecture foo of NAME
    s = s.replaceAll("(?is)" + endPat, "$1"+name+"$2"); // end NAME ;
    return setContent(s);
  }

  private StringBuffer errTitle = new StringBuffer();
  private StringBuffer errMessage = new StringBuffer();
  private int errCode = 0;
  private Exception errException;
  @Override
  public void showErrors() {
    if (valid && errTitle.length() == 0 && errMessage.length() == 0)
      return;
    if (errException != null)
      errException.printStackTrace();
    if (errCode == Softwares.ERROR) {
      JTextArea message = new JTextArea();
      message.setText(errMessage.toString());
      message.setEditable(false);
      message.setLineWrap(false);
      message.setMargin(new Insets(5, 5, 5, 5));

      JScrollPane sp = new JScrollPane(message);
      sp.setPreferredSize(new Dimension(700, 400));

      JOptionPane.showOptionDialog(null, sp, errTitle.toString(),
          JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE, null,
          new String[] { S.get("validationErrorButton") },
          S.get("validationErrorButton"));
    } else if (errCode == Softwares.ABORD) {
      JOptionPane.showMessageDialog(null, errMessage.toString(),
          errTitle.toString(), JOptionPane.INFORMATION_MESSAGE);
    } else {
      JOptionPane.showMessageDialog(null, errMessage.toString(),
          errTitle.toString(), JOptionPane.ERROR_MESSAGE);
    }
  }

  @Override
  public boolean setContent(String vhdl) {
    // System.out.println("vhdl set content..." + vhdl.length());
    if (valid && content.toString().equals(vhdl))
      return true;
    content = new StringBuffer(vhdl);
    valid = false;
    try {
      errTitle.setLength(0);
      errMessage.setLength(0);
      errCode = Softwares.validateVhdl(content.toString(), errTitle, errMessage);
      if (errCode != Softwares.SUCCESS)
        return false;

      VhdlParser parser = new VhdlParser(name, content.toString());
      try {
        parser.parse();
      } catch (Exception ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.length() == 0)
          msg = ex.toString();
        errTitle.append(S.get("validationParseError"));
        errMessage.append(msg);
        errException = ex;
        return false;
      }

      if (!parser.getName().equals(name)) {
        if (labelVHDLInvalidNotify(parser.getName(), logiFile))
          return false;
      } else {
        if (labelVHDLInvalidNotify(parser.getName(), null))
          return false;
      }

      valid = true;
      name = parser.getName();

      libraries = parser.getLibraries();
      architecture = parser.getArchitecture();

      ports.clear();
      ports.addAll(parser.getInputs());
      ports.addAll(parser.getOutputs());
      // System.out.println("did adding ports..." + ports.size());
      // Thread.dumpStack();

      // If name and type is unchanged, keep old generic and attribute.
      Generic[] oldGenerics = generics;
      List<Attribute<Integer>> oldAttrs = genericAttrs;

      generics = new Generic[parser.getGenerics().size()];
      genericAttrs = new ArrayList<Attribute<Integer>>();
      int i = 0;
      for (Generic g : parser.getGenerics()) {
        boolean found = false;
        if (oldGenerics != null) {
          for (int j = 0; j < oldGenerics.length; j++) {
            Generic old = oldGenerics[j];
            if (old != null && old.getName().equals(g.getName()) && old.getType().equals(g.getType())) {
              generics[i] = old;
              oldGenerics[j] = null;
              genericAttrs.add(oldAttrs.get(j));
              found = true;
              break;
            }
          }
        }
        if (!found) {
          generics[i] = g;
          genericAttrs.add(VhdlEntityAttributes.forGeneric(generics[i]));
        }
        i++;
      }

      staticAttrs = VhdlEntityAttributes.createBaseAttrs(this);

      valid = true;
      return true;
    } finally {
      fireContentSet();
    }
  }
}
