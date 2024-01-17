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

package com.bfh.logisim.download;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import javax.swing.SwingUtilities;

import com.bfh.logisim.fpga.Board;
import com.bfh.logisim.fpga.Chipset;
import com.bfh.logisim.fpga.PinBindings;
import com.bfh.logisim.gui.Commander;
import com.bfh.logisim.gui.Console;
import com.bfh.logisim.gui.FPGAReport;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.hdlgenerator.TickHDLGenerator;
import com.bfh.logisim.hdlgenerator.ToplevelHDLGenerator;
import com.bfh.logisim.settings.Settings;

public abstract class FPGADownload {

  static final String TOP_HDL = ToplevelHDLGenerator.HDL_NAME;
  static final String CLK_PORT = TickHDLGenerator.FPGA_CLK_NET;

  public final String name;

  public FPGADownload(String name) {
    this.name = name;
  }
  public static String getToolchain(Board board, Settings settings) {
    // First priority: user preference for given board
    String toolchain = settings.GetPreferredToolchain(board.name);
    // Fall back: select toolchain based on vendor
    if (toolchain == null)
      toolchain = vendorToolchain(board.fpga.Vendor);
    // Last restor: apio
    if (toolchain == null)
      toolchain = APIO_TOOLCHAIN;
    return toolchain;
  }

  public static FPGADownload forToolchain(String toolchain, Settings settings) {
    if (toolchain == null)
      toolchain = APIO_TOOLCHAIN;
    switch (toolchain) {
      case ALTERA_QUARTUS_TOOLCHAIN:
        return AlteraDownload.makeNew(settings);
      case XILINX_ISE_TOOLCHAIN:
        return new XilinxDownload();
      case LATTICE_DIAMOND_TOOLCHAIN:
        return new LatticeDownload();
      case LATTICE_ISPLEVER_TOOLCHAIN:
        return new LatticeDownload(); // ???
      case APIO_TOOLCHAIN:
        return new ApioDownload();
      default:
        return new ApioDownload();
    }
  }

  // Parameters set by Commander
  public FPGAReport err;
  public String lang;
  public Board board;
  public Settings settings;
  public String projectPath;
  public String circuitPath;
  public String scriptPath;
  public String sandboxPath;
  public String ucfPath;
  public boolean writeToFlash;

  public abstract boolean toolchainIsInstalled(Settings settings, FPGAReport err);

  public boolean generateScripts(PinBindings ioResources) {
    ArrayList<String> hdlFiles = new ArrayList<>();
    enumerateHDLFiles(circuitPath, hdlFiles);
    return generateScripts(ioResources, hdlFiles);
  }

  public abstract boolean generateScripts(PinBindings ioResources, ArrayList<String> hdlFiles);
  
  public abstract boolean readyForDownload();

  public abstract ArrayList<Stage> initiateDownload(Commander cmdr);
  
  private void enumerateHDLFiles(String path, ArrayList<String> files) {
    if (lang == Settings.VHDL)
      enumerateHDLFiles(path, files,
          FileWriter.EntityExtension + ".vhd",
          FileWriter.ArchitectureExtension + ".vhd");
    else
      enumerateHDLFiles(path, files, ".v", null);
  }
  
  private void enumerateHDLFiles(String path, ArrayList<String> files,
    String entityEnding, String behaviorEnding) {
    File dir = new File(path);
    if (!path.endsWith(File.separator))
      path += File.separator;
    for (File f : dir.listFiles()) {
      String subpath = path + f.getName();
      if (f.isDirectory())
        enumerateHDLFiles(subpath, files, entityEnding, behaviorEnding);
      else if (f.getName().endsWith(entityEnding))
        files.add(subpath.replace("\\", "/"));
      else if (f.getName().endsWith(behaviorEnding))
        files.add(subpath.replace("\\", "/"));
    }
  }

  public final static String ALTERA_QUARTUS_TOOLCHAIN = "Altera Quartus";
  public final static String XILINX_ISE_TOOLCHAIN = "Xilinx ISE";
  public final static String LATTICE_DIAMOND_TOOLCHAIN = "Lattice Diamond";
  public final static String LATTICE_ISPLEVER_TOOLCHAIN = "Lattice ispLEVER";
  public final static String APIO_TOOLCHAIN = "Apio";

  public static String normalizeToolchain(String toolchain) {
    if (toolchain == null)
      return null;
    toolchain = toolchain.toLowerCase().replaceAll("[ -_]+", " ").trim();
    switch (toolchain.toLowerCase()) {
      case "xilinx":
      case "xilinx ise":
      case "ise":
        return XILINX_ISE_TOOLCHAIN;
      case "altera":
      case "altera quartus":
      case "quartus":
        return ALTERA_QUARTUS_TOOLCHAIN;
      case "lattice":
      case "lattice diamond":
      case "diamond":
        return LATTICE_DIAMOND_TOOLCHAIN;
      case "lattice isplever":
      case "isplever":
        return LATTICE_ISPLEVER_TOOLCHAIN;
      case "apio":
        return APIO_TOOLCHAIN;
      default:
        return null;
    }
  }

  public static String vendorToolchain(char chipset) {
    switch (chipset) {
      case Chipset.ALTERA: return ALTERA_QUARTUS_TOOLCHAIN;
      case Chipset.XILINX: return XILINX_ISE_TOOLCHAIN;
      case Chipset.LATTICE: return LATTICE_DIAMOND_TOOLCHAIN;
      default: return null;
    }
  }

  public static String getLanguage(Board board, Settings settings, String toolchain) {
    String lang = settings.GetPreferredHDLType(board.name);
    if (lang != null)
      return lang;
    if (toolchain == null)
      return settings.GetHDLType();
    switch (toolchain) {
      case ALTERA_QUARTUS_TOOLCHAIN:
        return Settings.VHDL;
      case XILINX_ISE_TOOLCHAIN:
        return Settings.VHDL;
      case LATTICE_DIAMOND_TOOLCHAIN:
        return Settings.VHDL; // ??
      case LATTICE_ISPLEVER_TOOLCHAIN:
        return Settings.VHDL; // ??
      case APIO_TOOLCHAIN:
        return Settings.VERILOG;
      default:
        return Settings.VHDL;
    }
  }

  private static final String osname = System.getProperty("os.name");
  private static final boolean windowsOS = osname != null
      && osname.toLowerCase().indexOf("windows") != -1;
  private static final String dotexe = windowsOS ? ".exe" : "";

  public static final String ALTERA_QUARTUS_SH = "quartus_sh" + dotexe;
  public static final String ALTERA_QUARTUS_PGM = "quartus_pgm" + dotexe;
  public static final String ALTERA_QUARTUS_MAP = "quartus_map" + dotexe;
  public static final String ALTERA_QUARTUS_CPF = "quartus_cpf" + dotexe;
  public static final String[] ALTERA_PROGRAMS = {
    ALTERA_QUARTUS_SH, ALTERA_QUARTUS_PGM, ALTERA_QUARTUS_MAP, ALTERA_QUARTUS_CPF,
  };

  public static final String XILINX_XST = "xst" + dotexe;
  public static final String XILINX_NGDBUILD = "ngdbuild" + dotexe;
  public static final String XILINX_MAP = "map" + dotexe;
  public static final String XILINX_PAR = "par" + dotexe;
  public static final String XILINX_BITGEN = "bitgen" + dotexe;
  public static final String XILINX_IMPACT = "impact" + dotexe;
  public static final String XILINX_CPLDFIT = "cpldfit" + dotexe;
  public static final String XILINX_HPREP6 = "hprep6" + dotexe;
  public static final String[] XILINX_PROGRAMS = {
    XILINX_XST, XILINX_NGDBUILD, XILINX_MAP, XILINX_PAR,
    XILINX_BITGEN, XILINX_IMPACT, XILINX_CPLDFIT, XILINX_HPREP6,
  };

  public static final String LATTICE_DIAMOND_WIN = "pnmainc" + dotexe;
  public static final String LATTICE_DIAMOND_UNIX = "diamondc";
  public static final String LATTICE_ISPLEVER_WIN = "projnav" + dotexe;
  public static final String[] LATTICE_PROGRAMS = {
      LATTICE_DIAMOND_WIN, LATTICE_DIAMOND_UNIX // , LATTICE_ISPLEVER_WIN
  };

  public static final String BIN_APIO = "bin/apio";
  // public static final String APIO_PYVENV = "pyvenv.cfg";
  public static final String[] APIO_PROGRAMS = { BIN_APIO }; // APIO_PYVENV

  public class Stage {
    public final String title, msg, errmsg;
    public Console console;
    public Thread thread;
    public int exitValue = -1;
    public boolean failed, cancelled;

    ArrayList<String> cmd;

    public Stage(String title, String msg, 
        ArrayList<String> cmd, String errmsg) {
      this.title = title;
      this.msg = msg;
      this.cmd = cmd;
      this.errmsg = errmsg;
    }

    protected boolean prep() { return true; }
    protected boolean post() { return true; }

    public void startAndThen(Runnable completion) {
      if (!prep()) {
        failed = true;
        completion.run();
        return;
      }
      if (cmd == null) {
        completion.run();
        return;
      }
      console.printf(console.INFO, "Command: %s\n", shellEscape(cmd));
      ProcessBuilder builder = new ProcessBuilder(cmd);
      builder.directory(new File(sandboxPath));
      Process process;
      try {
        process = builder.start();
      } catch (IOException e) {
        console.printf(console.ERROR, e.getMessage());
        failed = true;
        completion.run();
        return;
      }
      InputStream stdout = process.getInputStream();
      InputStream stderr = process.getErrorStream();
      Thread t1 = console.copyFrom(console.INFO, stdout);
      Thread t2 = console.copyFrom(console.WARNING, stderr);
      thread = new Thread(() -> {
        try {
          process.waitFor();
          t1.join(500);
          t2.join(500);
          if (t1.isAlive()) {
            try { stdout.close(); } 
            catch (IOException e) { console.printf(console.ERROR, e.getMessage()); }
            t1.join();
          }
          if (t2.isAlive()) {
            try { stderr.close(); }
            catch (IOException e) { console.printf(console.ERROR, e.getMessage()); }
            t2.join();
          }
          exitValue = process.exitValue();
          if (exitValue != 0 || !post())
            failed = true;
        } catch (InterruptedException ex) {
          process.destroyForcibly();
          failed = true;
        } finally {
          SwingUtilities.invokeLater(completion);
        }
      });
      thread.start();
    }
  }

  public class RunnableStage extends Stage {

    public RunnableStage(String title, String msg, String errmsg) {
      super(title, msg, null, errmsg);
    }

    protected boolean run() { return true; }

    @Override
    public void startAndThen(Runnable completion) {
      if (!prep()) {
        failed = true;
        completion.run();
        return;
      }
      thread = new Thread(() -> {
        try {
          if (!run())
            failed = true;
          else if (!post())
            failed = true;
        // } catch (InterruptedException ex) {
        //   failed = true;
        } finally {
          SwingUtilities.invokeLater(completion);
        }
      });
      thread.start();
    }
  }

  private static String shellEscape(ArrayList<String> cmd) { // just for pretty-printing
    String s = "";
    for (String c : cmd) {
      if (!c.matches("[a-zA-Z0-9-+_=:,.]*")) {
        c = c.replaceAll("\\\\", "\\\\");
        c = c.replaceAll("`", "\\`");
        c = c.replaceAll("\\$", "\\$");
        c = c.replaceAll("!", "\\!");
        c = c.replaceAll("'", "'\\''");
        c = "'" + c + "'";
      }
      s += s.length() > 0 ? " " + c : c;
    }
    return s;
  }

}
