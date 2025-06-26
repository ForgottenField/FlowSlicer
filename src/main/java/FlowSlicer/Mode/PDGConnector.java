package FlowSlicer.Mode;

import FlowSlicer.Global;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;

import java.util.*;
@Slf4j
public class PDGConnector {
    private DependenceGraph currentDG;
    private CallGraph cg;
    private Unit currentUnit;
    private Unit currentReplacement;
    private int counter;
    private List<Edge> edgesToAdd;
    private List<Unit> nodesToRemove;
    private Map<String, AssignStmt> parameterMap;
    private Map<Unit, AssignStmt> returnMap;
    private Map<Unit, Unit> replacementMap;
    private Set<Edge> fieldEdges;

    public PDGConnector() {
        this.currentDG = null;
        this.cg = null;
        this.currentUnit = null;
        this.currentReplacement = null;
        this.counter = 0;
        this.edgesToAdd = new ArrayList<>();
        this.nodesToRemove = new ArrayList<>();
        this.parameterMap = new HashMap<>();
        this.returnMap = new HashMap<>();
        this.replacementMap = new HashMap<>();
        fieldEdges = new HashSet<>();
    }

    public DependenceGraph buildSDG() {
        // Find call edge targets
        log.info("Find call edge targets");
        for (final DependenceGraph pdg : Packs.getInstance()) {
            // skip dummyMain methods
            this.currentDG = pdg;
            for (final Unit unit : pdg) {
//                if (!unit.toString().contains("com.appbrain.a.bd: java.util.concurrent.atomic.AtomicBoolean e()"))
//                    continue;
                this.currentUnit = unit;
                final Stmt castedUnit = (Stmt) unit;
                if (castedUnit.containsInvokeExpr()) {
                    InvokeExpr expr = castedUnit.getInvokeExpr();
                    Set<SootMethod> methodTargets = new HashSet<>();
                    methodTargets.add(expr.getMethod());

                    for (Iterator<soot.jimple.toolkits.callgraph.Edge> i = Global.v().getAppModel().getLifeCycleHandler().getCg().edgesOutOf(unit); i.hasNext();) {
                        final SootMethod target = i.next().tgt();
                        if (target.isStaticInitializer())
                            continue;
                        methodTargets.add(target);
                    }

                    for (Iterator<soot.jimple.toolkits.callgraph.Edge> i = Global.v().getAppModel().getChaCallGraph().edgesOutOf(unit); i.hasNext();) {
                        final SootMethod target = i.next().tgt();
                        methodTargets.add(target);
                    }

                    methodTargets = new HashSet<>(methodTargets);

                    // Add static constructors if needed
                    final Set<SootMethod> toAdd = new HashSet<>();
                    for (final SootMethod methodTarget : methodTargets) {
                        if (methodTarget.isConstructor()) {
                            final SootMethod staticConstructor = methodTarget.getDeclaringClass()
                                    .getMethodByNameUnsafe("<clinit>");
                            if (staticConstructor != null) {
                                if (expr.getArgCount() == staticConstructor.getParameterCount()) {
                                    toAdd.add(staticConstructor);
                                }
                            }
                        }
                    }
                    methodTargets.addAll(toAdd);

                    Set<SootMethod> actualMethodTargets = new HashSet<>();
                    for (SootMethod methodTarget : methodTargets) {
                        if (Packs.getInstance().getPDG(methodTarget) != null) {
                            actualMethodTargets.add(methodTarget);
                        }
                    }
                    Packs.getInstance().getOriginalCallsiteToCalleeMap().put(unit, actualMethodTargets);
                }
            }
        }

        // utilize VFG to minimize graph
//        log.info("utilize VFG to minimize graph");
//        for (final DependenceGraph pdg : Packs.getInstance()) {
//            // skip dummyMain methods
//            if (Global.v().getAppModel().getLifeCycleHandler().getDummyMainMethods().contains(
//                    pdg.getMethod())) {
//                continue;
//            }
//
//            Set<Unit> flowDepartureSet = new HashSet<>();
//            Set<Unit> flowHubSet = new HashSet<>();
//            Set<Unit> flowTerminationSet = new HashSet<>();
//            Set<Unit> flowInFlightNodeSet = new HashSet<>();
//            Set<Unit> flowSourceAndSinkSet = new HashSet<>();
//
//            for (final Unit unit : pdg) {
//                if (pdg.getEntryNode().equals(unit)) {
//                    continue;
//                }
//
//                // category nodes into non-intersecting sets
//                if (Global.v().getAppModel().getTargetListFrom().contains(unit) ||
//                        Global.v().getAppModel().getTargetListTo().contains(unit)) {
//                    flowSourceAndSinkSet.add(unit);
//                } else if (Packs.getInstance().getUseDefineChain().get(unit).isEmpty() &&
//                        !Packs.getInstance().getDefineUseChain().get(unit).isEmpty()) {
//                    flowDepartureSet.add(unit);
//                } else if (Packs.getInstance().getOriginalCallsiteToCalleeMap().get(unit) != null &&
//                        !Packs.getInstance().getOriginalCallsiteToCalleeMap().get(unit).isEmpty())
//                {
//                    flowHubSet.add(unit);
//                } else if (pdg.getSuccsOf(unit).isEmpty()) {
//                    flowTerminationSet.add(unit);
//                } else {
//                    flowInFlightNodeSet.add(unit);
//                }
//            }
//
//            // delete flowFlightNodes
//            Stack<Unit> stack = new Stack<>();
//            Set<Unit> visited = new HashSet<>();
//            stack.push(pdg.getEntryNode());
//            int inFlightNodeNum = flowInFlightNodeSet.size();
//            while (!stack.isEmpty() && inFlightNodeNum > 0) {
//                Unit current = stack.pop();
//                visited.add(current);
//                for (Unit next : pdg.getSuccsOf(current)) {
//                    if (!stack.contains(next) && !visited.contains(next)) {
//                        stack.push(next);
//                    }
//                }
//                if (flowInFlightNodeSet.contains(current)) {
//                    inFlightNodeNum -= 1;
//                    for (Unit prev : pdg.getPredsOf(current)) {
//                        for (Unit next : pdg.getSuccsOf(current)) {
//                            pdg.addEdge(new Edge(prev, next, DependenceGraph.EDGE_TYPE_DATA));
//                        }
//                    }
//                    pdg.removeNode(current);
//                }
//            }
//        }

        // Add parameter nodes
        log.info("Adding parameter nodes");
        for (final DependenceGraph pdg : Packs.getInstance()) {
            this.edgesToAdd.clear();
            this.nodesToRemove.clear();

            this.currentDG = pdg;
            for (final Unit unit : pdg) {
                this.currentUnit = unit;
                findActualParameters(unit);
            }

            for (final Edge edge : this.edgesToAdd) {
                if (this.replacementMap.containsKey(edge.getFrom())) {
                    edge.setFrom(this.replacementMap.get(edge.getFrom()));
                }
                if (this.replacementMap.containsKey(edge.getTo())) {
                    edge.setTo(this.replacementMap.get(edge.getTo()));
                }
                pdg.addEdge(edge);
            }

            for (final Unit node : this.nodesToRemove) {
                pdg.removeNode(node);
            }
        }

//        for (DependenceGraph pdg : Packs.getInstance()) {
//            if (pdg.getMethod().getDeclaringClass().getName().equals("com.appbrain.a.bd$2"))
//                System.out.println(pdg.getMethod().getName());
//        }

        // Merge graphs
        log.info("Merging PDGs");
        final Iterator<DependenceGraph> iterator = Packs.getInstance().iterator();
        final DependenceGraph sdg = iterator.next().clone();
        while (iterator.hasNext()) {
            sdg.mergeWith(iterator.next());
        }

        // Add merged edges
        log.info("Adding connecting edges");
        this.currentDG = sdg;
        for (final Unit unit : sdg) {
            this.currentUnit = unit;
            if (unit instanceof Stmt) {
                final Stmt castedUnit = (Stmt) unit;
                if (castedUnit.containsInvokeExpr()) {
                    buildGraphConnection(unit, castedUnit.getInvokeExpr());
                }
            }
        }

        // Add field edges
        log.info("Adding (static-)field edges");
        addFieldEdges(sdg);

//        for (Unit unit : sdg) {
//            if (unit.toString().contains("staticinvoke <com.appbrain.a.bd: java.util.concurrent.atomic.AtomicBoolean e()>()")) {
//                for (Unit succ : sdg.getSuccsOf(unit)) {
//                    log.info(succ.toString());
//                }
//                for (Unit pred : sdg.getPredsOf(unit)) {
//                    log.info(pred.toString());
//                }
//            }
//        }

        Global.v().getAppModel().setSdg(sdg);
        return sdg;
    }

    private void addFieldEdges(DependenceGraph sdg) {
        final Map<SootField, Set<Unit>> fieldUsages = new HashMap<>();
        final Map<SootField, Set<Unit>> fieldDefinitions = new HashMap<>();
        for (final Unit unit : sdg.getAllNodes()) {
            // Usage of field
            if (unit instanceof DefinitionStmt) {
                final DefinitionStmt casted = (DefinitionStmt) unit;
                if (casted.getRightOp() instanceof FieldRef) {
                    final SootField field = ((FieldRef) casted.getRightOp()).getField();
                    if (!fieldUsages.containsKey(field)) {
                        fieldUsages.put(field, new HashSet<>());
                    }
                    fieldUsages.get(field).add(unit);
                }
            }

            // Definition of field
            if (PDGHelper.getReachingDefinition(unit) != null) {
                for (final ReachingDefinition rd : PDGHelper.getReachingDefinition(unit)) {
                    if (rd.getValueOrField().isField()) {
                        final SootField field = rd.getValueOrField().getField();
                        if (!fieldDefinitions.containsKey(field)) {
                            fieldDefinitions.put(field, new HashSet<>());
                        }
                        fieldDefinitions.get(field).addAll(rd.getUnits());
                    }
                }
            }
        }

        // Remove uses that have a direct definition predecessor
        for (final SootField field : fieldUsages.keySet()) {
            final Set<Unit> usesToRemove = new HashSet<>();
            for (final Unit use : fieldUsages.get(field)) {
                for (final Unit pred : sdg.getPredsOf(use)) {
                    if (fieldDefinitions.get(field).contains(pred)) {
                        usesToRemove.add(use);
                    }
                }
            }
            fieldUsages.get(field).removeAll(usesToRemove);
        }

        // Connect Usages with their Definitions
        for (final SootField field : fieldUsages.keySet()) {
            for (final Unit use : fieldUsages.get(field)) {
                if (fieldDefinitions.containsKey(field)) {
                    // Collect all edges
                    final Set<Edge> edges = new HashSet<>();
                    for (final Unit def : fieldDefinitions.get(field)) {
                        if (def != null) {
                            if (!sdg.hasEdge(def, use)) {
                                final Edge edge = new Edge(def, use);
                                edges.add(edge);
                            }
                            // else {
                            // edges.clear();
                            // break;
                            // }
                        }
                    }
                    // Add all edges
                    if (!edges.isEmpty()) {
                        for (final Edge edge : edges) {
                            addFieldEdge(edge, field, sdg);
                        }
                    }
                }
            }
        }
    }

    private void addFieldEdge(Edge edge, SootField field, DependenceGraph sdg) {
        if (field.isStatic()) {
            edge.setType(DependenceGraph.EDGE_TYPE_STATIC_FIELD_DATA);
        } else {
            edge.setType(DependenceGraph.EDGE_TYPE_FIELD_DATA);
        }
        sdg.addEdge(edge);
        if (SootHelper.isAnonymousClass(field.getDeclaringClass()) || SootHelper.isTemporaryField(field)) {
            Packs.getInstance().addAnyFieldEdgeLabel(edge);
        } else {
            Packs.getInstance().addFieldEdgeLabel(edge, field);
        }
    }

    private void buildGraphConnection(Unit unit, InvokeExpr expr) {
//        try {
//            // merge cg, sootCG, myCG as a new myCG
////            CallGraph sootCG = Scene.v().getCallGraph();
////            CallGraph cg = Global.v().getAppModel().getLifeCycleHandler().getCg();
////            mergeCallGraphs(cg, sootCG, myCG);
//
//
//            }
//        } catch (final Exception e) {
//            log.error(e + ": error connecting graph!");
//        }

        Unit cgUnit = Packs.getInstance().getReplacedNodesReplacementToOriginal().get(unit);
        if (cgUnit == null) {
            cgUnit = unit;
        }
        Set<SootMethod> methodTargets = Packs.getInstance().getOriginalCallsiteToCalleeMap().get(cgUnit);
        if (methodTargets == null) {
            return;
        }

        // Connect method calls with targets
        for (final SootMethod methodTarget : methodTargets) {
            // Call
            this.currentDG.addEdge(new Edge(unit, Packs.getInstance().getPDG(methodTarget).getEntryNode(),
                    DependenceGraph.EDGE_TYPE_CALL));

            // Parameters
            for (int i = 0; i < expr.getArgCount(); i++) {
                if (Packs.getInstance().getPDG(methodTarget).parameterExists(i)) {
                    final Unit from = this.parameterMap.get(toArgsString(unit, i));
                    this.currentDG
                            .addEdge(new Edge(from, Packs.getInstance().getPDG(methodTarget).getParameterNode(i),
                                    DependenceGraph.EDGE_TYPE_PARAMETER_IN));
                }
            }

            // Returns
            for (final Unit returnNode : Packs.getInstance().getPDG(methodTarget).getReturnNodes()) {
                final Unit returnParameterNode = this.returnMap.get(unit);
                if (returnParameterNode != null) {
                    this.currentDG.addEdge(
                            new Edge(returnNode, returnParameterNode, DependenceGraph.EDGE_TYPE_PARAMETER_OUT));
                } else {
                    this.currentDG.addEdge(
                            new Edge(returnNode, unit, DependenceGraph.EDGE_TYPE_PARAMETER_OUT));
                }
            }

            // Throws
            for (Type type : Packs.getInstance().getPDG(methodTarget).getExceptionsThrownMap().keySet()) {
                if (type != null) {
                    for (Unit throwBy : Packs.getInstance().getPDG(methodTarget).getExceptionsThrownMap().get(type)) {
                        if (throwBy != null) {
                            this.currentDG.addEdge(
                                    new Edge(throwBy, unit, DependenceGraph.EDGE_TYPE_PARAMETER_OUT));
                        }
                    }
                }
            }
        }
    }

    private String toArgsString(Unit unit, int number) {
        return unit.toString() + "#" + number;
    }

    private void findActualParameters(Unit unit) {
        if (unit instanceof Stmt) {
            final Stmt castedUnit = (Stmt) unit;
            if (castedUnit.containsInvokeExpr()) {
                if (unit instanceof AssignStmt) {
                    findActualParameters(castedUnit, ((AssignStmt) unit).getLeftOp());
                } else {
                    findActualParameters(castedUnit, null);
                }
            }
        }
    }

    private void findActualParameters(Stmt unit, Value returnValue) {
        Unit returnNode = null;
        // find actual parameter nodes
        if (returnValue != null) {
            returnNode = buildActualReturnNode(returnValue);
            Packs.getInstance().addActualParameterNode(returnNode);
        } else {
            this.currentReplacement = this.currentUnit;
        }
        final List<Unit> parameterNodes = new ArrayList<>();
        for (int i = 0; i < unit.getInvokeExpr().getArgCount(); i++) {
            final Value value = unit.getInvokeExpr().getArgs().get(i);
            if (value instanceof Local || value instanceof Constant) {
                final Unit parameterNode = buildActualParameterNodes(value, i);
                parameterNodes.add(parameterNode);
                Packs.getInstance().addActualParameterNode(parameterNode);
            }
        }

        // find actual return nodes
        if (returnNode != null) {
            if (!parameterNodes.isEmpty()) {
                for (final Unit parameterNode : parameterNodes) {
                    this.edgesToAdd.add(new Edge(parameterNode, returnNode, DependenceGraph.EDGE_TYPE_SUMMARY));
                }
            }
            Packs.getInstance().getReturnToCallNodes().put(returnNode, unit);
            Packs.getInstance().getCallToReturnNodes().put(unit, returnNode);
        }
    }

    private Unit buildActualParameterNodes(Value value, int number) {
        final Local newLocal = Jimple.v().newLocal("$actual_parameter_" + this.counter++, value.getType());
        final AssignStmt parameterNode = Jimple.v().newAssignStmt(newLocal, value);

        this.edgesToAdd.add(new Edge(this.currentReplacement, parameterNode, DependenceGraph.EDGE_TYPE_CONTROL));
        this.edgesToAdd.add(new Edge(this.currentReplacement, parameterNode, DependenceGraph.EDGE_TYPE_DATA));
        this.parameterMap.put(toArgsString(this.currentReplacement, number), parameterNode);

        return parameterNode;
    }

    private Unit buildActualReturnNode(Value value) {
        // Construct new locals, $actual_return_parameter_6
        final Local newLocal = Jimple.v().newLocal("$actual_return_parameter_" + this.counter++, value.getType());
        // Construct assignment statement for return values, $r3 = $actual_return_parameter_6
        final AssignStmt returnParameterNode = Jimple.v().newAssignStmt(value, newLocal);
        // Construct assignment statement for calling, $actual_return_parameter_6 = staticinvoke <de.foellix.aql.slicer.slicertestapp.MainActivity: java.lang.String access$100(de.foellix.aql.slicer.slicertestapp.MainActivity)>($r2)
        final AssignStmt replacement = Jimple.v().newAssignStmt(newLocal, ((AssignStmt) this.currentUnit).getRightOp());

        addAll(returnParameterNode.getBoxesPointingToThis(), replacement);
        this.returnMap.put(replacement, returnParameterNode);
        this.currentReplacement = replacement;
        addAll(this.currentUnit.getBoxesPointingToThis(), this.currentReplacement);
        Packs.getInstance().getReplacedNodesOriginalToReplacement().put(this.currentUnit, this.currentReplacement);
        Packs.getInstance().getReplacedNodesReplacementToOriginal().put(this.currentReplacement, this.currentUnit);
        this.replacementMap.put(this.currentUnit, this.currentReplacement);
        this.edgesToAdd.add(new Edge(replacement, returnParameterNode, DependenceGraph.EDGE_TYPE_CONTROL));
        this.edgesToAdd.add(new Edge(replacement, returnParameterNode, DependenceGraph.EDGE_TYPE_DATA));
        for (final Unit from : this.currentDG.getPredsOf(this.currentUnit)) {
            final Stmt castedUnit = (Stmt) from;
            if (castedUnit.containsInvokeExpr() && castedUnit instanceof AssignStmt) {
                continue;
            }
            for (final int type : this.currentDG.getEdgeTypes(from, this.currentUnit)) {
                this.edgesToAdd.add(new Edge(from, replacement, type));
            }
        }
        for (final Unit to : this.currentDG.getSuccsOf(this.currentUnit)) {
            for (final int type : this.currentDG.getEdgeTypes(this.currentUnit, to)) {
                this.edgesToAdd.add(new Edge(returnParameterNode, to, type));
            }
        }
        this.nodesToRemove.add(this.currentUnit);

        return returnParameterNode;
    }

    private void addAll(List<UnitBox> add, Unit to) {
        for (final UnitBox ub : add) {
            to.addBoxPointingToThis(ub);
        }
    }

    public void mergeCallGraphs(CallGraph cg, CallGraph sootCG, CallGraph myCG) {
        // Set to track unique edges
        Set<soot.jimple.toolkits.callgraph.Edge> uniqueEdges = new HashSet<>();

        // Add edges from your CHA CallGraph
        for (soot.jimple.toolkits.callgraph.Edge myEdge : myCG) {
            if (myEdge != null) {
                MethodOrMethodContext sm = myEdge.getTgt();
                if (sm != null) {
                    uniqueEdges.add(myEdge);
                }
            }
        }

        // Add edges from FlowDroid's CallGraph
        for (soot.jimple.toolkits.callgraph.Edge edge : cg) {
            if (edge != null) {
                MethodOrMethodContext sm = edge.getTgt();
                if (sm != null) {
                    if (uniqueEdges.add(edge)) {
                        myCG.addEdge(edge);
                    }
                }
            }
        }

        // Add edges from Soot's CallGraph
        for (soot.jimple.toolkits.callgraph.Edge edge : sootCG) {
            if (edge != null) {
                MethodOrMethodContext sm = edge.getTgt();
                if (sm != null) {
                    if (uniqueEdges.add(edge)) {
                        myCG.addEdge(edge);
                    }
                }
            }
        }
    }
}
