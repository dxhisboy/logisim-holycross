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

package com.cburch.logisim.std.gates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.VariableList;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.circuit.SplitterFactory;
import com.cburch.logisim.circuit.SplitterAttributes;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.Constant;
import com.cburch.logisim.std.wiring.Pin;

public class CircuitBuilder {

  // worst case inputs: 8x2 input pin and 12-bit splitter
  private static final int INPUT_START_X = 80 + 8*10 + 12*10;
  private static final int INPUT_START_Y = 80;
  private static final int INPUT_SPACE_Y = 50;

  private static final int OUTPUT_START_Y = 80;

  private static final int SPINE_START_X = INPUT_START_X + 20;
  private static final int SPINE_SPACE_X = 10;

  private static class CompareYs implements Comparator<Location> {
    public int compare(Location a, Location b) {
      return a.getY() - b.getY();
    }
  }

  private static class InputData {
    int startX;
    String[] names;
    HashMap<String, SingleInput> inputs = new HashMap<String, SingleInput>();

    InputData() {
    }

    int getSpineX(String input) {
      SingleInput data = inputs.get(input);
      return data.spineX;
    }

    int getStartX() {
      return startX;
    }

    void registerConnection(String input, Location loc) {
      SingleInput data = inputs.get(input);
      data.ys.add(loc);
    }
  }

  private static class Layout {
    // initialized by parent
    int y; // top edge relative to parent's top edge
    // (or edge corresponding to input)

    // initialized by self
    int width;
    int height;
    ComponentFactory factory;
    AttributeSet attrs;
    int outputY; // where output is relative to my top edge
    int subX; // where right edge of sublayouts should be relative to my left edge
    Layout[] subLayouts;
    String inputName; // for references directly to inputs

    Layout(int width, int height, int outputY, ComponentFactory factory,
        AttributeSet attrs, Layout[] subLayouts, int subX) {
      this.width = width;
      this.height = roundUp(height);
      this.outputY = outputY;
      this.factory = factory;
      this.attrs = attrs;
      this.subLayouts = subLayouts;
      this.subX = subX;
      this.inputName = null;
    }

    Layout(String inputName) {
      this(0, 0, 0, null, null, null, 0);
      this.inputName = inputName;
    }
  }

  private static class SingleInput {
    int spineX;
    ArrayList<Location> ys = new ArrayList<Location>();

    SingleInput(int spineX) {
      this.spineX = spineX;
    }
  }

  public static CircuitMutation build(Circuit destCirc, AnalyzerModel model,
      boolean twoInputs, boolean useNands) {
    CircuitMutation result = new CircuitMutation(destCirc);
    result.clear();

    Layout[] layouts = new Layout[model.getOutputs().bits.size()];
    int maxWidth = 0;
    for (int i = 0; i < layouts.length; i++) {
      String output = model.getOutputs().bits.get(i);
      Expression expr = model.getOutputExpressions()
          .getExpression(output);
      CircuitDetermination det = CircuitDetermination.create(expr);
      if (det != null) {
        if (twoInputs)
          det.convertToTwoInputs();
        if (useNands)
          det.convertToNands();
        det.repair();
        layouts[i] = layoutGates(det);
        maxWidth = Math.max(maxWidth, layouts[i].width);
      } else {
        layouts[i] = null;
      }
    }

    InputData inputData = computeInputData(model);
    int x = inputData.getStartX();
    int y = OUTPUT_START_Y;
    int outputX = x + maxWidth + 20;
    HashMap<String, Location> outlocs = new HashMap<>();
    for (int i = 0; i < layouts.length; i++) {
      String output = model.getOutputs().bits.get(i);
      Layout layout = layouts[i];
      Location outloc;
      int height;
      if (layout == null) {
        outloc = Location.create(outputX, y + 20);
        height = 40;
      } else {
        int dy = 0;
        if (layout.outputY < 20)
          dy = 20 - layout.outputY;
        height = Math.max(dy + layout.height, 40);
        outloc = Location.create(outputX, y + dy + layout.outputY);
        placeComponents(result, layouts[i], x, y + dy, inputData,
            outloc);
      }
      // placeOutput(result, output, outputName);
      outlocs.put(output, outloc);
      y += height + 10;
    }
    placeOutputs(result, model.getOutputs(), outlocs);
    placeInputs(result, model.getInputs(), inputData);
    return result;
  }

  private static class Bus {
    String name;
    Location loc[];
    Bus(String n, int width) { name = n; loc = new Location[width]; }
  }

  private static void placeOutputs(CircuitMutation result, VariableList outputs, HashMap<String, Location> outlocs) {
    for (Bus b: createBusses(outputs, outlocs)) {
      placePins(result, b, true);
    }
  }

  private static ArrayList<Bus> createBusses(VariableList vars, HashMap<String, Location> locs) {
    ArrayList<Bus> busses = new ArrayList<Bus>();
    for (Var v: vars.vars) {
      Bus bus = new Bus(v.name, v.width);
      for (int b = 0; b < v.width; b++)
        bus.loc[b] = locs.get(v.bitName(b));
      busses.add(bus);
    }
    return busses;
  }

  //
  // computeInputData
  //
  private static InputData computeInputData(AnalyzerModel model) {
    InputData ret = new InputData();
    VariableList inputs = model.getInputs();
    int spineX = SPINE_START_X;
    ret.names = new String[inputs.bits.size()];
    for (int i = 0; i < inputs.bits.size(); i++) {
      String name = inputs.bits.get(i);
      ret.names[i] = name;
      ret.inputs.put(name, new SingleInput(spineX));
      spineX += SPINE_SPACE_X;
    }
    ret.startX = spineX;
    return ret;
  }

  //
  // layoutGates
  //
  private static Layout layoutGates(CircuitDetermination det) {
    return layoutGatesSub(det);
  }

  private static Layout layoutGatesSub(CircuitDetermination det) {
    if (det instanceof CircuitDetermination.Input) {
      CircuitDetermination.Input input = (CircuitDetermination.Input) det;
      return new Layout(input.getName());
    } else if (det instanceof CircuitDetermination.Value) {
      CircuitDetermination.Value value = (CircuitDetermination.Value) det;
      ComponentFactory factory = Constant.FACTORY;
      AttributeSet attrs = factory.createAttributeSet();
      attrs.setAttr(Constant.ATTR_VALUE,
          Integer.valueOf(value.getValue()));
      Bounds bds = factory.getOffsetBounds(attrs);
      return new Layout(bds.getWidth(), bds.getHeight(), -bds.getY(),
          factory, attrs, new Layout[0], 0);
    }

    // We know det is a Gate. Determine sublayouts.
    CircuitDetermination.Gate gate = (CircuitDetermination.Gate) det;
    ComponentFactory factory = gate.getFactory();
    ArrayList<CircuitDetermination> inputs = gate.getInputs();

    // Handle a NOT implemented with a NAND as a special case
    if (gate.isNandNot()) {
      CircuitDetermination subDet = inputs.get(0);
      if (!(subDet instanceof CircuitDetermination.Input)) {
        Layout[] sub = new Layout[1];
        sub[0] = layoutGatesSub(subDet);
        sub[0].y = 0;

        AttributeSet attrs = factory.createAttributeSet();
        attrs.setAttr(GateAttributes.ATTR_SIZE, GateAttributes.SIZE_NARROW);
        attrs.setAttr(GateAttributes.ATTR_INPUTS, Integer.valueOf(2));

        // determine layout's width
        Bounds bds = factory.getOffsetBounds(attrs);
        int betweenWidth = 40;
        if (sub[0].width == 0)
          betweenWidth = 0;
        int width = sub[0].width + betweenWidth + bds.getWidth();

        // determine outputY and layout's height.
        int outputY = sub[0].y + sub[0].outputY;
        int height = sub[0].height;
        int minOutputY = roundUp(-bds.getY());
        if (minOutputY > outputY) {
          // we have to shift everything down because otherwise
          // the component will peek over the rectangle's top.
          int dy = minOutputY - outputY;
          sub[0].y += dy;
          height += dy;
          outputY += dy;
        }
        int minHeight = outputY + bds.getY() + bds.getHeight();
        if (minHeight > height)
          height = minHeight;

        // ok; create and return the layout.
        return new Layout(width, height, outputY, factory, attrs, sub,
            sub[0].width);
      }
    }

    Layout[] sub = new Layout[inputs.size()];
    boolean[] invert = new boolean[inputs.size()];
    int subWidth = 0; // maximum width of sublayouts
    int subHeight = 0; // total height of sublayouts
    for (int i = 0; i < sub.length; i++) {
      CircuitDetermination subcirc = inputs.get(i);
      if (sub.length > 1 && subcirc instanceof CircuitDetermination.Gate) {
        CircuitDetermination.Gate subgate = (CircuitDetermination.Gate)inputs.get(i);
        if (subgate.getFactory() == NotGate.FACTORY) {
          invert[i] = true;
          subcirc = subgate.getInputs().get(0);
        }
      }
      sub[i] = layoutGatesSub(subcirc);
      if (sub.length % 2 == 0 && i == (sub.length + 1) / 2
          && sub[i - 1].height + sub[i].height == 0) {
        // if there are an even number of inputs, then there is a
        // 20-tall gap between the middle two inputs. Ensure the two
        // middle inputs are at least 20 pixels apart.
        subHeight += 10;
      }
      sub[i].y = subHeight;
      subWidth = Math.max(subWidth, sub[i].width);
      subHeight += sub[i].height + 10;
    }
    subHeight -= 10;

    AttributeSet attrs = factory.createAttributeSet();
    if (factory == NotGate.FACTORY) {
      attrs.setAttr(NotGate.ATTR_SIZE, NotGate.SIZE_NARROW);
    } else {
      attrs.setAttr(GateAttributes.ATTR_SIZE, GateAttributes.SIZE_NARROW);
      int ins = sub.length;
      attrs.setAttr(GateAttributes.ATTR_INPUTS, Integer.valueOf(ins));
    }
    for (int i = 0; i < invert.length; i++) {
      if (!invert[i])
        continue;
      attrs.setAttr(new NegateAttribute(i, null), Boolean.TRUE);
    }

    // determine layout's width
    Bounds bds = factory.getOffsetBounds(attrs);
    int betweenWidth = 40 + 10 * (sub.length / 2 - 1);
    if (sub.length == 1)
      betweenWidth = 20;
    if (subWidth == 0)
      betweenWidth = 0;
    int width = subWidth + betweenWidth + bds.getWidth();

    // determine outputY and layout's height.
    int outputY;
    if (sub.length % 2 == 1) { // odd number - match the middle input
      int i = (sub.length - 1) / 2;
      outputY = sub[i].y + sub[i].outputY;
    } else { // even number - halfway between middle two inputs
      int i0 = (sub.length / 2) - 1;
      int i1 = (sub.length / 2);
      int o0 = sub[i0].y + sub[i0].outputY;
      int o1 = sub[i1].y + sub[i1].outputY;
      outputY = roundDown((o0 + o1) / 2);
    }
    int height = subHeight;
    int minOutputY = roundUp(-bds.getY());
    if (minOutputY > outputY) {
      // we have to shift everything down because otherwise
      // the component will peek over the rectangle's top.
      int dy = minOutputY - outputY;
      for (int i = 0; i < sub.length; i++)
        sub[i].y += dy;
      height += dy;
      outputY += dy;
    }
    int minHeight = outputY + bds.getY() + bds.getHeight();
    if (minHeight > height)
      height = minHeight;

    // ok; create and return the layout.
    return new Layout(width, height, outputY, factory, attrs, sub, subWidth);
  }

  //
  // placeComponents
  //
  /**
   * @param circuit
   *            the circuit where to place the components.
   * @param layout
   *            the layout specifying the gates to place there.
   * @param x
   *            the left edge of where the layout should be placed.
   * @param y
   *            the top edge of where the layout should be placed.
   * @param inputData
   *            information about how to reach inputs.
   * @param output
   *            a point to which the output should be connected.
   */
  private static void placeComponents(CircuitMutation result, Layout layout,
      int x, int y, InputData inputData, Location output) {
    if (layout.inputName != null) {
      int inputX = inputData.getSpineX(layout.inputName);
      Location input = Location.create(inputX, output.getY());
      inputData.registerConnection(layout.inputName, input);
      result.add(Wire.create(input, output));
      return;
    }

    Location compOutput = Location.create(x + layout.width, output.getY());
    Component parent = layout.factory.createComponent(compOutput,
        layout.attrs);
    result.add(parent);
    if (!compOutput.equals(output)) {
      result.add(Wire.create(compOutput, output));
    }

    // handle a NOT gate pattern implemented with NAND as a special case
    if (layout.factory == NandGate.FACTORY && layout.subLayouts.length == 1
        && layout.subLayouts[0].inputName == null) {
      Layout sub = layout.subLayouts[0];

      Location input0 = parent.getEnd(1).getLocation();
      Location input1 = parent.getEnd(2).getLocation();

      int midX = input0.getX() - 20;
      Location subOutput = Location.create(midX, output.getY());
      Location midInput0 = Location.create(midX, input0.getY());
      Location midInput1 = Location.create(midX, input1.getY());
      result.add(Wire.create(subOutput, midInput0));
      result.add(Wire.create(midInput0, input0));
      result.add(Wire.create(subOutput, midInput1));
      result.add(Wire.create(midInput1, input1));

      int subX = x + layout.subX - sub.width;
      placeComponents(result, sub, subX, y + sub.y, inputData, subOutput);
      return;
    }

    if (layout.subLayouts.length == parent.getEnds().size() - 2) {
      int index = layout.subLayouts.length / 2 + 1;
      Object factory = parent.getFactory();
      if (factory instanceof AbstractGate) {
        Value val = ((AbstractGate) factory).getIdentity();
        Integer valInt = Integer.valueOf(val.toIntValue());
        Location loc = parent.getEnd(index).getLocation();
        AttributeSet attrs = Constant.FACTORY.createAttributeSet();
        attrs.setAttr(Constant.ATTR_VALUE, valInt);
        result.add(Constant.FACTORY.createComponent(loc, attrs));
      }
    }

    for (int i = 0; i < layout.subLayouts.length; i++) {
      Layout sub = layout.subLayouts[i];

      int inputIndex = i + 1;
      Location subDest = parent.getEnd(inputIndex).getLocation();

      int subOutputY = y + sub.y + sub.outputY;
      if (sub.inputName != null) {
        int destY = subDest.getY();
        if (i == 0 && destY < subOutputY
            || i == layout.subLayouts.length - 1
            && destY > subOutputY) {
          subOutputY = destY;
        }
      }

      Location subOutput;
      int numSubs = layout.subLayouts.length;
      if (subOutputY == subDest.getY()) {
        subOutput = subDest;
      } else {
        int back;
        if (i < numSubs / 2) {
          if (subOutputY < subDest.getY()) { // bending upward
            back = i;
          } else {
            back = ((numSubs - 1) / 2) - i;
          }
        } else {
          if (subOutputY > subDest.getY()) { // bending downward
            back = numSubs - 1 - i;
          } else {
            back = i - (numSubs / 2);
          }
        }
        int subOutputX = subDest.getX() - 20 - 10 * back;
        if (layout.attrs.getValue(new NegateAttribute(i, null)) == Boolean.TRUE)
          subOutputX += 10;
        subOutput = Location.create(subOutputX, subOutputY);
        Location mid = Location.create(subOutputX, subDest.getY());
        result.add(Wire.create(subOutput, mid));
        result.add(Wire.create(mid, subDest));
      }

      int subX = x + layout.subX - sub.width;
      int subY = y + sub.y;
      placeComponents(result, sub, subX, subY, inputData, subOutput);
    }
  }

  //
  // placeInputs
  //
  private static void placeInputs(CircuitMutation result, VariableList inputs, InputData inputData) {
    ArrayList<Location> forbiddenYs = new ArrayList<Location>();
    Comparator<Location> compareYs = new CompareYs();
    int curX = INPUT_START_X;
    int curY = INPUT_START_Y;
    HashMap<String, Location> inlocs = new HashMap<>();
    for (int i = 0; i < inputData.names.length; i++) {
      String name = inputData.names[i];
      SingleInput singleInput = inputData.inputs.get(name);

      // determine point where we can intersect with spine
      int spineX = singleInput.spineX;
      Location spineLoc = Location.create(spineX, curY);
      if (singleInput.ys.size() > 0) {
        // search for a Y that won't intersect with others
        // (we needn't bother if the pin doesn't connect
        // with anything anyway.)
        Collections.sort(forbiddenYs, compareYs);
        while (Collections.binarySearch(forbiddenYs, spineLoc,
              compareYs) >= 0) {
          curY += 10;
          spineLoc = Location.create(spineX, curY);
        }
        singleInput.ys.add(spineLoc);
      }
      Location loc = Location.create(curX, curY);
      inlocs.put(name, loc);

      ArrayList<Location> spine = singleInput.ys;
      if (spine.size() > 0) {
        // create wire connecting pin to spine
        /*
         * This should no longer matter - the wires will be repaired
         * anyway by the circuit's WireRepair class. if (spine.size() ==
         * 2 && spine.get(0).equals(spine.get(1))) { // a freak accident
         * where the input is used just once, // and it happens that the
         * pin is placed where no // spine is necessary Iterator<Wire>
         * it = circuit.getWires(spineLoc).iterator(); Wire existing =
         * it.next(); Wire replace = Wire.create(loc,
         * existing.getEnd1()); result.replace(existing, replace); }
         * else {
         */
        result.add(Wire.create(loc, spineLoc));
        // }

        // create spine
        Collections.sort(spine, compareYs);
        Location prev = spine.get(0);
        for (int k = 1, n = spine.size(); k < n; k++) {
          Location cur = spine.get(k);
          if (!cur.equals(prev)) {
            result.add(Wire.create(prev, cur));
            prev = cur;
          }
        }
      }

      // advance y and forbid spine intersections for next pin
      forbiddenYs.addAll(singleInput.ys);
      curY += INPUT_SPACE_Y;
    }
    for (Bus b: createBusses(inputs, inlocs)) {
      placePins(result, b, false);
    }
  }

  private static Location placeSplitter(CircuitMutation result, Location loc[], boolean isOutput) {
    int n = loc.length;
    ComponentFactory factory = SplitterFactory.instance;
    AttributeSet attrs = factory.createAttributeSet();
    attrs.setAttr(SplitterAttributes.ATTR_WIDTH, BitWidth.create(n));
    attrs.setAttr(SplitterAttributes.ATTR_FANOUT, n);
    attrs.setAttr(SplitterAttributes.ATTR_APPEARANCE, SplitterAttributes.APPEAR_LEFT);
    attrs.setAttr(StdAttr.FACING, isOutput ? Direction.NORTH : Direction.SOUTH);
    Bounds bds = factory.getOffsetBounds(attrs);
    int w = roundUp(bds.getWidth());
    int h = roundUp(bds.getHeight());
    Location splitloc;
    if (isOutput)
      splitloc = loc[0].translate(w, h);
    else
      splitloc = loc[n-1].translate(-w, -h);
    Component splitter = factory.createComponent(splitloc, attrs);
    result.add(splitter);
    // Create the L-wires
    for (int i = 0; i < n; i++) {
      Location a = loc[i];
      Location c = splitter.getEnd(i+1).getLocation();
      Location b = Location.create(c.getX(), a.getY());
      if (a.getX() != b.getX())
        result.add(Wire.create(a, b));
      if (b.getY() != c.getY())
        result.add(Wire.create(b, c));
    }
    return splitloc;
  }

  private static void placePins(CircuitMutation result, Bus bus, boolean isOutput) {
    if (bus.loc.length == 1) {
      placePin(result, bus.loc[0], bus.name, isOutput, 1);
    } else {
      Location splitloc = placeSplitter(result, bus.loc, isOutput);
      Location pinloc = splitloc.translate(isOutput ? 10 : -10, 0);
      result.add(Wire.create(splitloc, pinloc));
      placePin(result, pinloc, bus.name, isOutput, bus.loc.length);
    }
  }

  private static void placePin(CircuitMutation result, Location loc,
      String name, boolean isOutput, int width) {
    ComponentFactory factory = Pin.FACTORY;
    AttributeSet attrs = factory.createAttributeSet();
    attrs.setAttr(StdAttr.FACING, isOutput ? Direction.WEST : Direction.EAST);
    attrs.setAttr(Pin.ATTR_TYPE, isOutput ? Pin.OUTPUT : Pin.INPUT);
    attrs.setAttr(Pin.ATTR_BEHAVIOR, Pin.SIMPLE);
    attrs.setAttr(StdAttr.LABEL, name);
    attrs.setAttr(StdAttr.LABEL_LOC, Direction.NORTH);
    attrs.setAttr(StdAttr.WIDTH, BitWidth.create(width));
    result.add(factory.createComponent(loc, attrs));
  }

  private static int roundDown(int value) {
    return value / 10 * 10;
  }

  private static int roundUp(int value) {
    return (value + 9) / 10 * 10;
  }

  private CircuitBuilder() {
  }
}
