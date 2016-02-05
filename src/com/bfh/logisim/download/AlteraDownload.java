/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.bfh.logisim.download;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

import com.bfh.logisim.designrulecheck.Netlist;
import com.bfh.logisim.fpgaboardeditor.BoardInformation;
import com.bfh.logisim.fpgaboardeditor.FPGAClass;
import com.bfh.logisim.fpgaboardeditor.PullBehaviors;
import com.bfh.logisim.fpgagui.FPGAReport;
import com.bfh.logisim.fpgagui.MappableResourcesContainer;
import com.bfh.logisim.hdlgenerator.FileWriter;
import com.bfh.logisim.hdlgenerator.TickComponentHDLGeneratorFactory;
import com.bfh.logisim.hdlgenerator.ToplevelHDLGeneratorFactory;
import com.bfh.logisim.settings.Settings;
import com.cburch.logisim.proj.Projects;

public class AlteraDownload implements Runnable {

	public static void Download(Settings MySettings, String scriptPath,
			String ProjectPath, String SandboxPath, FPGAReport MyReporter) {
        AlteraDownload downloader = new AlteraDownload(MySettings, scriptPath,
                ProjectPath, SandboxPath, MyReporter);
        new Thread(downloader).start();
    }

    private JFrame panel;
    private JLabel LocText;
    private JProgressBar progres;
    private boolean stopRequested = false;

    private Settings MySettings;
    private String scriptPath;
    private String ProjectPath;
    private String SandboxPath;
    private FPGAReport MyReporter;

	public AlteraDownload(Settings MySettings, String scriptPath,
			String ProjectPath, String SandboxPath, FPGAReport MyReporter) {
        this.MySettings = MySettings;
        this.scriptPath = scriptPath;
        this.ProjectPath = ProjectPath;
        this.SandboxPath = SandboxPath;
        this.MyReporter = MyReporter;
		GridBagConstraints gbc = new GridBagConstraints();
		panel = new JFrame("Altera Downloading");
		panel.setResizable(false);
		panel.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        panel.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                cancel();
            }
        });
		GridBagLayout thisLayout = new GridBagLayout();
		panel.setLayout(thisLayout);
		// PointerInfo mouseloc = MouseInfo.getPointerInfo();
		// Point mlocation = mouseloc.getLocation();
		// panel.setLocation(mlocation.x, mlocation.y);
		LocText = new JLabel("Altera Downloader");
        LocText.setMinimumSize(new Dimension(600, 30));
        LocText.setPreferredSize(new Dimension(600, 30));
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(LocText, gbc);
		progres = new JProgressBar(0, 5);
		progres.setValue(1);
		progres.setStringPainted(true);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel.add(progres, gbc);
		panel.pack();
		panel.setLocation(Projects.getCenteredLoc(panel.getWidth(),
				panel.getHeight() * 4));
		panel.setVisible(true);
    }

    private void setStatus(String msg) {
        LocText.setText(msg);
        Rectangle labelRect = LocText.getBounds();
        labelRect.x = 0;
        labelRect.y = 0;
        LocText.repaint(labelRect);
    }

    private void setProgress(int val) {
		progres.setValue(val);
		Rectangle ProgRect = progres.getBounds();
		ProgRect.x = 0;
		ProgRect.y = 0;
		progres.repaint(ProgRect);
    }

    public void cancel() {
        setStatus("Cancelling... please wait");
        stopRequested = true;
        synchronized(lock) {
            if (altera != null) {
                altera.destroy();
            }
        }
    }

    public void run() {
        try {
            String fatal = download();
            if (fatal != null) {
                MyReporter.AddFatalError(fatal);
            }
        } catch (IOException e) {
            MyReporter.AddFatalError("Internal Error during Altera download");
        } catch (InterruptedException e) {
            MyReporter.AddFatalError("Internal Error during Altera download");
        }
        panel.setVisible(false);
        panel.dispose();
    }

    private Process altera = null;
    private Object lock = new Object();

    private boolean alteraCommand(int progid, String... args) throws IOException, InterruptedException {
		List<String> command = new ArrayList<String>();
        command.add(MySettings.GetAlteraToolPath() + File.separator + Settings.AlteraPrograms[progid]);
        for (String arg: args)
            command.add(arg);
        ProcessBuilder Altera1 = new ProcessBuilder(command);
        Altera1.directory(new File(SandboxPath));
        synchronized(lock) {
            altera = Altera1.start();
        }
        InputStream is = altera.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        MyReporter.ClsScr();
        while ((line = br.readLine()) != null) {
            MyReporter.print(line);
        }
        altera.waitFor();
        return (altera.exitValue() == 0);
    }

    private String download() throws IOException, InterruptedException {
        setStatus("Generating FPGA files and performing download; this may take a while");
		boolean SofFileExists = new File(SandboxPath
				+ ToplevelHDLGeneratorFactory.FPGAToplevelName + ".sof")
				.exists();

		setProgress(1);
        if (stopRequested)
            return null;
		if (!SofFileExists) {
            setStatus("Creating Project");
            if (!alteraCommand(0, "-t", scriptPath.replace(ProjectPath, ".." + File.separator) + "AlteraDownload.tcl"))
                return "Failed to Create a Quartus Project, cannot download";
		}

		setProgress(2);
        if (stopRequested)
            return null;
		if (!SofFileExists) {
            setStatus("Optimize Project");
            if (!alteraCommand(2, ToplevelHDLGeneratorFactory.FPGAToplevelName, "--optimize=area"))
                return "Failed to optimize (AREA) Project, cannot download";
		}

		setProgress(3);
        if (stopRequested)
            return null;
		if (!SofFileExists) {
            setStatus("Synthesizing and creating configuration file (this may take a while)");
            if (!alteraCommand(0, "--flow", "compile", ToplevelHDLGeneratorFactory.FPGAToplevelName))
                return "Failed to synthesize design and to create the configuration files, cannot download";
		}

		setStatus("Downloading");
        if (stopRequested)
            return null;
		Object[] options = { "Yes, download" };
		if (JOptionPane.showOptionDialog(
						progres,
						"Verify that your board is connected and you are ready to download.",
						"Ready to download ?", JOptionPane.YES_OPTION,
						JOptionPane.WARNING_MESSAGE, null, options, options[0]) == JOptionPane.CLOSED_OPTION) {
			MyReporter.AddSevereWarning("Download aborted.");
			return null;
		}

		setProgress(4);
        if (stopRequested)
            return null;
        // if there is no .sof generated, try with the .pof
        String bin;
        if (new File(SandboxPath
                + ToplevelHDLGeneratorFactory.FPGAToplevelName + ".sof")
                .exists()) {
            bin = "P;" + ToplevelHDLGeneratorFactory.FPGAToplevelName + ".sof";
        } else {
            bin = "P;" + ToplevelHDLGeneratorFactory.FPGAToplevelName + ".pof";
        }
        if (!alteraCommand(1, "-c", "usb-blaster", "-m", "jtag", "-o", bin))
            return "Failed to Download design; did you connect the board?";

        return null;
	}

	public static boolean GenerateQuartusScript(FPGAReport MyReporter,
			String ScriptPath, Netlist RootNetList,
			MappableResourcesContainer MapInfo, BoardInformation BoardInfo,
			ArrayList<String> Entities, ArrayList<String> Architectures,
			String HDLType) {
		File ScriptFile = FileWriter.GetFilePointer(ScriptPath,
				"AlteraDownload.tcl", MyReporter);
		if (ScriptFile == null) {
			ScriptFile = new File(ScriptPath + "AlteraDownload.tcl");
			return ScriptFile.exists();
		}
		String FileType = (HDLType.equals(Settings.VHDL)) ? "VHDL_FILE"
				: "VERILOG_FILE";
		ArrayList<String> Contents = new ArrayList<String>();
		Contents.add("# Load Quartus II Tcl Project package");
		Contents.add("package require ::quartus::project");
		Contents.add("");
		Contents.add("set need_to_close_project 0");
		Contents.add("set make_assignments 1");
		Contents.add("");
		Contents.add("# Check that the right project is open");
		Contents.add("if {[is_project_open]} {");
		Contents.add("    if {[string compare $quartus(project) \""
				+ ToplevelHDLGeneratorFactory.FPGAToplevelName + "\"]} {");
		Contents.add("        puts \"Project "
				+ ToplevelHDLGeneratorFactory.FPGAToplevelName
				+ " is not open\"");
		Contents.add("        set make_assignments 0");
		Contents.add("    }");
		Contents.add("} else {");
		Contents.add("    # Only open if not already open");
		Contents.add("    if {[project_exists "
				+ ToplevelHDLGeneratorFactory.FPGAToplevelName + "]} {");
		Contents.add("        project_open -revision "
				+ ToplevelHDLGeneratorFactory.FPGAToplevelName + " "
				+ ToplevelHDLGeneratorFactory.FPGAToplevelName);
		Contents.add("    } else {");
		Contents.add("        project_new -revision "
				+ ToplevelHDLGeneratorFactory.FPGAToplevelName + " "
				+ ToplevelHDLGeneratorFactory.FPGAToplevelName);
		Contents.add("    }");
		Contents.add("    set need_to_close_project 1");
		Contents.add("}");
		Contents.add("# Make assignments");
		Contents.add("if {$make_assignments} {");
		Contents.addAll(GetAlteraAssignments(BoardInfo));
		Contents.add("");
		Contents.add("    # Include all entities and gates");
		Contents.add("");
		for (int i = 0; i < Entities.size(); i++) {
			Contents.add("    set_global_assignment -name " + FileType + " \""
					+ Entities.get(i) + "\"");
		}
		for (int i = 0; i < Architectures.size(); i++) {
			Contents.add("    set_global_assignment -name " + FileType + " \""
					+ Architectures.get(i) + "\"");
		}
		Contents.add("");
		Contents.add("    # Map fpga_clk and ionets to fpga pins");
		if (RootNetList.NumberOfClockTrees() > 0) {
			Contents.add("    set_location_assignment "
					+ BoardInfo.fpga.getClockPinLocation() + " -to "
					+ TickComponentHDLGeneratorFactory.FPGAClock);
		}
		Contents.addAll(MapInfo.GetFPGAPinLocs(FPGAClass.VendorAltera));
		Contents.add("    # Commit assignments");
		Contents.add("    export_assignments");
		Contents.add("");
		Contents.add("    # Close project");
		Contents.add("    if {$need_to_close_project} {");
		Contents.add("        project_close");
		Contents.add("    }");
		Contents.add("}");
		return FileWriter.WriteContents(ScriptFile, Contents, MyReporter);
	}

	private static ArrayList<String> GetAlteraAssignments(
			BoardInformation CurrentBoard) {
		ArrayList<String> result = new ArrayList<String>();
		String Assignment = "    set_global_assignment -name ";
		result.add(Assignment + "FAMILY \"" + CurrentBoard.fpga.getTechnology()
				+ "\"");
		result.add(Assignment + "DEVICE " + CurrentBoard.fpga.getPart());
		String[] Package = CurrentBoard.fpga.getPackage().split(" ");
		result.add(Assignment + "DEVICE_FILTER_PACKAGE " + Package[0]);
		result.add(Assignment + "DEVICE_FILTER_PIN_COUNT " + Package[1]);
		if (CurrentBoard.fpga.getUnusedPinsBehavior() == PullBehaviors.Float) {
			result.add(Assignment
					+ "RESERVE_ALL_UNUSED_PINS \"AS INPUT TRI-STATED\"");
		}
		if (CurrentBoard.fpga.getUnusedPinsBehavior() == PullBehaviors.PullUp) {
			result.add(Assignment
					+ "RESERVE_ALL_UNUSED_PINS \"AS INPUT PULLUP\"");
		}
		if (CurrentBoard.fpga.getUnusedPinsBehavior() == PullBehaviors.PullDown) {
			result.add(Assignment
					+ "RESERVE_ALL_UNUSED_PINS \"AS INPUT PULLDOWN\"");
		}
		result.add(Assignment + "FMAX_REQUIREMENT \""
				+ GetClockFrequencyString(CurrentBoard) + "\"");
		result.add(Assignment + "RESERVE_NCEO_AFTER_CONFIGURATION \"USE AS REGULAR IO\"");
		result.add(Assignment + "CYCLONEII_RESERVE_NCEO_AFTER_CONFIGURATION \"USE AS REGULAR IO\"");
		return result;
	}

	private static String GetClockFrequencyString(BoardInformation CurrentBoard) {
		long clkfreq = CurrentBoard.fpga.getClockFrequency();
		if (clkfreq % 1000000 == 0) {
			clkfreq /= 1000000;
			return Long.toString(clkfreq) + " MHz ";
		} else if (clkfreq % 1000 == 0) {
			clkfreq /= 1000;
			return Long.toString(clkfreq) + " kHz ";
		}
		return Long.toString(clkfreq);
	}
}
