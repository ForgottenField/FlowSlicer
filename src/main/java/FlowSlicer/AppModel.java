package FlowSlicer;

import FlowSlicer.GraphStructure.BidirectionalGraph;
import FlowSlicer.Mode.DependenceGraph;
import FlowSlicer.Mode.LifeCycleHandler;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.util.HashMultiMap;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Getter
public class AppModel implements Serializable {
    private static final long serialVersionUID = 1L;
    // basic app info
    private String appName;
    private String appPath;

    // manifest info
    private String mainActivity;
    private String manifestString;
    private String packageName;
    private Set<String> permissionSet;
    private List<String> components;
    private List<String> activities;
    private List<String> services;
    private List<String> providers;
    private List<String> receivers;

    // dependency graph related
    private int nodesBeforeSlicing;
    private int nodesAfterSlicing;
    private int methodsBeforeSlicing;
    private int methodsAfterSlicing;
    private int sootClassNum;
    private int sootMethodNum;
    private int globalStmtNum;
    private ConcurrentHashMap<String, List<Unit>> methodToSourceAPIsMap;
    private ConcurrentHashMap<String, List<Unit>> methodToSinkAPIsMap;
    private ConcurrentHashMap<Unit, String> sourceAPIToMethodMap;
    private ConcurrentHashMap<Unit, String> sinkAPIToMethodMap;
    private List<Unit> targetListTo;
    private List<Unit> targetListFrom;
    private Set<Unit> toCriteriaSet;
    private Set<Unit> fromCriteriaSet;
    private Multimap<Unit, Unit> sinkToSourceUnitMultimap;
    private Multimap<Unit, Unit> sourceToSinkUnitMultimap;
    private Multimap<String, String> virtualEdgesMultiMap;
    private Set<String> inputClassSet;
    private HashSet<String> globalSootClassSet;
    private HashSet<String> localSootClassSet;
    private HashSet<String> globalSootMethodSet;
    private HashSet<String> supportClassSet;
    private HashSet<String> supportMethodSet;

    // Call Graph related
    private LifeCycleHandler lifeCycleHandler;
    private BidirectionalGraph<String> callGraphBidirectional;
    private BidirectionalGraph<Integer> sccGraphBidirectional;
    private Map<Integer, Integer> independentPermutation;
    private List<Integer> sccGraphTopologicalOrder;
    private List<Integer> sccGraphReverseTopologicalOrder;
    private CallGraph chaCallGraph;

    // Slicing related
    private DependenceGraph sdg;
    private DependenceGraph sdgSliced;
    private Set<Unit> sinkUnitBackwardSlice;
    private ConcurrentHashMap<Unit, Set<Unit>> sliceUnitToSinkUnitMap;
    private ConcurrentHashSet<Unit> sourceUnitForwardSlice;

    public AppModel() {
        appName = Config.getInstance().getAppName();
        appPath = Config.getInstance().getAppPath();
        mainActivity = "";
        manifestString = "";
        packageName = "";
        permissionSet = new HashSet<>();
        components = new ArrayList<>();
        activities = new ArrayList<>();
        services = new ArrayList<>();
        providers = new ArrayList<>();
        receivers = new ArrayList<>();
        nodesBeforeSlicing = 0;
        nodesAfterSlicing = 0;
        methodsBeforeSlicing = 0;
        methodsAfterSlicing = 0;
        sootClassNum = 0;
        sootMethodNum = 0;
        globalStmtNum = 0;
        methodToSinkAPIsMap = new ConcurrentHashMap<>();
        methodToSourceAPIsMap = new ConcurrentHashMap<>();
        sourceAPIToMethodMap = new ConcurrentHashMap<>();
        sinkAPIToMethodMap = new ConcurrentHashMap<>();
        targetListFrom = new ArrayList<>();
        targetListTo = new ArrayList<>();
        toCriteriaSet = new HashSet<>();
        fromCriteriaSet = new HashSet<>();
        sinkToSourceUnitMultimap = HashMultimap.<Unit, Unit>create();
        sourceToSinkUnitMultimap = HashMultimap.<Unit, Unit>create();
        virtualEdgesMultiMap = HashMultimap.<String, String>create();
        inputClassSet = new HashSet<>();
        globalSootClassSet = new HashSet<>();
        localSootClassSet = new HashSet<>();
        globalSootMethodSet = new HashSet<>();
        supportClassSet = new HashSet<>();
        supportMethodSet = new HashSet<>();
        lifeCycleHandler = null;
        callGraphBidirectional = null;
        sccGraphBidirectional = null;
        independentPermutation = null;
        sccGraphTopologicalOrder = null;
        sccGraphReverseTopologicalOrder = null;
        chaCallGraph = new CallGraph();
        sdg = null;
        sdgSliced  = null;
        sinkUnitBackwardSlice = new HashSet<>();
        sourceUnitForwardSlice = new ConcurrentHashSet<>();
        sliceUnitToSinkUnitMap = new ConcurrentHashMap<>();
    }

    public void setMainActivity (String mainActivity) {
        this.mainActivity = mainActivity;
    }

    public void setManifestString (String manifestString) {
        this.manifestString = manifestString;
    }

    public void setPackageName (String packageName) {
        this.packageName = packageName;
    }

    public void setComponents (List<String> components) {
        this.components = components;
    }

    public void setNodesBeforeSlicing (int nodesBeforeSlicing){
        this.nodesBeforeSlicing = nodesBeforeSlicing;
    }

    public void setNodesAfterSlicing (int nodesAfterSlicing){
        this.nodesAfterSlicing = nodesAfterSlicing;
    }

    public void setMethodsBeforeSlicing (int methodsBeforeSlicing){
        this.methodsBeforeSlicing = methodsBeforeSlicing;
    }

    public void setMethodsAfterSlicing (int methodsAfterSlicing){
        this.methodsAfterSlicing = methodsAfterSlicing;
    }

    public void setSootClassNum (int sootClassNum){
        this.sootClassNum = sootClassNum;
    }

    public void setSootMethodNum (int sootMethodNum){
        this.sootMethodNum = sootMethodNum;
    }

    public void setGlobalStmtNum (int globalStmtNum) {
        this.globalStmtNum = globalStmtNum;
    }

    public void setTargetListTo (List<Unit> targetListTo) {
        this.targetListTo = targetListTo;
    }

    public void setTargetListFrom (List<Unit> targetListFrom) {
        this.targetListFrom = targetListFrom;
    }

    public void setToCriteriaSet (Set<Unit> toCriteriaSet) {
        this.toCriteriaSet = toCriteriaSet;
    }

    public void setFromCriteriaSet (Set<Unit> fromCriteriaSet) {
        this.fromCriteriaSet = fromCriteriaSet;
    }
    public void setSinkToSourceUnitMultimap (Multimap<Unit, Unit> sinkToSourceUnitMultimap) {
        this.sinkToSourceUnitMultimap = sinkToSourceUnitMultimap;
    }

    public void setSourceToSinkUnitMultimap (Multimap<Unit, Unit> sourceToSinkUnitMultimap) {
        this.sourceToSinkUnitMultimap = sourceToSinkUnitMultimap;
    }

    public void setLocalSootClassSet (HashSet<String> localSootClassSet) {
        this.localSootClassSet = localSootClassSet;
    }

    public void setLifeCycleHandler (LifeCycleHandler lifeCycleHandler) {
        this.lifeCycleHandler = lifeCycleHandler;
    }

    public void setCallGraphBidirectional (BidirectionalGraph<String> callGraphBidirectional) {
        this.callGraphBidirectional = callGraphBidirectional;
    }

    public void setSccGraphBidirectional (BidirectionalGraph<Integer> sccGraphBidirectional) {
        this.sccGraphBidirectional = sccGraphBidirectional;
    }

    public void setIndependentPermutation (Map<Integer, Integer> independentPermutation) {
        this.independentPermutation = independentPermutation;
    }

    public void setSccGraphTopologicalOrder (List<Integer> sccGraphTopologicalOrder) {
        this.sccGraphTopologicalOrder = sccGraphTopologicalOrder;
    }

    public void setSccGraphReverseTopologicalOrder (List<Integer> sccGraphReverseTopologicalOrder) {
        this.sccGraphReverseTopologicalOrder = sccGraphReverseTopologicalOrder;
    }

    public void setSdg (DependenceGraph sdg) {
        this.sdg = sdg;
    }

    public void setSdgSliced (DependenceGraph sdgSliced) {
        this.sdgSliced = sdgSliced;
    }
}
