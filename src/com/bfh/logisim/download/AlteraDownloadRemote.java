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
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.bfh.logisim.fpga.Chipset;
import com.bfh.logisim.fpga.PinBindings;
import com.bfh.logisim.fpga.PullBehavior;
import com.bfh.logisim.gui.Console;
import com.bfh.logisim.gui.Commander;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.cburch.logisim.hdl.Hdl;

public class AlteraDownloadRemote extends AlteraDownload {

  protected AlteraDownloadRemote() {
    supportsRemoteJTAG = true;
  }

  @Override
  public boolean readyForDownload() {
    if (remoteJTAG)
      return super.readyForDownload();
    String fmt = settings.GetUseRBF() ? "rbf" : "svf";
    return new File(sandboxPath + TOP_HDL + "." + fmt).exists();
  }

  @Override
  public ArrayList<Stage> initiateDownload(Commander cmdr) {
    ArrayList<Stage> stages = new ArrayList<>();

    //  example: http://some.server.org/home/quartus/synthesize.php
    String url = settings.GetAlteraToolPath();
    //  example: http://some.server.org/
    String urlbase = url.substring(0, url.indexOf('/', 8)+1);
    //  example: http://some.server.org/home/quartus/
    String urldir = url.substring(0, url.lastIndexOf('/')+1);

    //  example: /tmp/project_fpga_workspace/foo/bar.zip
    String zipname = projectPath.substring(0, projectPath.length()-1) + ".zip";
    //  example: /tmp/project_fpga_workspace/foo/bar_bitstream.zip
    String bitstreamzip = projectPath.substring(0, projectPath.length()-1) + "_bitstream.zip";

    String use64bit = settings.GetAltera64Bit() ? "1" : "0";

    if (!readyForDownload()) {
      stages.add(new RunnableStage(
            "synthesis", "Synthesizing (may take a while)",
            "Failed to synthesize design, cannot download") {
        @Override
        protected boolean run() {
          if (writeToFlash && !remoteJTAG) {
            console.printf(console.ERROR, "SPI Flash not yet supported with openFPGAloader.");
            console.printf(console.ERROR, "Uncheck the 'Use Flash?' option or the 'remote JTAG' option.");
            return false;
          }
          console.printf("Compressing project before upload to " + url);

          if (!Zip.compress(console, zipname, projectPath)) {
            console.printf(console.ERROR, "Failed to compress project.");
            return false;
          }

          // just in case of non-remote JTAG via openFPGAloader
          String openFPGAloaderFormat = settings.GetUseRBF() ? "rbf" : "svf";

          if (!HTTP.post(console, url, "operation", "synthesize",
                "use64bit", use64bit,
                "flashname", board.fpga.FlashName,
                "format", openFPGAloaderFormat,
                "zipfile", new File(zipname)))
            return false;

          String resulturl;
          String lastline = console.getText().get(console.getText().size()-1);
          if (lastline.startsWith("RESULT: /")) {
            resulturl = urlbase + lastline.substring(9);
          } else if (lastline.startsWith("RESULT: ")) {
            resulturl = urldir + lastline.substring(8);
          } else {
            console.printf(console.ERROR, "Failed.");
            return false;
          }

          if (!HTTP.get(console, resulturl, bitstreamzip))
            return false;

          if (!Zip.uncompress(console, bitstreamzip, projectPath))
            return false;

          return true;
        }
      });
    }


    if (remoteJTAG) {
      stages.add(new RunnableStage(
            "scan", "Searching for FPGA Devices",
            "Could not find any FPGA devices. Did you connect the FPGA board?") {
        @Override
        protected boolean prep() {
          boolean ok = prepForScan(cmdr, console);
          cancelled = scanWasCancelled;
          return ok;
        }
        @Override
        protected boolean run() {
          console.printf("Listing cables from " + url);
          return HTTP.post(console, url, "operation", "list-cables",
                  "use64bit", use64bit);
        }
        @Override
        protected boolean post() {
          boolean ok = postScanDetectCable(cmdr, console);
          cancelled = scanWasCancelled;
          return ok;
        }
      });
      stages.add(new RunnableStage(
            "remote download", "Downloading to Remote FPGA", 
            "Failed to download design; did you connect the board?") {
        @Override
        protected boolean run() {
          if (writeToFlash)
            return HTTP.post(console, url, "operation", "program", 
                "use64bit", use64bit,
                "mode", "as", "cable", cablename, "bitfile", new File(sandboxPath+flashfile));
          else
            return HTTP.post(console, url, "operation", "program",
                "use64bit", use64bit,
                "mode", "jtag", "cable", cablename, "bitfile", new File(sandboxPath+bitfile));
        }
        @Override
        protected boolean post() {
          String lastline = console.getText().get(console.getText().size()-1);
          return lastline.startsWith("success");
        }
      });
    } else {
      final String openFPGAloader = findOpenFPGAloaderExecutable();
      if (openFPGAloader == null) {
        return new ArrayList<>();
      }
      if (board.openFPGAloader_name == null) {
        err.AddFatalError("Board does not support openFPGAloader yet.");
        return new ArrayList<>();
      }
      stages.add(new ProcessStage(
            "download", "Downloading to Local FPGA",
            null /* will be assigned in prep() */,
            "Failed to download design; did you connect the board?") {
        protected boolean prep() {
          boolean ok = prepForScan(cmdr, console);
          cancelled = scanWasCancelled;
          cmd = new ArrayList<>();
          cmd.add(openFPGAloader);
          cmd.add("-b");
          cmd.add(board.openFPGAloader_name);
          cmd.add(bitfile);
          return ok;
        }
      });
    }
    return stages;
  }

  private String findOpenFPGAloaderExecutable() {
    String p = settings.GetOpenFPGALoaderPath();
    if (p != null) {
      File script = new File(p);
      if (script.exists() && !script.isDirectory() && script.canExecute())
        return p;
      if (script.exists() && script.isDirectory()) {
        String pp = p + "/openFPGAloader";
        script = new File(pp);
        if (script.exists() && !script.isDirectory() && script.canExecute())
          return pp;
      }
      err.AddFatalError("OpenFPGAloaderPath="+p+" is not executable, nor is it a directory"
          + " containing openFPGAloader. Please adjust FPGA Settings then try again.");
      return null;
    }
    // Try just using "openFPGAloader", hope it is found on system path?
    return "openFPGAloader";
  }

  protected boolean prepForScan(Commander cmdr, Console console) {
    if (remoteJTAG)
      return super.prepForScan(cmdr, console);

    // local JTAG via openFPGAloader
    String fmt = settings.GetUseRBF() ? "rbf" : "svf";
    if (new File(sandboxPath + TOP_HDL + "." + fmt).exists()) {
      bitfile = TOP_HDL + "." + fmt;
    }
    if (bitfile == null) {
      console.printf(console.ERROR, "Error: Design must be synthesized before download.");
      return false;
    }
    if (!cmdr.confirmDownload()) {
      scanWasCancelled = true;
      return false;
    }
    return true;
  }
}
