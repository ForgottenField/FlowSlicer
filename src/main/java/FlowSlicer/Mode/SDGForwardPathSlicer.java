package FlowSlicer.Mode;

import FlowSlicer.Global;
import soot.Unit;
import soot.jimple.GotoStmt;

import java.util.*;

public class SDGForwardPathSlicer {
    protected Stack<Unit> stack;
    protected DependenceGraph dg;
    protected Set<Unit> startUnitList;
    private Set<Unit> visited1;
    private Set<Unit> visited2;
    private Set<Unit> unitsInSlice;
    protected List<Set<Unit>> allPaths = new ArrayList<>();
    protected Stack<Unit> currentPath = new Stack<>();
    private int maxPathLength;

    public SDGForwardPathSlicer(DependenceGraph dg, Set<Unit> start, int maxPathLength) {
        this.stack = new Stack<>();
        this.dg = dg;

        this.startUnitList = start;

        this.visited1 = new HashSet<>();
        this.visited2 = new HashSet<>();
        this.unitsInSlice = new HashSet<>();
        this.maxPathLength = maxPathLength;
    }

    public Set<Unit> slice() {
        // Slice
        return sliceDirected(this.startUnitList);
    }

    private Set<Unit> sliceDirected(Set<Unit> startUnitList) {
        for (Unit unit : startUnitList) {
            visit(unit);
            for (Set<Unit> path : allPaths) {
                this.visited1.addAll(path);
            }
            currentPath.clear();
            allPaths.clear();
        }

        // Keep loops
        for (final Unit unit : this.dg) {
            if (unit instanceof GotoStmt) {
                final GotoStmt castedUnit = (GotoStmt) unit;
                if (this.visited1.contains(castedUnit.getTarget())) {
                    this.visited2.add(unit);
                }
            }
        }

        // Slice
        final Set<Unit> inSlice = new HashSet<>();
        inSlice.addAll(this.visited1);
        inSlice.addAll(this.visited2);
        unitsInSlice.addAll(inSlice);

        return unitsInSlice;
    }

    protected void visit(Unit start) {
        Collection<Unit> targets = Global.v().getAppModel().getSourceToSinkUnitMultimap().get(start);
        this.stack.push(start);
        boolean full = false;
        while (!this.stack.isEmpty()) {
            Unit unit = stack.peek();
            if (currentPath.size() < maxPathLength) {
                currentPath.push(unit);
                full = false;
            } else {
                full = true;
                stack.pop();
            }

            if (!full) {
                if (targets.contains(unit)) {
                    allPaths.add(new HashSet<>(currentPath));
                } else {
                    if (this.dg.getSuccsOf(unit) != null) {
                        for (final Unit succ : this.dg.getSuccsOf(unit)) {
                            if (!this.stack.contains(succ)) {
                                this.stack.push(succ);
                            }
                        }
                    }
                }
            }

            while (!currentPath.isEmpty() && currentPath.peek().equals(stack.peek())) {
                currentPath.pop();
                stack.pop();
            }
        }
    }
}
