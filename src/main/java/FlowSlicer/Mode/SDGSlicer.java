package FlowSlicer.Mode;

import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.GotoStmt;
import soot.jimple.NopStmt;
import soot.jimple.Stmt;

import java.util.*;

public abstract class SDGSlicer {
    protected Queue<Unit> worklist;
    protected DependenceGraph dg;
    protected List<Unit> targetUnitList;
    protected Set<SootField> fieldsToConsider;
    protected Set<Unit> csVisited;
    public GlobalVisit globalVisit;

    private Set<Unit> visited1;
    private Set<Unit> visited2;
    private Set<Unit> visited3;
    private Set<Unit> contextSensitivelyAllowed;
    private Set<Integer> ignoredTypes;

    public SDGSlicer(DependenceGraph dg, Set<Unit> target, Set<SootField> fieldsToConsider) {
        this.worklist = new LinkedList<>();
        this.dg = dg;

        this.targetUnitList = new ArrayList<>();
        this.targetUnitList.addAll(target);

        this.fieldsToConsider = null;
        this.csVisited = new HashSet<>();

        this.visited1 = new HashSet<>();
        this.visited2 = new HashSet<>();
        this.visited3 = new HashSet<>();
        this.contextSensitivelyAllowed = new HashSet<>();
        this.ignoredTypes = new HashSet<>();

        // Collect fields
        if (fieldsToConsider != null && !fieldsToConsider.isEmpty()) {
            this.fieldsToConsider = fieldsToConsider;
        }

        // Initialize context-sensitivity
        for (Unit unit : target) {
            init(unit);
        }
        this.contextSensitivelyAllowed = new HashSet<>(this.contextSensitivelyAllowed);

        // Register
        this.globalVisit = new GlobalVisit(this.contextSensitivelyAllowed);
    }

    public SDGSlicer(DependenceGraph dg, Unit target, Set<SootField> fieldsToConsider) {
        this.worklist = new LinkedList<>();
        this.dg = dg;

        this.targetUnitList = new ArrayList<>();
        this.targetUnitList.add(target);

        this.fieldsToConsider = null;
        this.csVisited = new HashSet<>();

        this.visited1 = new HashSet<>();
        this.visited2 = new HashSet<>();
        this.visited3 = new HashSet<>();
        this.contextSensitivelyAllowed = new HashSet<>();
        this.ignoredTypes = new HashSet<>();

        // Collect fields
        if (fieldsToConsider != null && !fieldsToConsider.isEmpty()) {
            this.fieldsToConsider = fieldsToConsider;
        }

        // Initialize context-sensitivity
        init(target);

        // Register
        this.globalVisit = new GlobalVisit(this.contextSensitivelyAllowed);
    }

    public Set<Unit> slice() {
        // Slice
        final Set<Unit> slice = sliceDirected(this.targetUnitList);
        this.globalVisit.getVisited().addAll(slice);
        this.globalVisit.getContextSensitivity().addAll(this.contextSensitivelyAllowed);

        return slice;
    }

    private Set<Unit> sliceDirected(List<Unit> targetUnitList) {
        int before = -1;
        do {
            if (before != -1) {
                this.visited1.clear();
                this.visited2.clear();
            }
            before = this.contextSensitivelyAllowed.size();
            // Phase 1
            Set<Integer> ignoreTypes = new HashSet<>(this.ignoredTypes);
            ignoreTypes.add(DependenceGraph.EDGE_TYPE_PARAMETER_OUT);
            this.worklist.addAll(targetUnitList);
            visit(this.visited1, ignoreTypes);

            if (before == this.contextSensitivelyAllowed.size()) {
                // Phase 2
                ignoreTypes = new HashSet<>(this.ignoredTypes);
                ignoreTypes.add(DependenceGraph.EDGE_TYPE_PARAMETER_IN);
//                ignoreTypes.add(DependenceGraph.EDGE_TYPE_CALL);
                // Call edges cannot simply be ignored here since they must be traversed
                // if a function is reached via e.g. field edges before.
                // Instead context-sensitivity is tracked.
                this.worklist.addAll(this.visited1);
                visit(this.visited2, ignoreTypes);
            }
        } while (before < this.contextSensitivelyAllowed.size());

        // Keep loops
        for (final Unit unit : this.dg) {
            if (unit instanceof GotoStmt) {
                final GotoStmt castedUnit = (GotoStmt) unit;
                if (this.visited1.contains(castedUnit.getTarget()) || this.visited2.contains(castedUnit.getTarget())) {
                    this.visited3.add(unit);
                }
            }
        }

        // Slice
        final Set<Unit> unitsInSlice = new HashSet<>();
        unitsInSlice.addAll(this.visited1);
        unitsInSlice.addAll(this.visited2);
        unitsInSlice.addAll(this.visited3);
        return unitsInSlice;
    }

    protected abstract void init(Unit start);

    public void visit(Set<Unit> visited, Set<Integer> ignoreEdgeTypes) {
        while (!this.worklist.isEmpty()) {
            visit(this.worklist.poll(), visited, ignoreEdgeTypes, true);
        }
    }

    protected abstract void visit(Unit start, Set<Unit> visited, Set<Integer> ignoreEdgeTypes, boolean ignoreAlways);

    protected boolean ignoreEdge(Set<Integer> edgeTypes, Set<Integer> ignoreEdgeTypes, boolean ignoreAlways) {
        if (ignoreEdgeTypes.isEmpty()) {
            return false;
        } else {
            if (ignoreAlways) {
                for (final Integer type : edgeTypes) {
                    if (ignoreEdgeTypes.contains(type)) {
                        return true;
                    }
                }
                return false;
            } else {
                for (final Integer type : edgeTypes) {
                    if (!ignoreEdgeTypes.contains(type)) {
                        return false;
                    }
                }
                return true;
            }
        }
    }

    public Set<Integer> getIgnoreTypes() {
        return this.ignoredTypes;
    }

    protected Set<Unit> getContextSensitivelyAllowed() {
        return this.contextSensitivelyAllowed;
    }

    protected Set<Unit> findMethodCalls(Unit unit) {
        final Set<Unit> methodCalls = new HashSet<>();
        if (!this.csVisited.contains(unit)) {
            this.csVisited.add(unit);

            if (unit instanceof Stmt) {
                final Stmt castedUnit = (Stmt) unit;
                if (castedUnit.containsInvokeExpr()) {
                    methodCalls.add(unit);
                }
            }
            Unit entryNode = getEntryNode(unit);
            if (entryNode == null) {
                entryNode = unit;
            }
            if (this.dg.getPredsOf(entryNode) != null) {
                methodCalls.addAll(this.dg.getPredsOf(entryNode));
            }

            final Set<Unit> methodsToAdd = new HashSet<>();
            //递归寻找所有方法调用语句
            for (final Unit call : methodCalls) {
                methodsToAdd.addAll(findMethodCalls(call));
            }
            methodCalls.addAll(methodsToAdd);
            //采用HashSet进行去重操作
            for (final Unit methodCall : new HashSet<>(methodCalls)) {
                if (methodCall instanceof Stmt && !((Stmt) methodCall).containsInvokeExpr()) {
                    methodCalls.remove(methodCall);
                }
            }
        }
        return methodCalls;
    }

    private Unit getEntryNode(Unit unit) {
        final SootMethod sm = SootHelper.getMethod(unit);
        if (sm != null) {
            final Unit firstUnitOfSM = sm.retrieveActiveBody().getUnits().getFirst();
            final Set<Unit> candidates = this.dg.getPredsOf(firstUnitOfSM);
            if (candidates != null) {
                for (final Unit candidate : candidates) {
                    if (candidate instanceof NopStmt) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    public List<Unit> getTargetUnitList() {
        return this.targetUnitList;
    }

    public GlobalVisit getGlobalVisit() {
        return this.globalVisit;
    }
}
