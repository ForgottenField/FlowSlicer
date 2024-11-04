package FlowSlicer.Mode;

import com.google.common.collect.Multimap;
import soot.*;
import soot.jimple.*;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PDGHelper {
    private static Map<Unit, Set<ReachingDefinition>> reachingDefinitionMap = new ConcurrentHashMap<>();
    public static DependenceGraph getPDG(Body b) {
        // Compute Control Flow
        final UnitGraph cfg = new ExceptionalUnitGraph(b);
        final BlockGraph blockCFG = new ExceptionalBlockGraph(b);
        final DependenceGraph pdg = new DependenceGraph(b.getMethod(), cfg);
        final MHGPostDominatorsFinder<Unit> domFinder = new MHGPostDominatorsFinder<>(cfg);
        for (final Unit x : cfg) {
            for (final Unit y : cfg.getSuccsOf(x)) {
                if (!domFinder.isDominatedBy(x, y)) {
                    addControlDependence(x, y, domFinder, pdg, new HashSet<>());
                }
            }
        }

        // Include entry node
        for (final Unit x : cfg) {
            if (pdg.getPredsOf(x).isEmpty()) {
                pdg.addEdge(new Edge(pdg.getEntryNode(), x, DependenceGraph.EDGE_TYPE_CONTROL_ENTRY));
            }
        }

        // Compute Data Flow
        ForwardFlowAnalysis<Unit, Set<ReachingDefinition>> rd = new ReachingDefinitionAnalysis(cfg);

        for (final Unit defUnit : cfg) {
            Packs.getInstance().getDefValuesMap().put(defUnit, rd.getFlowBefore(defUnit));
            final Set<ValueOrField> defValues = getDefValues(defUnit, rd.getFlowBefore(defUnit));

            for (final ValueOrField defValue : defValues) {
                if (defValue.isLocalOrRef()) {
                    for (final Unit useUnit : cfg) {
                        if (defUnit == useUnit) {
                            continue;
                        }

                        boolean valid = false;
                        for (final ValueOrField useValue : getUseValues(useUnit)) {
                            valid = useValue.equals(defValue);
                            if (valid) {
                                break;
                            }
                        }

                        if (valid && contains(rd.getFlowBefore(useUnit), defValue, defUnit)) {
//                            Packs.getInstance().getDefineUseChain().put(defUnit, useUnit);
//                            Packs.getInstance().getUseDefineChain().put(useUnit, defUnit);
                            pdg.addEdge(new Edge(defUnit, useUnit, DependenceGraph.EDGE_TYPE_DATA));
                        }
                    }
                }
            }
        }
//        Multimap<Unit, Unit> collection = Packs.getInstance().getDefineUseChain();

        // Add exceptions thrown
        for (final Unit unit : b.getUnits()) {
            if (unit instanceof ThrowStmt) {
                final ThrowStmt castedUnit = (ThrowStmt) unit;
                final Type type = castedUnit.getOp().getType();
                pdg.addExceptionThrown(type, unit);
            }
        }

        for (final Unit unit : b.getUnits()) {
            reachingDefinitionMap.put(unit, rd.getFlowBefore(unit));
        }

        return pdg;
    }

    private static void addControlDependence(Unit x, Unit y, MHGPostDominatorsFinder<Unit> domFinder, DependenceGraph pdg, Set<Unit> visited) {
        visited.add(y);
        if (x != y) {
            pdg.addEdge(new Edge(x, y, DependenceGraph.EDGE_TYPE_CONTROL));
            for (final Unit z : domFinder.getGraph().getPredsOf(y)) {
                if (z!= y && !visited.contains(z)) {
                    addControlDependence(x, z, domFinder, pdg, visited);
                }
            }
        }
    }

    private static Set<ValueOrField> getDefValues(Unit defUnit, Set<ReachingDefinition> rds) {
        final Set<ValueOrField> defValues = new HashSet<>();

        // DefBoxes
        for (final ValueBox box : defUnit.getDefBoxes()) {
            defValues.add(new ValueOrField(box.getValue()));
        }
        // Other
        if (defUnit instanceof Stmt) {
            if (defUnit instanceof DefinitionStmt) {
                defValues.add(new ValueOrField(((DefinitionStmt) defUnit).getLeftOp()));
            }
            if (!defValues.isEmpty()) {
                final Set<ValueOrField> toAdd = new HashSet<>();
                for (final ValueOrField defValue : defValues) {
                    for (final ReachingDefinition aliasRD : rds) {
                        if (aliasRD.hasAliases() && aliasRD.getValueOrField().equals(defValue)) {
                            toAdd.addAll(aliasRD.getAliases());
                        }
                    }
                }
                defValues.addAll(toAdd);
            }
            if (((Stmt) defUnit).containsInvokeExpr()) {
                // Base
                if (((Stmt) defUnit).getInvokeExpr() instanceof InstanceInvokeExpr) {
                    if (StubDroidReader.assignsToBase(((Stmt) defUnit).getInvokeExpr().getMethod())) {
                        defValues.add(
                                new ValueOrField(((InstanceInvokeExpr) ((Stmt) defUnit).getInvokeExpr()).getBase()));
                    }
                }
                // Parameters
                for (int i = 0; i < ((Stmt) defUnit).getInvokeExpr().getArgCount(); i++) {
                    if (StubDroidReader.assignsToParameter(((Stmt) defUnit).getInvokeExpr().getMethod(), i)) {
                        defValues.add(new ValueOrField(((Stmt) defUnit).getInvokeExpr().getArg(i)));
                    }
                }
            }
        }
        return defValues;
    }

    public static Set<ValueOrField> getUseValues(Unit useUnit) {
        final Set<ValueOrField> useValues = new HashSet<>();

        // UseBoxes
        for (final ValueBox useBox : useUnit.getUseBoxes()) {
            useValues.add(new ValueOrField(useBox.getValue()));
        }
        // ArrayRef on LHS
        if (useUnit instanceof DefinitionStmt) {
            final Value lhs = ((DefinitionStmt) useUnit).getLeftOp();
            if (lhs instanceof ArrayRef) {
                useValues.add(new ValueOrField(lhs));
            }
        }
        // Base
        if (useUnit instanceof InvokeStmt) {
            final InvokeStmt otherCfgUnitsInvokeStmt = (InvokeStmt) useUnit;
            if (otherCfgUnitsInvokeStmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
                final InstanceInvokeExpr otherCfgUnitsInvokeExpr = (InstanceInvokeExpr) otherCfgUnitsInvokeStmt
                        .getInvokeExpr();
                useValues.add(new ValueOrField(otherCfgUnitsInvokeExpr.getBase()));
            }
        }

        return useValues;
    }

    private static boolean contains(Set<ReachingDefinition> rds, ValueOrField value, Unit defUnit) {
        for (final ReachingDefinition rd : rds) {
            if (rd.getValueOrField().equals(value) && rd.getUnits().contains(defUnit)) {
                return true;
            }
        }
        return false;
    }

    public static Set<ReachingDefinition> getReachingDefinition(Unit unit) {
        return reachingDefinitionMap.get(unit);
    }
}
