package FlowSlicer.Mode;

import soot.SootField;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.FieldRef;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class FieldCollector {
    protected DependenceGraph dg;
    protected Unit targetUnit;
    protected Set<SootField> fieldsToConsider;

    public FieldCollector(DependenceGraph dg, Unit targetUnit) {
        this.dg = dg;
        this.targetUnit = targetUnit;
        this.fieldsToConsider = new HashSet<>();
    }

    public Set<SootField> collectFields() {
        return collectFields(this.targetUnit);
    }

    private Set<SootField> collectFields(Unit targetUnitList) {
        // (Remember: !initial -> fieldsToConsider is subset of fieldsWhileCollecting regarding the current target)
        addConsideredFields(targetUnitList);
        collectFieldsForward(targetUnitList);
        collectFieldsBackward(targetUnitList);

        return this.fieldsToConsider;
    }

    private void collectFieldsForward(Unit startUnit) {
        Set<Unit> visited = new HashSet<>();
        Stack<Unit> stack = new Stack<>();
        stack.push(startUnit);

        while (!stack.isEmpty()) {
            Unit unit = stack.pop();
            if (visited.contains(unit)) {
                continue;
            }
            visited.add(unit);

            if (this.dg.getSuccsOf(unit) != null) {
                for (final Unit succ : this.dg.getSuccsOf(unit)) {
                    Edge temp = new Edge(unit, succ);
                    if (!visited.contains(succ) && (this.dg.getEdgeTypes(temp).size() > 1
                            || !this.dg.getEdgeTypes(temp).contains(DependenceGraph.EDGE_TYPE_CONTROL))) {
                        addConsideredFields(succ);
                        stack.push(succ);
                    }
                    temp = null;
                }
            }
        }
    }

    private void collectFieldsBackward(Unit startUnit) {
        Set<Unit> visited = new HashSet<>();
        Stack<Unit> stack = new Stack<>();
        stack.push(startUnit);

        while (!stack.isEmpty()) {
            Unit unit = stack.pop();
            if (visited.contains(unit)) {
                continue;
            }
            visited.add(unit);

            if (this.dg.getPredsOf(unit) != null) {
                for (final Unit pred : this.dg.getPredsOf(unit)) {
                    Edge temp = new Edge(pred, unit);
                    if (!visited.contains(pred) && (this.dg.getEdgeTypes(temp).size() > 1
                            || !this.dg.getEdgeTypes(temp).contains(DependenceGraph.EDGE_TYPE_CONTROL))) {
                        addConsideredFields(pred);
                        stack.push(pred);
                    }
                    temp = null;
                }
            }
        }
    }

    private void addConsideredFields(Unit unit) {
        for (final ValueBox box : unit.getUseAndDefBoxes()) {
            if (box.getValue() instanceof FieldRef) {
                // FieldRef
                final SootField field = ((FieldRef) box.getValue()).getField();
                this.fieldsToConsider.add(field);

                // Remember: Alias not required? They would destroy running example!
            }
        }
    }
}
