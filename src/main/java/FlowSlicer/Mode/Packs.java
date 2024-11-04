package FlowSlicer.Mode;

import FlowSlicer.Config;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.collect.ConcurrentHashSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@Slf4j
@Getter
public class Packs implements Iterable<DependenceGraph>{
    private ConcurrentHashMap<SootMethod, DependenceGraph> pdgMap;// The first of pdgMap is the merged sdg.

    /* contains map from the statement which is both invoke and assignment statement to Replacement statement, for example,
        r2 = specialinvoke r0.<android.arch.lifecycle.Lifecycle$State calculateTargetState(android.arch.lifecycle.LifecycleObserver)>($r1)
        ->
        $actual_return_parameter_4 = specialinvoke r0.<android.arch.lifecycle.Lifecycle$State calculateTargetState(android.arch.lifecycle.LifecycleObserver)>($r1)
    */
    private Map<Unit, Unit> replacedNodesOriginalToReplacement;
    private Map<Unit, Unit> replacedNodesReplacementToOriginal;

    /* contains actual parameter, for example,
        $actual_parameter_0 = $r1
        $actual_return_parameter_1 = $r2
    */
    private Set<Unit> actualParameterNodes;
    private Map<Unit, SootMethod> entryNodeMap;//unit is the entry node of sootMethod.

    /* contains map from call node to return node, for example,
        r2 = specialinvoke r0.<android.arch.lifecycle.Lifecycle$State calculateTargetState(android.arch.lifecycle.LifecycleObserver)>($r1)
        ->
        r2 = $actual_return_parameter_4
    */
    private Map<Unit, Unit> callToReturnNodesMap;
    private Map<Unit, Unit> returnToCallNodesMap;
    private Set<String> callBackClasses;
    private Map<Unit, Set<ReachingDefinition>> defValuesMap;
    private Map<Unit, Set<SootMethod>> originalCallsiteToCalleeMap;
    private Multimap<Unit, Unit> fieldEdgeFromToMap;
    private Map<Integer, EdgeLabel> edgeLabels;
    private static Packs instance = new Packs();
    public static final EdgeLabel ANY_FIELD_LABEL = new EdgeLabel();
    private Packs(){
        this.pdgMap = new ConcurrentHashMap<>();
        this.replacedNodesOriginalToReplacement = new ConcurrentHashMap<>();
        this.replacedNodesReplacementToOriginal = new ConcurrentHashMap<>();
        this.entryNodeMap = new ConcurrentHashMap<>();
        this.actualParameterNodes = new ConcurrentHashSet<>();
        this.callToReturnNodesMap = new ConcurrentHashMap<>();
        this.returnToCallNodesMap = new ConcurrentHashMap<>();
        this.defValuesMap = new ConcurrentHashMap<>();
        originalCallsiteToCalleeMap = new ConcurrentHashMap<>();
        fieldEdgeFromToMap = HashMultimap.<Unit, Unit>create();
        this.callBackClasses = null;
        this.edgeLabels = new ConcurrentHashMap<>();
    }

    public static Packs getInstance() {
        return instance;
    }

    @Override
    public Iterator<DependenceGraph> iterator() {
        final List<DependenceGraph> list = new ArrayList<>(this.pdgMap.values());
        return list.iterator();
    }
    public SootMethod getEntryMethod(Unit node) {
        return this.entryNodeMap.get(node);
    }

    public Set<Unit> getEntryNodes(SootMethod sm) {
        final Set<SootClass> allClasses = SootHelper.getAllAccessibleClasses(sm.getDeclaringClass());
        final Set<Unit> entryNodes = new HashSet<>();
        for (final Unit entryNode : this.entryNodeMap.keySet()) {
            final SootMethod candidate = this.entryNodeMap.get(entryNode);
            if (sm.getSignature().equals(candidate.getSignature())) {
                // Exact match
                entryNodes.clear();
                entryNodes.add(entryNode);
                return entryNodes;
            } else if (sm.getName().equals(candidate.getName())) {
                // Candidate match
                SootClass sc = candidate.getDeclaringClass();
                while (sc != null) {
                    if (allClasses.contains(sc)) {
                        entryNodes.add(entryNode);
                        break;
                    }
                    boolean added = false;
                    for (final SootClass si : sc.getInterfaces()) {
                        if (si == sm.getDeclaringClass()) {
                            entryNodes.add(entryNode);
                            added = true;
                            break;
                        }
                    }
                    if (added) {
                        break;
                    }
                    sc = sc.getSuperclassUnsafe();
                }
            }
        }
        return entryNodes;
    }

    public void addPdg(SootMethod method, DependenceGraph pdg) {
        this.pdgMap.put(method, pdg);
    }

    public DependenceGraph getPDG(SootMethod method) {
        return this.pdgMap.get(method);
    }

    public Map<SootMethod, DependenceGraph> getPdgMap(){
        return this.pdgMap;
    }

    public Set<Unit> getActualParameterNodes() {
        return this.actualParameterNodes;
    }

    public Map<Unit, SootMethod> getEntryNodeMap(){
        return this.entryNodeMap;
    }

    public void putEntryNode(Unit node, SootMethod sm) {
        this.entryNodeMap.put(node, sm);
    }
    public Map<Unit, Unit> getReplacedNodesOriginalToReplacement() {
        return this.replacedNodesOriginalToReplacement;
    }

    public Map<Unit, Unit> getReplacedNodesReplacementToOriginal() {
        return this.replacedNodesReplacementToOriginal;
    }

    public SootMethod getMethodFromEntryNode(Unit node) {
        return this.entryNodeMap.get(node);
    }
    public Map<Unit, Unit> getCallToReturnNodes() {
        return this.callToReturnNodesMap;
    }

    public Map<Unit, Unit> getReturnToCallNodes() {
        return this.returnToCallNodesMap;
    }
    public Map<Unit, Set<ReachingDefinition>> getDefValuesMap() {
        return this.defValuesMap;
    }

    public SootField getFieldEdgeLabel(Edge edge) {
        if (this.edgeLabels.containsKey(edge.hashCode())) {
            return this.edgeLabels.get(edge.hashCode()).getField();
        } else {
            return null;
        }
    }

    public Set<String> getCallBackClasses() {
        if (this.callBackClasses == null) {
            this.callBackClasses = new HashSet<>();
            this.callBackClasses.addAll(Config.getInstance().getCallbacksList());
        }
        return this.callBackClasses;
    }

    public void addAnyFieldEdgeLabel(Edge edge) {
        this.edgeLabels.put(edge.hashCode(), ANY_FIELD_LABEL);
    }

    public void addFieldEdgeLabel(Edge edge, SootField field) {
        this.edgeLabels.put(edge.hashCode(), new EdgeLabel(field));
    }

    public boolean isAnonymousClassEdge(Edge edge) {
        if (this.edgeLabels.containsKey(edge.hashCode())) {
            return this.edgeLabels.get(edge.hashCode()).isAny();
        } else {
            return false;
        }
    }

    public void addActualParameterNode(Unit unit) {
        this.actualParameterNodes.add(unit);
    }

    private static class EdgeLabel {
        private SootField field;
        private boolean any;

        EdgeLabel() {
            this.field = null;
            this.any = true;
        }

        EdgeLabel(SootField field) {
            this.field = field;
            this.any = false;
        }

        public SootField getField() {
            return this.field;
        }

        public boolean isAny() {
            return this.any;
        }
    }
}
