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

package com.cburch.logisim.circuit.appear;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cburch.draw.model.CanvasModelEvent;
import com.cburch.draw.model.CanvasModelListener;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Drawing;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.util.EventSourceWeakSupport;

import com.cburch.logisim.circuit.appear.DynamicElement;

public class CircuitAppearance extends Drawing {
  private class MyListener implements CanvasModelListener {
    public void modelChanged(CanvasModelEvent event) {
      if (!suppressRecompute) {
        setDefaultAppearance(false);
        fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
      }
    }
  }

  private Circuit circuit;
  private EventSourceWeakSupport<CircuitAppearanceListener> listeners;
  private PortManager portManager;
  private CircuitPins circuitPins;
  private MyListener myListener;
  private boolean isDefault;
  private boolean suppressRecompute;

  public CircuitAppearance(Circuit circuit) {
    this.circuit = circuit;
    listeners = new EventSourceWeakSupport<CircuitAppearanceListener>();
    portManager = new PortManager(this);
    circuitPins = new CircuitPins(portManager);
    myListener = new MyListener();
    suppressRecompute = false;
    addCanvasModelWeakListener(null, myListener);
    setDefaultAppearance(true);
  }

  public void addCircuitAppearanceWeakListener(Object owner, CircuitAppearanceListener l) { listeners.add(owner, l); }
  public void removeCircuitAppearanceWeakListener(Object owner, CircuitAppearanceListener l) { listeners.remove(owner, l); }

  @Override
  public void addObjects(int index, Collection<? extends CanvasObject> shapes) {
    super.addObjects(index, shapes);
    checkToFirePortsChanged(shapes);
  }

  @Override
  public void addObjects(Map<? extends CanvasObject, Integer> shapes) {
    super.addObjects(shapes);
    checkToFirePortsChanged(shapes.keySet());
  }

  private boolean affectsPorts(Collection<? extends CanvasObject> shapes) {
    for (CanvasObject o : shapes) {
      if (o instanceof AppearanceElement) {
        return true;
      }
    }
    return false;
  }

  private void checkToFirePortsChanged(
      Collection<? extends CanvasObject> shapes) {
    if (affectsPorts(shapes)) {
      recomputePorts();
    }
  }

  /**
   * Code taken from Cornell's version of Logisim:
   * http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  public boolean contains(Location loc) {
    Location query;
    AppearanceAnchor anchor = findAnchor();

    if (anchor == null) {
      query = loc;
    } else {
      Location anchorLoc = anchor.getLocation();
      query = loc.translate(anchorLoc.getX(), anchorLoc.getY());
    }

    for (CanvasObject o : getObjectsFromBottom()) {
      if (!(o instanceof AppearanceElement) && o.contains(query, true)) {
        return true;
      }
    }

    return false;
  }

  private AppearanceAnchor findAnchor() {
    for (CanvasObject shape : getObjectsFromBottom()) {
      if (shape instanceof AppearanceAnchor) {
        return (AppearanceAnchor) shape;
      }
    }
    return null;
  }

  private Location findAnchorLocation() {
    AppearanceAnchor anchor = findAnchor();
    if (anchor == null) {
      return Location.create(100, 100);
    } else {
      return anchor.getLocation();
    }
  }

  void fireCircuitAppearanceChanged(int affected) {
    CircuitAppearanceEvent event;
    event = new CircuitAppearanceEvent(circuit, affected);
    for (CircuitAppearanceListener listener : listeners) {
      listener.circuitAppearanceChanged(event);
    }
  }

  public Bounds getAbsoluteBounds() {
    return getBounds(false);
  }

  private Bounds getBounds(boolean relativeToAnchor) {
    Bounds ret = null;
    Location offset = null;
    for (CanvasObject o : getObjectsFromBottom()) {
      if (o instanceof AppearanceElement) {
        Location loc = ((AppearanceElement) o).getLocation();
        if (o instanceof AppearanceAnchor) {
          offset = loc;
        }
        if (ret == null) {
          ret = Bounds.create(loc);
        } else {
          ret = ret.add(loc);
        }
      } else {
        if (ret == null) {
          ret = o.getBounds();
        } else {
          ret = ret.add(o.getBounds());
        }
      }
    }
    if (ret == null) {
      return Bounds.EMPTY_BOUNDS;
    } else if (relativeToAnchor && offset != null) {
      return ret.translate(-offset.getX(), -offset.getY());
    } else {
      return ret;
    }
  }

  public CircuitPins getCircuitPins() {
    return circuitPins;
  }

  public AttributeOption getCircuitAppearance() {
    if (circuit == null || circuit.getStaticAttributes() == null)
      return null;
    else
      return circuit.getStaticAttributes().getValue(CircuitAttributes.APPEARANCE_ATTR);
  }

  public String getCircuitName() {
    if (circuit == null || circuit.getStaticAttributes() == null)
      return null;
    else
      return circuit.getStaticAttributes().getValue(CircuitAttributes.NAME_ATTR);
  }

  public Direction getFacing() {
    AppearanceAnchor anchor = findAnchor();
    if (anchor == null) {
      return Direction.EAST;
    } else {
      return anchor.getFacing();
    }
  }

  public Bounds getOffsetBounds() {
    return getBounds(true);
  }

  public SortedMap<Location, Instance> getPortOffsets(Direction facing) {
    Location anchor = null;
    Direction defaultFacing = Direction.EAST;
    List<AppearancePort> ports = new ArrayList<AppearancePort>();
    for (CanvasObject shape : getObjectsFromBottom()) {
      if (shape instanceof AppearancePort) {
        ports.add((AppearancePort) shape);
      } else if (shape instanceof AppearanceAnchor) {
        AppearanceAnchor o = (AppearanceAnchor) shape;
        anchor = o.getLocation();
        defaultFacing = o.getFacing();
      }
    }

    SortedMap<Location, Instance> ret = new TreeMap<Location, Instance>();
    for (AppearancePort port : ports) {
      Location loc = port.getLocation();
      if (anchor != null) {
        loc = loc.translate(-anchor.getX(), -anchor.getY());
      }
      if (facing != defaultFacing) {
        loc = loc.rotate(defaultFacing, facing, 0, 0);
      }
      ret.put(loc, port.getPin());
    }
    return ret;
  }

  public boolean isDefaultAppearance() {
    return isDefault;
  }

  public void paintSubcircuit(InstancePainter painter, Graphics g, Direction facing) {
    Direction defaultFacing = getFacing();
    double rotate = 0.0;
    if (facing != defaultFacing) {
      rotate = defaultFacing.toRadians() - facing.toRadians();
      ((Graphics2D) g).rotate(rotate);
    }
    Location offset = findAnchorLocation();
    g.translate(-offset.getX(), -offset.getY());
    CircuitState state = null;
    if (painter.getShowState()) {
      try { state = (CircuitState)painter.getData(); }
      catch (UnsupportedOperationException e) { }
    }
    for (CanvasObject shape : getObjectsFromBottom()) {
      if (!(shape instanceof AppearanceElement)) {
        DynamicCondition dyn = shape.getDynamicCondition();
        if (dyn != null && !dyn.evaluateCondition(state))
          continue;
        Graphics dup = g.create();
        if (shape instanceof DynamicElement)
          ((DynamicElement)shape).paintDynamic(dup, state);
        else
          shape.paint(dup, null);
        dup.dispose();
      }
    }
    g.translate(offset.getX(), offset.getY());
    if (rotate != 0.0) {
      ((Graphics2D) g).rotate(-rotate);
    }
  }

  public void recomputeDefaultAppearance() {
    if (isDefault) {
      List<CanvasObject> shapes;
      shapes = DefaultAppearance.build(circuitPins.getPins(),
          getCircuitName(), getCircuitAppearance());
      setObjectsForce(shapes);
    }
  }

  void recomputePorts() {
    if (isDefault) {
      recomputeDefaultAppearance();
    } else {
      fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
    }
  }

  @Override
  public void removeObjects(Collection<? extends CanvasObject> shapes) {
    super.removeObjects(shapes);
    checkToFirePortsChanged(shapes);
  }

  public void fixDynamicElement(InstanceComponent c, InstanceComponent r) {
    ArrayList<CanvasObject> toRemove = new ArrayList<>();
    ArrayList<DynamicElement.Path> toPatch = new ArrayList<>();
    ArrayList<CanvasObject> toRemoveCondition = new ArrayList<>();
    for (CanvasObject shape : getObjectsFromBottom()) {
      boolean removed = false;
      if (shape instanceof DynamicElement) {
        DynamicElement.Path path = ((DynamicElement)shape).getPath();
        if (path.contains(c)) {
          if (r == null) {
            toRemove.add(shape);
            removed = true;
          } else {
            toPatch.add(path);
          }
        }
      }
      if (!removed) {
        DynamicCondition dyn = shape.getDynamicCondition();
        DynamicElement.Path path = dyn == null ? null : dyn.getPath();
        if (path != null && path.contains(c)) {
          if (r == null) {
            toRemoveCondition.add(shape);
          } else {
            toPatch.add(path);
          }
        } 
      }
    }
    if (toRemove.isEmpty() && toRemoveCondition.isEmpty() && toPatch.isEmpty())
      return;
    boolean oldSuppress = suppressRecompute;
    try {
      suppressRecompute = true;
      for (CanvasObject shape : toRemoveCondition) {
        shape.clearDynamicCondition();
      }
      removeObjects(toRemove);
      for (DynamicElement.Path path : toPatch) {
        path.replaceInstance(c, r);
      }
      recomputeDefaultAppearance();
    } finally {
      suppressRecompute = oldSuppress;
    }
    fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
  }

  void replaceAutomatically(List<AppearancePort> removes,
      List<AppearancePort> adds) {
    // this should be called only when substituting ports via PortManager
    boolean oldSuppress = suppressRecompute;
    try {
      suppressRecompute = true;
      removeObjects(removes);
      addObjects(getObjectsFromBottom().size() - 1, adds);
      recomputeDefaultAppearance();
    } finally {
      suppressRecompute = oldSuppress;
    }
    fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
  }

  public void setDefaultAppearance(boolean value) {
    if (isDefault != value) {
      isDefault = value;
      if (value) {
        recomputeDefaultAppearance();
      }
    }
  }

  public void setObjectsForce(List<? extends CanvasObject> shapesBase) {
    // Outside the appearance editor, the anchor is not drawn at all, and ports
    // are always drawn last (as the top layer) by the simulation rendering
    // code. So, the layer-order of ports and anchor within the shape lists does
    // not really matter much. However, we force the anchor to be in the last
    // position (top  layer), so it is easier to move, and we force the ports to
    // be next to last (near top layer), so it matches the simulation rendering.
    List<CanvasObject> shapes = new ArrayList<CanvasObject>(shapesBase);
    int end = shapes.size() - 1;
    int reserved = 0;
    for (int i = end; i >= 0; i--) {
      CanvasObject o = shapes.get(i);
      if (o instanceof AppearanceAnchor) {
        if (i != end) {
          shapes.remove(i);
          shapes.add(o);
        }
        reserved++;
      } else if (o instanceof AppearancePort) {
        if (i != end - reserved) {
          shapes.remove(i);
          shapes.add(end - reserved, o);
        }
        reserved++;
      }
    }

    try {
      suppressRecompute = true;
      super.removeObjects(new ArrayList<CanvasObject>(
            getObjectsFromBottom()));
      super.addObjects(0, shapes);
    } finally {
      suppressRecompute = false;
    }
    fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
  }

  @Override
  public void translateObjects(Collection<? extends CanvasObject> shapes,
      int dx, int dy) {
    super.translateObjects(shapes, dx, dy);
    checkToFirePortsChanged(shapes);
  }
}
