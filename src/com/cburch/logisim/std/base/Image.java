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

package com.cburch.logisim.std.base;
import static com.cburch.logisim.std.Strings.S;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.gui.main.ExportImage;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.Errors;
import com.cburch.logisim.util.JInputDialog;
import com.cburch.logisim.util.StringGetter;

public class Image extends InstanceFactory {

  // Image just displays an image, which can be embedded in the project or
  // linked to an external file. Like Text, this component has no behavior,
  // inputs, or outputs. 

  public static class ImageContent {
    String format; // "PNG" or "JPG"
    Attributes.LinkedFile source;
    byte[] imgData;
    BufferedImage img;
    long timestamp;

    public ImageContent(Attributes.LinkedFile src) {
      this.source = src;
      if (src.relative.toString().toLowerCase().endsWith(".png"))
        format = "PNG";
      else
        format = "JPG";
    }

    public ImageContent(ImageContent other) {
      this.format = other.format;
      this.source = other.source;
      this.imgData = other.imgData;
      this.img = other.img;
      this.timestamp = other.timestamp;
    }

    public ImageContent(File f) throws IOException {
      imgData = Files.readAllBytes(f.toPath());
      ByteArrayInputStream stream = new ByteArrayInputStream(imgData);
      img = ImageIO.read(stream);
      stream.close();
      if (f.toString().toLowerCase().endsWith(".png"))
        format = "PNG";
      else
        format = "JPG";
    }

    public ImageContent(String format, BufferedImage img, byte[] imgData) {
      this.format = format;
      this.img = img;
      this.imgData = imgData;
    }

    public BufferedImage getImage() {
      if (source == null)
        return img;
      long ts = source.absolute.lastModified();
      if (ts == timestamp)
        return img;
      timestamp = ts;
      try {
        img = ImageIO.read(source.absolute);
      } catch (IOException e) {
        img = null;
        // maybe don't warn?
        System.out.println("image file access error: " + source.relative);
      }
      return img;
    }

  }

  public static final Attribute<Attributes.LinkedFile> ATTR_FILENAME_SINGLETON =
      Attributes.forFilename("filename", 
          S.getter("stdImageFilename"), S.getter("stdImageLoadDialogTitle"),
          ExportImage.PNG_FILTER, ExportImage.JPG_FILTER);

  public static final ImageContentAttribute ATTR_IMAGE_CONTENT =
    new ImageContentAttribute("image", S.getter("stdImageContents"));

  public static class ImageContentAttribute extends Attribute<ImageContent> {

    public ImageContentAttribute(String name, StringGetter disp) {
      super(name, disp);
    }

    @Override
    public java.awt.Component getCellEditor(Window source, ImageContent s) {
      return new FileChooser((Frame)source, s);
    }

    @Override
    public String toDisplayString(ImageContent value) {
      if (value == null)
        return S.get("stdImageClickToLoad");
      else if (value.source != null)
        return value.source.relative + " [" + value.source.absolute + "]";
      else
        return String.format(S.get("stdImageImageInfo"), value.format, value.imgData.length);
    }

    @Override
    public String toStandardString(ImageContent value) {
      return toStandardStringRelative(value, null);
    }
    
    @Override
    public String toStandardStringRelative(ImageContent value, String outFilename) {
      if (value == null) {
        return "";
      } else if (value.source != null) {
        return "file:" + ATTR_FILENAME_SINGLETON.toStandardStringRelative(value.source, outFilename);
      } else {
        try {
          ByteArrayOutputStream result = new ByteArrayOutputStream();
          result.write((value.format+":\n").getBytes("UTF-8"));
          byte[] LF = System.lineSeparator().getBytes("UTF-8"); // "\n" or "\r\n"
          OutputStream encoded = Base64.getMimeEncoder(76, LF).wrap(result);
          encoded.write(value.imgData, 0, value.imgData.length);
          try { encoded.flush(); } catch (IOException e) { e.printStackTrace(); }
          try { result.flush(); } catch (IOException e) { e.printStackTrace(); }
          try { encoded.close(); } catch (IOException e) { e.printStackTrace(); }
          try { result.close(); } catch (IOException e) { e.printStackTrace(); }
          return new String(result.toByteArray(), "UTF-8");
        } catch (Exception e) {
          e.printStackTrace();
          return "";
        }
      }
    }

    @Override
    public ImageContent parse(String str) {
      throw new UnsupportedOperationException("parse filename without source");
    }

    @Override
    public ImageContent parseFromUser(Window source, String value) {
      throw new UnsupportedOperationException("parse filename without source");
    }

    @Override
    public ImageContent parseFromFilesystem(File directory, String value) {
      if (value == null || value.equals(""))
        return null;
      if (value.startsWith("file:")) {
        return new ImageContent(ATTR_FILENAME_SINGLETON.parseFromFilesystem(directory, value.substring(5)));
      } else if (value.startsWith("PNG:\n") || value.startsWith("JPG:\n")) {
        String format = value.substring(0, 3);
        try {
          byte[] bytes = value.getBytes("UTF-8");
          ByteArrayInputStream input = new ByteArrayInputStream(bytes, 5, bytes.length-5);
          InputStream decoded = Base64.getMimeDecoder().wrap(input);
          bytes = decoded.readAllBytes();
          decoded.close();
          input.close();
          input = new ByteArrayInputStream(bytes);
          BufferedImage img = ImageIO.read(input);
          input.close();
          return new ImageContent(format, img, bytes);
        } catch (IOException e) {
          e.printStackTrace();
          return null;
        }
      } else {
        throw new IllegalArgumentException("Bad image data for " + getName());
      }
    }
  }

  private static class FileChooser extends java.awt.Component implements JInputDialog<ImageContent> {
    Frame parent;
    ImageContent result;

    FileChooser(Frame parent, ImageContent r) {
      this.parent = parent;
      this.result = r;
    }

    public void setValue(ImageContent r) { result = r; }
    public ImageContent getValue() { return result; }

    public void setVisible(boolean b) {
      if (!b)
        return;
      JInputDialog<Attributes.LinkedFile> chooser =
        (JInputDialog<Attributes.LinkedFile>)
        ATTR_FILENAME_SINGLETON.getCellEditor(parent, result == null ? null : result.source);

      chooser.setVisible(true);
      Attributes.LinkedFile lf = chooser.getValue();

      if (lf == null)
        return;

      long size;
      try {
        size = Files.size(lf.absolute.toPath());
      } catch (IOException ex) {
        Errors.title(S.get("stdImageErrorTitle")).show(S.get("stdImageErrorMessage"), ex);
        return;
      }

      String[] options = {
        S.get("stdImageEmbedOption"),
        S.get("stdImageLinkOption"),
        S.get("stdImageCancelOption") };
      int choice = JOptionPane.showOptionDialog(parent,
          String.format(S.get("stdImageStorageDialogQuestion"), size),
          S.get("stdImageStorageDialogTitle"), 0,
          JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
      if (choice == 0) {
        try {
          result = new ImageContent(lf.absolute);
        } catch (IOException ex) {
          Errors.title(S.get("stdImageErrorTitle")).show(S.get("stdImageErrorMessage"), ex);
          return;
        }
      } else if (choice == 1) {
          result = new ImageContent(lf);
      } else {
        result = null;
      }
    }
  }

  public Image() {
    super("Image", S.getter("stdImageComponent"));
    setIconName("image.gif");
    setShouldSnap(false);
    setAttributes(
        new Attribute[] { ATTR_IMAGE_CONTENT,
          StdAttr.LABEL, StdAttr.LABEL_LOC, StdAttr.LABEL_FONT, StdAttr.LABEL_COLOR },
        new Object[] { null,
            "", Direction.SOUTH, StdAttr.DEFAULT_LABEL_FONT, Color.BLACK });
  }
  
  @Override
  protected void configureNewInstance(Instance instance) {
    instance.computeLabelTextField(0);
    instance.addAttributeListener();
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Image.ImageContent content = attrs.getValue(ATTR_IMAGE_CONTENT);
    BufferedImage img = content == null ? null : content.getImage();
    if (img == null)
      return Bounds.create(0, 0, 20, 20);
    return Bounds.create(0, 0, img.getWidth(), img.getHeight());
  }

  @Override
  public boolean HDLIgnore() { return true; }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.LABEL_LOC)
      instance.computeLabelTextField(0);
    else if (attr == ATTR_IMAGE_CONTENT) {
      instance.recomputeBounds();
      instance.fireInvalidated();
    }
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    paint(painter, true);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    paint(painter, false);
  }

  private void paint(InstancePainter painter, boolean border) {
    Image.ImageContent content = painter.getAttributeValue(ATTR_IMAGE_CONTENT);
    BufferedImage img = content == null ? null : content.getImage();
    Bounds bds = painter.getBounds();
    Graphics g = painter.getGraphics();

    int x = bds.getX();
    int y = bds.getY();
    if (img != null) {
      int w = img.getWidth();
      int h = img.getHeight();
      g.drawImage(img,
          x, y, x+w, y+h,
          0, 0, w, h,
          null);
    } else {
      int w = bds.getWidth();
      int h = bds.getHeight();
      g.setColor(Color.LIGHT_GRAY);
      g.fillRect(x, y, w, h);
      g.setColor(Color.RED);
      g.drawLine(x, y, x+w-1, y+w-1);
      g.drawLine(x, y+w-1, x+w-1, y);
    }

    painter.drawLabel();
    
    if (border) {
      g.setColor(Color.GRAY);
      g.drawRect(x, y, bds.getWidth(), bds.getHeight());
    }
  }

  @Override
  public void propagate(InstanceState state) { }

}
