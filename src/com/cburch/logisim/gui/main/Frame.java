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
import static com.cburch.logisim.gui.main.Strings.S;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.cburch.draw.toolbar.Toolbar;
import com.cburch.draw.toolbar.ToolbarModel;
import com.cburch.hdl.HdlModel;
import com.cburch.logisim.Main;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.gui.appear.AppearanceView;
import com.cburch.logisim.gui.generic.AttrTable;
import com.cburch.logisim.gui.generic.AttrTableModel;
import com.cburch.logisim.gui.generic.BasicZoomModel;
import com.cburch.logisim.gui.generic.CanvasPane;
import com.cburch.logisim.gui.generic.CardPanel;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.gui.generic.RegTabContent;
import com.cburch.logisim.gui.generic.ZoomControl;
import com.cburch.logisim.gui.generic.ZoomModel;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.std.hdl.HdlContentView;
import com.cburch.logisim.std.hdl.VhdlContent;;
import com.cburch.logisim.std.hdl.VhdlSimulatorConsole;
import com.cburch.logisim.std.hdl.VhdlSimulatorListener;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.HorizontalSplitPane;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.VerticalSplitPane;

public class Frame extends LFrame.MainWindow implements LocaleListener {
  class MyProjectListener implements ProjectListener, LibraryListener,
        CircuitListener, PropertyChangeListener, ChangeListener {

    public void attributeListChanged(AttributeEvent e) {
    }

    @Override
    public void circuitChanged(CircuitEvent event) {
      if (event.getAction() == CircuitEvent.ACTION_SET_NAME) {
        computeTitle();
      }
    }

    private void enableSave() {
      boolean ok = getProject().isFileDirty();
      getRootPane().putClientProperty("windowModified", ok);
    }

    @Override
    public void libraryChanged(LibraryEvent e) {
      if (e.getAction() == LibraryEvent.SET_NAME) {
        computeTitle();
      } else if (e.getAction() == LibraryEvent.DIRTY_STATE) {
        enableSave();
      }
    }

    @Override
    public void projectChanged(ProjectEvent event) {
      int action = event.getAction();

      if (action == ProjectEvent.ACTION_SET_FILE) {
        computeTitle();
        project.setTool(project.getOptions().getToolbarData().getFirstTool());
        placeToolbar();
      } else if (action == ProjectEvent.ACTION_SET_STATE) {
        if (event.getData() instanceof CircuitState) {
          CircuitState state = (CircuitState)event.getData();
          if (state.getParentState() != null)
            topTab.setSelectedIndex(1); // sim explorer view
        }
      } else if (action == ProjectEvent.ACTION_SET_CURRENT) {
        if (event.getData() instanceof Circuit) {
          setEditorView(EDIT_LAYOUT);
          if (appearance != null)
            appearance.setCircuit(project, project.getCircuitState());
        } else if (event.getData() instanceof HdlModel) {
          setHdlEditorView((HdlModel)event.getData());
        }
        viewAttributes(null, null, true);
        computeTitle();
      } else if (action == ProjectEvent.ACTION_SET_TOOL) {
        if (attrTable == null)
          return; // for startup
        Tool oldTool = (Tool) event.getOldData();
        Tool newTool = (Tool) event.getData();
        if (!getEditorView().equals(EDIT_APPEARANCE))
          viewAttributes(oldTool, newTool, false);
      }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (AppPreferences.TOOLBAR_PLACEMENT.isSource(event)) {
        placeToolbar();
      }
    }

    @Override
    public void stateChanged(ChangeEvent event) {
      Object source = event.getSource();
      if (source == mainPanel) {
        firePropertyChange(EDITOR_VIEW, "???", getEditorView());
      }
    }
  }

  class MyWindowListener extends WindowAdapter {

    @Override
    public void windowClosing(WindowEvent e) {
      if (confirmClose(S.get("confirmCloseTitle"))) {
        layoutCanvas.closeCanvas();
        // frameClosing() is needed to record window position, which may be used
        // in savePreferences() later. Normally Projects has a listener that
        // invokes frameClosing() directly, but that may not be called before
        // this listener, depending on AWT ordering of listeners, and the
        // location must be recorded before the dispose call.
        Projects.frameClosing(Frame.this);
        Frame.this.dispose();
      }
    }

    @Override
    public void windowOpened(WindowEvent e) {
      layoutCanvas.computeSize(true);
    }
  }

  private class VhdlSimState extends JPanel implements VhdlSimulatorListener {

    private static final long serialVersionUID = 1L;
    Ellipse2D.Double circle;
    Color color;
    private int margin = 5;

    public VhdlSimState() {
      int radius = 15;
      circle = new Ellipse2D.Double(margin, margin, radius, radius);
      setOpaque(false);
      color = Color.GRAY;
      this.setBorder(new EmptyBorder(margin, margin, margin, margin));
    }

    public Dimension getPreferredSize() {
      Rectangle bounds = circle.getBounds();
      return new Dimension(bounds.width + 2 * margin, bounds.height + 2
          * margin);
    }

    public void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D) g;
      g2.setColor(color);
      g2.fill(circle);
    }

    @Override
    public void stateChanged() {

      switch (project.getVhdlSimulator().getState()) {
      case DISABLED:
        color = Color.GRAY;
        break;
      case ENABLED:
        color = Color.RED;
        break;
      case STARTING:
        color = Color.ORANGE;
        break;
      case RUNNING:
        color = new Color(40, 180, 40);
        break;
      }

      this.repaint();

      // this.setText("VHDL Sim : " +
      // project.getSimulator().getCircuitState().getVhdlSimulator().getState());
    }

  }

  private static Point getInitialLocation() {
    String s = AppPreferences.WINDOW_LOCATION.get();
    if (s == null) {
      return null;
    }
    int comma = s.indexOf(',');
    if (comma < 0) {
      return null;
    }
    try {
      int x = Integer.parseInt(s.substring(0, comma));
      int y = Integer.parseInt(s.substring(comma + 1));
      while (isProjectFrameAt(x, y)) {
        x += 20;
        y += 20;
      }
      Rectangle desired = new Rectangle(x, y, 50, 50);

      int gcBestSize = 0;
      Point gcBestPoint = null;
      GraphicsEnvironment ge;
      ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      for (GraphicsDevice gd : ge.getScreenDevices()) {
        for (GraphicsConfiguration gc : gd.getConfigurations()) {
          Rectangle gcBounds = gc.getBounds();
          if (gcBounds.intersects(desired)) {
            Rectangle inter = gcBounds.intersection(desired);
            int size = inter.width * inter.height;
            if (size > gcBestSize) {
              gcBestSize = size;
              int x2 = Math.max(
                  gcBounds.x,
                  Math.min(inter.x, inter.x + inter.width - 50));
              int y2 = Math.max(
                  gcBounds.y,
                  Math.min(inter.y, inter.y + inter.height - 50));
              gcBestPoint = new Point(x2, y2);
            }
          }
        }
      }
      if (gcBestPoint != null) {
        if (isProjectFrameAt(gcBestPoint.x, gcBestPoint.y)) {
          gcBestPoint = null;
        }
      }
      return gcBestPoint;
    } catch (Throwable t) {
      return null;
    }
  }

  private static boolean isProjectFrameAt(int x, int y) {
    for (Project current : Projects.getOpenProjects()) {
      Frame frame = current.getFrame();
      if (frame != null) {
        Point loc = frame.getLocationOnScreen();
        int d = Math.abs(loc.x - x) + Math.abs(loc.y - y);
        if (d <= 3) {
          return true;
        }
      }
    }
    return false;
  }

  private static final long serialVersionUID = 1L;
  public static final String EDITOR_VIEW = "editorView";
  public static final String EXPLORER_VIEW = "explorerView";
  public static final String EDIT_LAYOUT = "layout";
  public static final String EDIT_APPEARANCE = "appearance";
  public static final String EDIT_HDL = "hdl";
  private static final double[] ZOOM_OPTIONS = {
    20, 40, 60, 70, 80, 90, 100, 120, 150, 200, 250, 300, 400 };
  private MyProjectListener myProjectListener = new MyProjectListener();
  // GUI elements shared between views
  private MainMenuListener menuListener;
  private Toolbar toolbar;
  private HorizontalSplitPane leftRegion, rightRegion, editRegion;
  private VerticalSplitPane mainRegion;
  private JPanel rightPanel;
  private CardPanel mainPanel;
  // left-side elements
  private JTabbedPane topTab, bottomTab;
  private Toolbox toolbox;
  private SimulationExplorer simExplorer;
  private AttrTable attrTable;
  private RegTabContent regPanel;
  private VhdlSimState vhdlSimState;
  private ZoomControl zoom;
  // for the Layout view
  private LayoutToolbarModel layoutToolbarModel;
  private Canvas layoutCanvas;
  private VhdlSimulatorConsole vhdlSimulatorConsole;
  private HdlContentView hdlEditor;

  private ZoomModel layoutZoomModel;

  private LayoutEditHandler layoutEditHandler;

  private AttrTableSelectionModel attrTableSelectionModel;

  // for the Appearance view
  private AppearanceView appearance;

  // for VHDL Editor
  private ToolbarModel hdlToolbarModel;

  private Double lastFraction = AppPreferences.WINDOW_RIGHT_SPLIT.get();

  public Frame(Project project) {
    super(project);

    setBackground(Color.white);
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    addWindowListener(new MyWindowListener());

    project.addProjectWeakListener(null, myProjectListener);
    project.addLibraryWeakListener(/*null,*/ myProjectListener);
    project.addCircuitWeakListener(/*null,*/ myProjectListener);
    computeTitle();

    // set up elements for the Layout view
    layoutToolbarModel = new LayoutToolbarModel(this, project);
    layoutCanvas = new Canvas(project);
    layoutZoomModel = new BasicZoomModel(AppPreferences.LAYOUT_SHOW_GRID,
        AppPreferences.LAYOUT_ZOOM, ZOOM_OPTIONS);

    layoutCanvas.getGridPainter().setZoomModel(layoutZoomModel);
    layoutEditHandler = new LayoutEditHandler(this);
    attrTableSelectionModel = new AttrTableSelectionModel(project, this);

    // set up menu bar and toolbar
    menuListener = new MainMenuListener(this, menubar);
    menuListener.setEditHandler(layoutEditHandler);
    toolbar = new Toolbar(layoutToolbarModel);

    // set up the left-side components
    toolbox = new Toolbox(project, this, menuListener);
    simExplorer = new SimulationExplorer(project, menuListener);

    bottomTab = new JTabbedPane();
    bottomTab.setFont(new Font("Dialog", Font.BOLD, 9));
    bottomTab.addTab("Properties", attrTable = new AttrTable(this));
    bottomTab.addTab("State", regPanel = new RegTabContent(this));

    vhdlSimState = new VhdlSimState();
    vhdlSimState.stateChanged();
    project.getVhdlSimulator().addVhdlSimStateListener(vhdlSimState);

    zoom = new ZoomControl(layoutZoomModel, layoutCanvas);

    // set up the central area
    CanvasPane canvasPane = new CanvasPane(layoutCanvas);
    JPanel circEditor = new JPanel(new BorderLayout());
    canvasPane.setZoomModel(layoutZoomModel);
    mainPanel = new CardPanel();
    mainPanel.addView(EDIT_LAYOUT, canvasPane);
    mainPanel.setView(EDIT_LAYOUT);
    circEditor.add(mainPanel, BorderLayout.CENTER);

    // set up the contents, split down the middle, with the canvas
    // on the right and a split pane on the left containing the
    // explorer and attribute values.
    JPanel explPanel = new JPanel(new BorderLayout());
    explPanel.add(toolbox, BorderLayout.CENTER);

    JPanel simPanel = new JPanel(new BorderLayout());
    simPanel.add(simExplorer, BorderLayout.CENTER);

    topTab = new JTabbedPane();
    topTab.setFont(new Font("Dialog", Font.BOLD, 9));
    topTab.add("Design", explPanel); // index=0 
    topTab.add("Simulate", simPanel); // index=1 (see ACTION_SET_STATE below)
    // FIXME: on mac, JTabbedPane leaves too much whitespace here

    JPanel attrFooter = new JPanel(new BorderLayout());
    attrFooter.add(zoom);

    JPanel bottomTabAndZoom = new JPanel(new BorderLayout(0, 4));
    bottomTabAndZoom.add(bottomTab, BorderLayout.CENTER);
    bottomTabAndZoom.add(attrFooter, BorderLayout.SOUTH);

    leftRegion = new HorizontalSplitPane(topTab, bottomTabAndZoom,
        AppPreferences.WINDOW_LEFT_SPLIT.get());

    hdlEditor = new HdlContentView(project);
    vhdlSimulatorConsole = new VhdlSimulatorConsole(project);
    editRegion = new HorizontalSplitPane(circEditor, hdlEditor, 1.0);
    rightRegion = new HorizontalSplitPane(editRegion, vhdlSimulatorConsole, 1.0); // ?

    rightPanel = new JPanel(new BorderLayout()); // panel where toolbar is attached
    rightPanel.add(rightRegion, BorderLayout.CENTER);

    mainRegion = new VerticalSplitPane(leftRegion, rightPanel,
        AppPreferences.WINDOW_MAIN_SPLIT.get());

    getContentPane().add(mainRegion, BorderLayout.CENTER);

    computeTitle();

    this.setSize(AppPreferences.WINDOW_WIDTH.get(),
        AppPreferences.WINDOW_HEIGHT.get());
    Point prefPoint = getInitialLocation();
    if (prefPoint != null) {
      this.setLocation(prefPoint);
    }
    this.setExtendedState(AppPreferences.WINDOW_STATE.get());

    menuListener.register(mainPanel);
    KeyboardToolSelection.register(toolbar);

    project.setFrame(this);
    if (project.getTool() == null) {
      project.setTool(project.getOptions().getToolbarData().getFirstTool());
    }
    mainPanel.addChangeListener(myProjectListener);
    AppPreferences.TOOLBAR_PLACEMENT
        .addPropertyChangeListener(myProjectListener);
    placeToolbar();

    LocaleManager.addLocaleListener(this);
    toolbox.updateStructure();
  }

  private void computeTitle() {
    String s;
    Circuit circuit = project.getCurrentCircuit();
    HdlModel hdl = project.getCurrentHdl();
    String name = project.getLogisimFile().getName();
    if (circuit != null)
      s = S.fmt("titleCircFileKnown", circuit.getName(), name);
    else if (hdl != null)
      s = S.fmt("titleCircFileKnown", hdl.getName(), name);
    else
      s = S.fmt("titleFileKnown", name);
    this.setTitle(s + " (v " + Main.VERSION_NAME + ")");
    myProjectListener.enableSave();
  }

  public boolean confirmClose() {
    return confirmClose(S.get("confirmCloseTitle"));
  }

  // returns true if user is OK with proceeding
  public boolean confirmClose(String title) {
    String message = S.fmt("confirmDiscardMessage", project.getLogisimFile().getName());

    if (!project.isFileDirty()) {
      return true;
    }
    toFront();
    String[] options = {
      S.get("saveOption"),
      S.get("discardOption"),
      S.get("cancelOption") };
    int result = JOptionPane.showOptionDialog(this, message, title, 0,
        JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
    boolean ret;
    if (result == 0) {
      ret = ProjectActions.doSave(project);
    } else if (result == 1) {
      dispose(); // close the current project
      ret = true;
    } else {
      ret = false;
    }

    return ret;
  }

  public Canvas getCanvas() {
    return layoutCanvas;
  }

  public String getEditorView() {
    return (getHdlEditorView() != null ? EDIT_HDL : mainPanel.getView());
  }

  public VhdlSimulatorConsole getVhdlSimulatorConsole() {
    return vhdlSimulatorConsole;
  }

  public ZoomModel getZoomModel() {
    return layoutZoomModel;
  }

  @Override
  public void localeChanged() {
    computeTitle();
  }

  private void placeToolbar() {
    String loc = AppPreferences.TOOLBAR_PLACEMENT.get();
    Container contents = getContentPane();
    contents.remove(toolbar);
    rightPanel.remove(toolbar);

    if (AppPreferences.TOOLBAR_DOWN_MIDDLE.equals(loc)) {
      toolbar.setOrientation(Toolbar.VERTICAL);
      rightPanel.add(toolbar, BorderLayout.WEST);
    } else if (Direction.NORTH.toString().equals(loc)) {
      toolbar.setOrientation(Toolbar.HORIZONTAL);
      rightPanel.add(toolbar, BorderLayout.NORTH);
    } else if (Direction.SOUTH.toString().equals(loc)) {
      toolbar.setOrientation(Toolbar.HORIZONTAL);
      rightPanel.add(toolbar, BorderLayout.SOUTH);
    } else if (Direction.EAST.toString().equals(loc)) {
      toolbar.setOrientation(Toolbar.VERTICAL);
      rightPanel.add(toolbar, BorderLayout.EAST);
    } else if (Direction.WEST.toString().equals(loc)) {
      toolbar.setOrientation(Toolbar.VERTICAL);
      contents.add(toolbar, BorderLayout.WEST);
    }
    contents.validate();
  }

  public void savePreferences() {
    AppPreferences.TICK_FREQUENCY.set(project.getSimulator().getTickFrequency());
    AppPreferences.LAYOUT_SHOW_GRID.set(layoutZoomModel.getShowGrid());
    AppPreferences.LAYOUT_ZOOM.set(layoutZoomModel.getZoomFactor());
    if (appearance != null) {
      ZoomModel aZoom = appearance.getZoomModel();
      AppPreferences.APPEARANCE_SHOW_GRID.set(aZoom.getShowGrid());
      AppPreferences.APPEARANCE_ZOOM.set(aZoom.getZoomFactor());
    }
    int state = getExtendedState() & ~JFrame.ICONIFIED;
    AppPreferences.WINDOW_STATE.set(state);
    Dimension dim = getSize();
    AppPreferences.WINDOW_WIDTH.set(dim.width);
    AppPreferences.WINDOW_HEIGHT.set(dim.height);
    Point loc;
    try {
      loc = getLocationOnScreen();
    } catch (IllegalComponentStateException e) {
      loc = Projects.getLocation(this);
    }
    if (loc != null) {
      AppPreferences.WINDOW_LOCATION.set(loc.x + "," + loc.y);
    }
    AppPreferences.WINDOW_LEFT_SPLIT.set(leftRegion.getFraction());

    if (rightRegion.getFraction() < 1.0)
      AppPreferences.WINDOW_RIGHT_SPLIT.set(rightRegion.getFraction());
    AppPreferences.WINDOW_MAIN_SPLIT.set(mainRegion.getFraction());
    AppPreferences.DIALOG_DIRECTORY.set(JFileChoosers.getCurrentDirectory());
  }

  void setAttrTableModel(AttrTableModel value) {
    attrTable.setAttrTableModel(value);
    if (value instanceof AttrTableToolModel) {
      Tool tool = ((AttrTableToolModel) value).getTool();
      toolbox.setHaloedTool(tool);
      layoutToolbarModel.setHaloedTool(tool);
    } else {
      toolbox.setHaloedTool(null);
      layoutToolbarModel.setHaloedTool(null);
    }
    if (value instanceof AttrTableComponentModel) {
      Circuit circ = ((AttrTableComponentModel) value).getCircuit();
      Component comp = ((AttrTableComponentModel) value).getComponent();
      layoutCanvas.setHaloedComponent(circ, comp);
    } else {
      layoutCanvas.setHaloedComponent(null, null);
    }
  }

  public void setEditorView(String view) {
    String curView = mainPanel.getView();
    if (hdlEditor.getHdlModel() == null && curView.equals(view))
      return;
    editRegion.setFraction(1.0);
    hdlEditor.setHdlModel(null);

    if (view.equals(EDIT_APPEARANCE)) { // appearance view
      AppearanceView app = appearance;
      if (app == null) {
        app = new AppearanceView();
        app.setCircuit(project, project.getCircuitState());
        mainPanel.addView(EDIT_APPEARANCE, app.getCanvasPane());
        appearance = app;
      }
      toolbar.setToolbarModel(app.getToolbarModel());
      app.getAttrTableDrawManager(attrTable).attributesSelected();
      zoom.setZoomModel(app.getZoomModel());
      menuListener.setEditHandler(app.getEditHandler());
      mainPanel.setView(view);
      app.getCanvas().requestFocus();
    } else { // layout view
      toolbar.setToolbarModel(layoutToolbarModel);
      zoom.setZoomModel(layoutZoomModel);
      menuListener.setEditHandler(layoutEditHandler);
      viewAttributes(null, project.getTool(), true);
      mainPanel.setView(view);
      layoutCanvas.requestFocus();
    }
  }

  public void setVhdlSimulatorConsoleStatus(boolean visible) {
    if (visible) {
      rightRegion.setFraction(lastFraction);
    } else {
      lastFraction = rightRegion.getFraction();
      rightRegion.setFraction(1);
    }
  }

  private void setHdlEditorView(HdlModel hdl) {
    hdlEditor.setHdlModel(hdl);
    editRegion.setFraction(0.0);

    toolbar.setToolbarModel(hdlEditor.getToolbarModel());
    // toolbar.setToolbarModel(app.getToolbarModel());
    // app.getAttrTableDrawManager(attrTable).attributesSelected();
    // zoom.setZoomModel(app.getZoomModel());
    // menuListener.setEditHandler(app.getEditHandler());
    // mainPanel.setView(view);
    // app.getCanvas().requestFocus();
  }

  public HdlModel getHdlEditorView() {
    return hdlEditor.getHdlModel();
  }

  public void viewAttributes(Library lib) {
    setAttrTableModel(new AttrTableLibraryModel(project, lib));
  }

  void viewAttributes(Tool newTool) {
    viewAttributes(null, newTool, false);
  }

  // private void viewAttributes(Tool newTool, boolean force) {
  //   viewAttributes(null, newTool, force);
  // }

  private void viewAttributes(Tool oldTool, Tool newTool, boolean force) {
    if (newTool == null && !force)
      return;
    AttributeSet newAttrs = newTool == null ? null : newTool.getAttributeSet(layoutCanvas);
    if (newAttrs == null) {
      // AttrTableModel oldModel = attrTable.getAttrTableModel();
      // boolean same = oldModel instanceof AttrTableToolModel
      //     && ((AttrTableToolModel) oldModel).getTool() == oldTool;
      // if (!force && !same && !(oldModel instanceof AttrTableCircuitModel)) {
      //   System.out.println("skip view attr");
      //   System.out.printf("oldTool = %s\n", oldTool);
      //   System.out.printf("newTool = %s\n", newTool);
      //   System.out.printf("oldModel = %s\n", oldModel);
      //   if (oldModel instanceof AttrTableToolModel)
      //     System.out.printf("oldModel.tool = %s\n", ((AttrTableToolModel) oldModel).getTool());
      //   Thread.dumpStack();
      //   // return;
      // }
      Circuit circ = project.getCurrentCircuit();
      HdlModel hdl = project.getCurrentHdl();
      if (circ != null)
        setAttrTableModel(new AttrTableCircuitModel(project, circ));
      else if (hdl != null && hdl instanceof VhdlContent)
        setAttrTableModel(new AttrTableVhdlModel(project, (VhdlContent)hdl));
      else if (force)
        setAttrTableModel(null);
    } else if (newAttrs instanceof SelectionAttributes) {
      setAttrTableModel(attrTableSelectionModel);
    } else {
      setAttrTableModel(new AttrTableToolModel(project, newTool));
    }
  }

  public void viewComponentAttributes(Circuit circ, Component comp) {
    if (comp == null) {
      setAttrTableModel(null);
    } else {
      setAttrTableModel(new AttrTableComponentModel(project, circ, comp));
    }
  }

  public void clearExplorerSelection() {
    if (toolbox != null)
      toolbox.clearExplorerSelection();
  }

  public Library getSelectedToolboxLibrary() {
    return toolbox == null ? null : toolbox.getSelectedLibrary();
  }

  public Tool getSelectedToolboxTool() {
    return toolbox == null ? null : toolbox.getSelectedTool();
  }

  public void fireLibrarySelected() {
    layoutEditHandler.computeEnabled();
  }

  public void revealInExplorer(List<Library> libPath, Object obj) {
    if (toolbox != null)
      toolbox.reveal(libPath, obj);
  }

  public HdlContentView getHdlContentView() {
    return hdlEditor;
  }

}
