package FlowSlicer.Mode;

import FlowSlicer.Global;
import FlowSlicer.Statistics;
import lombok.extern.slf4j.Slf4j;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.*;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.*;
@Slf4j
public class DependenceGraph implements FlowSlicer.Mode.DirectedGraph<Unit> {
    public static final int EDGE_TYPE_CONTROL = 0;
    public static final int EDGE_TYPE_CONTROL_ENTRY = 1;//Edge from entry point to the first statement
    public static final int EDGE_TYPE_DATA = 2;//Edge from data define point to data use point
    public static final int EDGE_TYPE_CALL = 3;//Edge from invoke statement to method entry point
    public static final int EDGE_TYPE_CALLBACK = 4;//Edge from callback invoke statement to callback entry point
    public static final int EDGE_TYPE_PARAMETER_IN = 5;//Edge from caller input parameter to callee input parameter
    public static final int EDGE_TYPE_PARAMETER_OUT = 6;//Edge from callee output parameter to caller output parameter
    public static final int EDGE_TYPE_SUMMARY = 7;//Edge from caller input parameter to caller output parameter
    public static final int EDGE_TYPE_FIELD_DATA = 8;
    public static final int EDGE_TYPE_STATIC_FIELD_DATA = 9;
    public static final int EDGE_TYPE_EXCEPTION = 10;
    public static final int EDGE_TYPE_CALLBACK_ALTERNATIVE = 11;
    public static final int EDGE_TYPE_CALLBACK_DEFINITION = 12;
    public static final int EDGE_TYPE_ICC = 13;
    private SootMethod method;//every dependenceGraph refers to a method
    private Set<Unit> allNodes;
    private Unit entryNode;
    private List<Unit> entryNodes;
    private Set<Unit> sourceNodes;
    private Set<Unit> sinkNodes;
    private Set<Unit> callNodes;
    private Set<Unit> nopNodes;
    private List<Unit> returnNodes;
    private Map<Unit, Set<Unit>> predecessors;// to -> from
    private Map<Unit, Set<Unit>> successors;// from -> to
    private Map<Integer, Set<Integer>> typeMap;//Map from value of edge hash to types of the edge
    private Unit[] parameterNodes;
    private Map<Type, Set<Unit>> exceptionsThrownMap;
    private Unit root;

    public DependenceGraph(SootMethod method, UnitGraph cfg) {
        this(method, cfg, null);
    }

    public DependenceGraph(SootMethod method, UnitGraph cfg, Unit entryNode) {
        this.method = method;
        this.allNodes = new HashSet<>();
        this.predecessors = new HashMap<>();
        this.successors = new HashMap<>();
        this.typeMap = new HashMap<>();
        this.exceptionsThrownMap = new HashMap<>();
        this.root = null;

        // Entry node
        if (entryNode == null) {
            this.entryNode = Jimple.v().newNopStmt();
        } else {
            this.entryNode = entryNode;
        }
        this.entryNodes = new ArrayList<>();
        this.entryNodes.add(this.entryNode);
        addNode(this.entryNode);

        // Other nodes
        this.returnNodes = new ArrayList<>();
        this.sourceNodes = new HashSet<>();
        this.sinkNodes = new HashSet<>();
        this.callNodes = new HashSet<>();
        this.nopNodes = new HashSet<>();

        this.parameterNodes = new Unit[method.getParameterCount()];
        if (cfg != null) {
            for (final Unit unit : cfg) {
                addNode(unit);
                for (final ValueBox box : unit.getUseBoxes()) {
                    if (box.getValue() instanceof ParameterRef) {
                        this.parameterNodes[((ParameterRef) box.getValue()).getIndex()] = unit;
                    }
                }
                if (unit instanceof ReturnStmt || unit instanceof ReturnVoidStmt) {
                    this.returnNodes.add(unit);
                    continue;
                }
                if (((Stmt)unit).containsInvokeExpr()) {
                    if (Global.v().getAppModel().getSourceToSinkUnitMultimap().keySet().contains(unit)) {
                        this.sourceNodes.add(unit);
                    } else if (Global.v().getAppModel().getSinkToSourceUnitMultimap().keySet().contains(unit)) {
                        this.sinkNodes.add(unit);
                    } else {
                        this.callNodes.add(unit);
                    }
                    continue;
                }
                this.nopNodes.add(unit);
            }
        }
        assert (allNodes.size() == sourceNodes.size() + sinkNodes.size() + callNodes.size() + entryNodes.size() + returnNodes.size() + nopNodes.size());
    }

    public void addNode(Unit unit) {
        nodeToString(unit, this.method);
        this.allNodes.add(unit);
        this.predecessors.put(unit, new HashSet<>());
        this.successors.put(unit, new HashSet<>());
    }

    public Set<Unit> notIn(Set<Unit> in) {
        final Set<Unit> notIn = new HashSet<>(this.allNodes);
        notIn.removeAll(in);
        return notIn;
    }

    public void removeNodes(Set<Unit> unitsToRemove) {
        if (unitsToRemove == null) {
            return;
        }
        for (final Unit unitToRemove : unitsToRemove) {
            removeNode(unitToRemove);
        }
    }

    public void removeNode(Unit node) {
        this.allNodes.remove(node);
        if (this.predecessors.containsKey(node)) {
            for (final Unit pred : this.predecessors.get(node)) {
                if (this.successors.containsKey(pred)) {
                    this.successors.get(pred).remove(node);
                }
            }
        }
        if (this.successors.containsKey(node)) {
            for (final Unit succ : this.successors.get(node)) {
                if (this.predecessors.containsKey(succ)) {
                    this.predecessors.get(succ).remove(node);
                }
            }
        }
        this.predecessors.remove(node);
        this.successors.remove(node);
    }

    public void addEdge(Edge edge) {
        // Check if node is known
        if (!this.allNodes.contains(edge.getFrom())) {
            addNode(edge.getFrom());
        }
        if (!this.allNodes.contains(edge.getTo())) {
            addNode(edge.getTo());
        }

        // Type
        final Integer edgeHash = new Edge(edge.getFrom(), edge.getTo()).hashCode();
        if (!this.typeMap.containsKey(edgeHash)) {
            this.typeMap.put(edgeHash, new HashSet<>());
        }
        this.typeMap.get(edgeHash).add(edge.getType());

        // Predecessors & Successors
        this.predecessors.get(edge.getTo()).add(edge.getFrom());
        this.successors.get(edge.getFrom()).add(edge.getTo());
    }

    public boolean hasEdge(Unit from, Unit to) {
        if (this.successors.get(from) == null) {
            return false;
        } else {
            return this.successors.get(from).contains(to);
        }
    }

    public void addExceptionThrown(Type type, Unit thrownBy) {
        if (!this.exceptionsThrownMap.containsKey(type)) {
            this.exceptionsThrownMap.put(type, new HashSet<>());
        }
        this.exceptionsThrownMap.get(type).add(thrownBy);
    }

    public String nodeToString(Unit node) {
        return nodeToString(node, this.method);
    }

    public static String nodeToString(Unit node, SootMethod sm) {
        String str = node.toString();
        if (node instanceof NopStmt) {
            if (Packs.getInstance().getMethodFromEntryNode(node) == null) {
                if (sm != null) {
                    Packs.getInstance().putEntryNode(node, sm);
                } else {
                    log.info("No suitable method entry found: " + node);
                    return node.hashCode() + ":\nUNKNOWN";
                }
            }
            final SootMethod loadedMethodRef = Packs.getInstance().getMethodFromEntryNode(node);
            str = "ENTRY: " + loadedMethodRef.getName() + (loadedMethodRef.getParameterTypes().isEmpty() ? "" : "\n")
                    + (loadedMethodRef.getParameterTypes()).toString().replace("[", "(").replace("]", ")") + "\n["
                    + loadedMethodRef.getDeclaringClass().getName() + "]";
        }else if (Packs.getInstance().getReturnToCallNodes().containsKey(node)) {
            str = str + "\nRETURN: " + Packs.getInstance().getReplacedNodesOriginalToReplacement()
                    .get(Packs.getInstance().getReturnToCallNodes().get(node)).hashCode();
        }
        return node.hashCode() + ":\n" + str;
    }

    public SootMethod getMethod() {
        return this.method;
    }

    public void setMethod(SootMethod sm) {
        this.method = sm;
    }

    public Set<Unit> getAllNodes(){
        return this.allNodes;
    }

    public Unit getEntryNode() {
        return this.entryNode;
    }

    public List<Unit> getEntryNodes(){
        return this.entryNodes;
    }

    public List<Unit> getReturnNodes(){
        return this.returnNodes;
    }

    public Map<Unit, Set<Unit>> getPredecessors(){
        return this.predecessors;
    }

    public Map<Unit, Set<Unit>> getSuccessors(){
        return this.successors;
    }

    public Map<Integer, Set<Integer>> getTypeMap(){
        return this.typeMap;
    }

    public Unit[] getParameterNodes(){
        return this.parameterNodes;
    }

    public Map<Type, Set<Unit>> getExceptionsThrownMap(){
        return this.exceptionsThrownMap;
    }

    public Set<Unit> getExceptionsThrown(Type type) {
        return this.exceptionsThrownMap.get(type);
    }

    public Unit getRoot() {
        if (this.root == null) {
            final List<Unit> candidates = new ArrayList<>();
            for (final Unit head : getHeads()) {
                if (getPredsOf(head).isEmpty()) {
                    candidates.add(head);
                }
            }
            if (candidates.size() > 1) {
                log.info("The graph has more than one root node - selecting dummy main.");
                for (final Unit candidate : candidates) {
                    if (nodeToString(candidate).contains("ENTRY: dummyMainMethod")) {
                        this.root = candidate;
                        break;
                    }
                }
                if (this.root == null) {
                    log.info("No dummy main found - selecting first candidate.");
                    this.root = candidates.get(0);
                }
            } else if (candidates.size() == 1) {
                this.root = candidates.get(0);
            }
        }
        return this.root;
    }

    public void mergeWith(DependenceGraph graph) {
        // Merge nodes
        for (final Unit unit : graph) {
            this.allNodes.add(unit);
        }

        // Merge entry nodes
        this.entryNodes.addAll(graph.getHeads());

        // Merge Edges
        for (final Unit unit : graph) {
            if (this.predecessors.containsKey(unit)) {
                this.predecessors.get(unit).addAll(graph.getPredsOf(unit));
            } else {
                this.predecessors.put(unit, graph.getPredsOf(unit));
            }
            if (this.successors.containsKey(unit)) {
                this.successors.get(unit).addAll(graph.getSuccsOf(unit));
            } else {
                this.successors.put(unit, graph.getSuccsOf(unit));
            }
        }

        // Merge TypeMap
        for (final Integer edgeHash : graph.getTypeMap().keySet()) {
            this.typeMap.put(edgeHash, graph.getTypeMap().get(edgeHash));
        }
    }

    public Unit getParameterNode(int number) {
        return this.parameterNodes[number];
    }
    public void setParameterNode(Unit[] parameterNodes){this.parameterNodes = parameterNodes;}
    public boolean parameterExists(int number) {
        return this.parameterNodes.length > number;
    }
    public Set<Integer> getEdgeTypes(Edge edge) {
        return this.typeMap.get(edge.hashCode());
    }

    public Set<Integer> getEdgeTypes(Unit from, Unit to) {
        return this.typeMap.get(new Edge(from, to).hashCode());
    }

    @Override
    public List<Unit> getHeads() {
        return this.entryNodes;
    }

    @Override
    public List<Unit> getTails() {
        return null;
    }

    @Override
    public Set<Unit> getPredsOf(Unit s) {
        return this.predecessors.get(s);
    }

    @Override
    public Set<Unit> getSuccsOf(Unit s) {
        return this.successors.get(s);
    }

    @Override
    public int size() {
        return this.allNodes.size();
    }

    @Override
    public Iterator<Unit> iterator() {
        return this.allNodes.iterator();
    }

    @Override
    public DependenceGraph clone() {
        final DependenceGraph clone = new DependenceGraph(this.method, null, this.entryNode);
        clone.allNodes.addAll(this.allNodes);
        clone.entryNodes.addAll(this.entryNodes);
        clone.returnNodes.addAll(this.returnNodes);
        clone.entryNode = this.entryNode;
        for (final Unit key : this.predecessors.keySet()) {
            clone.predecessors.put(key, new HashSet<>(this.predecessors.get(key)));
        }
        for (final Unit key : this.successors.keySet()) {
            clone.successors.put(key, new HashSet<>(this.successors.get(key)));
        }
        for (final Integer key : this.typeMap.keySet()) {
            clone.typeMap.put(key, new HashSet<>(this.typeMap.get(key)));
        }
        if(this.parameterNodes != null){
            clone.parameterNodes = this.parameterNodes.clone();
        }
        for (final Type key : this.exceptionsThrownMap.keySet()) {
            clone.exceptionsThrownMap.put(key, new HashSet<>(this.exceptionsThrownMap.get(key)));
        }
        clone.root = this.root;
        return clone;
    }
}
