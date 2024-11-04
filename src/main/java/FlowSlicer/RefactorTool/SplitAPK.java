package FlowSlicer.RefactorTool;

import FlowSlicer.AppModel;
import FlowSlicer.Config;
import FlowSlicer.Global;
import FlowSlicer.GraphStructure.BidirectionalGraph;
import FlowSlicer.Mode.*;
import FlowSlicer.Statistics;
import FlowSlicer.XMLObject.Reference;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import heros.solver.CountingThreadPoolExecutor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Getter
public abstract class SplitAPK {
    protected AppModel appModel;
    protected Config config;
    protected Packs packs;

    public SplitAPK(){
        this.appModel = Global.v().getAppModel();
        this.config = Config.getInstance();
        this.packs = Packs.getInstance();
    }

    public abstract void splitApk() throws IOException;

    public Integer handleInteger() {
        throw new RuntimeException();
    }

    public void handleExceptions() {
        log.info("Handling Exceptions");
        HashSet<String> includedClassSet = (Config.getInstance().isPartialSDGConstruction()) ? appModel.getLocalSootClassSet() : appModel.getGlobalSootClassSet();

        // Explicitly handling exceptions thrown
        for (final SootClass sc : Scene.v().getApplicationClasses()) {
            if (includedClassSet.contains(sc.getName()) && !SootHelper.isOrdinaryLibraryClass(sc)) {
                final List<SootMethod> snapshotIterator = new ArrayList<>(sc.getMethods());
                for (final SootMethod sm : snapshotIterator) {
                    if (sm.isConcrete()) {
                        final DependenceGraph pdg = Packs.getInstance().getPDG(sm);
                        final Body b = sm.retrieveActiveBody();
                        // Get Targets
                        for (final Trap t : b.getTraps()) {
                            if (t.getHandlerUnit() instanceof IdentityStmt) {
                                final IdentityStmt target = (IdentityStmt) t.getHandlerUnit();
                                if (target.getRightOp() instanceof CaughtExceptionRef) {
                                    final Type targetType = target.getLeftOp().getType();

                                    // Add edges
                                    for (final Unit unit : b.getUnits()) {
                                        if (unit instanceof InvokeStmt) {
                                            final InvokeStmt castedUnit = (InvokeStmt) unit;
                                            if (includedClassSet.contains(castedUnit.getInvokeExpr().getMethod().getDeclaringClass().getName())) {
                                                final DependenceGraph pdgOfCalledMethod = Packs.getInstance().getPDG(castedUnit.getInvokeExpr().getMethod());
                                                if (pdgOfCalledMethod != null && pdgOfCalledMethod.getExceptionsThrown(targetType) != null) {
                                                    for (final Unit thrownBy : pdgOfCalledMethod.getExceptionsThrown(targetType)) {
                                                        final Edge temp = new Edge(thrownBy, target, DependenceGraph.EDGE_TYPE_EXCEPTION);
                                                        pdg.addEdge(temp);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void buildDummyMainPDG() {
        // Build DummyMain PDG
        log.info("Constructing DummyMain PDG");

        appModel.setLifeCycleHandler(new LifeCycleHandler(appModel.getAppPath()));
        for (final SootMethod dummyMain : appModel.getLifeCycleHandler().getDummyMainMethods()) {
            log.info("Building PDG for DummyMainMethods");
            final DependenceGraph pdg = PDGHelper.getPDG(dummyMain.getActiveBody());
            Packs.getInstance().addPdg(dummyMain, pdg);
        }
    }

    public void addCallBackEdges() {
        log.info("Adding Callbacks to PDGs");

        if (appModel.getLifeCycleHandler() != null && Packs.getInstance().getCallBackClasses() != null && !Packs.getInstance().getCallBackClasses().isEmpty()) {
            for (final DependenceGraph pdg : Packs.getInstance()) {
                final Set<Edge> toAdd = new HashSet<>();
                for (final Unit useUnit : pdg) {
                    if (useUnit instanceof Stmt) {
                        final Stmt castedUseUnit = (Stmt) useUnit;
                        if (castedUseUnit.containsInvokeExpr()) {
                            final InvokeExpr invoke = castedUseUnit.getInvokeExpr();
                            for (int i = 0; i < invoke.getMethod().getParameterCount(); i++) {
                                if (Packs.getInstance().getCallBackClasses().contains(invoke.getMethod().getParameterType(i).toString())) {
                                    final Value useValue = invoke.getArg(i);
                                    if (SootHelper.isCallBackClass(useValue)) {
                                        // Step 1:
                                        for (final SootMethod sm : SootHelper.getAllAccessibleMethods(useValue.getType())) {
                                            if (SootHelper.isCallBackMethod(sm) && !Packs.getInstance().getEntryNodes(sm).isEmpty()) {
                                                final Set<Unit> entryNodes = Packs.getInstance().getEntryNodes(sm);
//                                                if (entryNodes.size() > 1) {
//                                                    log.info("Over-approximating due to " + entryNodes.size()
//                                                            + " callback candidates when dealing with: " + useUnit
//                                                            + " (Method: " + pdg.getMethod() + ")");
//                                                }
                                                for (final Unit entryNode : entryNodes) {
                                                    toAdd.add(new Edge(useUnit, entryNode, DependenceGraph.EDGE_TYPE_CALLBACK));
                                                }
                                                for (final Unit entryNode1 : entryNodes) {
                                                    for (final Unit entryNode2 : entryNodes) {
                                                        if (entryNode1 != entryNode2) {
                                                            toAdd.add(new Edge(entryNode1, entryNode2, DependenceGraph.EDGE_TYPE_CALLBACK_ALTERNATIVE));
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Step 2:
                                        Set<Unit> defUnits = null;
                                        for (final ReachingDefinition rd : Packs.getInstance().getDefValuesMap().get(useUnit)) {
                                            if (rd.getValueOrField().equals(new ValueOrField(useValue))) {
                                                defUnits = rd.getUnits();
                                                break;
                                            }
                                        }
                                        if (defUnits != null) {
                                            for (final Unit defUnit : defUnits) {
                                                toAdd.add(new Edge(useUnit, defUnit, DependenceGraph.EDGE_TYPE_CALLBACK_DEFINITION));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                for (final Edge edge : toAdd) {
                    pdg.addEdge(edge);
                }
            }
        }
    }

    public void findSlicingTargets(List<Reference> targetFromList, List<Reference> targetToList, Map<Reference, List<Reference>> sinkToSourcesMap) {
        Map<Reference, Unit> referenceUnitMap = new HashMap<>();
        Collection<Local> localList = new ArrayList<>();
        Map<String, String> localValueMap = new HashMap<>();

        //由于每个<statement, method, class>三元组可以唯一确定一个Unit，因此用数组大小相等的关系控制findSlicingTargets的结束逻辑。
        for (final SootClass sc : Scene.v().getApplicationClasses()) {
            if (!SootHelper.isOrdinaryLibraryClass(sc)) {
                for (final SootMethod sm : sc.getMethods()) {
                    final Body body = SootHelper.getActiveBodyIfMethodExists(sm);
                    if (body == null) {
                        continue;
                    }
                    // 增加匹配逻辑，解决JIMPLE字节码不匹配的问题
                    localList.addAll(body.getLocals());
                    for (final Unit unit : body.getUnits()) {
                        if (unit instanceof SwitchStmt || unit instanceof GotoStmt || unit instanceof IfStmt) {
                            continue;
                        }
                        if (unit instanceof JAssignStmt) {
                            Value left = ((JAssignStmt) unit).getLeftOp();
                            Value right = ((JAssignStmt) unit).getRightOp();
                            for (Local local : localList) {
                                if (local.getName().equals(String.valueOf(left)) && right instanceof Constant) {
                                    localValueMap.put(local.getName(), String.valueOf(right));
                                }
                            }
                        }
                        for (Reference targetFrom : targetFromList) {
                            if (isFromOrTo(targetFrom, unit, sm, sc, localValueMap)) {
                                Unit targetFromUnit = Packs.getInstance().getReplacedNodesOriginalToReplacement().get(unit);
                                if (targetFromUnit == null) {
                                    targetFromUnit = unit;
                                }
                                appModel.getTargetListFrom().add(targetFromUnit);
                                referenceUnitMap.put(targetFrom, targetFromUnit);
                                break;
                            }
                        }

                        for (Reference targetTo : targetToList) {
                            if (isFromOrTo(targetTo, unit, sm, sc, localValueMap)) {
                                Unit targetToUnit = Packs.getInstance().getReplacedNodesOriginalToReplacement().get(unit);
                                if (targetToUnit == null) {
                                    targetToUnit = unit;
                                }
                                appModel.getTargetListTo().add(targetToUnit);
                                referenceUnitMap.put(targetTo, targetToUnit);
                                break;
                            }
                        }
                    }
                    localList.clear();
                    localValueMap.clear();
                    if (targetFromList.size() == appModel.getTargetListFrom().size() && targetToList.size() == appModel.getTargetListTo().size()){
                        break;
                    }
                }
            }
            if (targetFromList.size() == appModel.getTargetListFrom().size() && targetToList.size() == appModel.getTargetListTo().size()){
                break;
            }
        }

        Set<Unit> toCriteriaSet = new HashSet<>(appModel.getTargetListTo());
        Set<Unit> fromCriteriaSet = new HashSet<>(appModel.getTargetListFrom());
        appModel.setToCriteriaSet(toCriteriaSet);
        appModel.setFromCriteriaSet(fromCriteriaSet);

        // 记录额外信息，将Sink与Sources进行配对
        for (Reference sinkReference : sinkToSourcesMap.keySet()){
            Unit sinkUnit = referenceUnitMap.get(sinkReference);
            if (sinkUnit != null) {
                for (Reference sourceReference : sinkToSourcesMap.get(sinkReference)){
                    Unit sourceUnit = referenceUnitMap.get(sourceReference);
                    if (sourceUnit != null) {
                        appModel.getSinkToSourceUnitMultimap().put(sinkUnit, sourceUnit);
                        appModel.getSourceToSinkUnitMultimap().put(sourceUnit, sinkUnit);
                    }
                }
            }
        }
    }

    public boolean isFromOrTo(Reference fromOrTo, Unit unit, SootMethod sm, SootClass sc, Map<String, String> localValueMap) {
        if (fromOrTo != null && ((Stmt)unit).containsInvokeExpr()) {
            String unitMethod = ((Stmt)unit).getInvokeExpr().getMethodRef().toString();
            Boolean isAssignment = false;
            if (unit instanceof AssignStmt) {
               isAssignment = true;
            }
//            String leftValue = (isAssignment) ? ((AssignStmt) unit).getLeftOpBox().getValue().toString() : null;
//            String baseInvoker = null;
//            if (!((Stmt) unit).getInvokeExpr().getUseBoxes().isEmpty()) {
//                baseInvoker= ((Stmt)unit).getInvokeExpr().getUseBoxes().get(0).getValue().toString();
//            }

            List<String> unitParamsList = new ArrayList<>();
            List<String> replacedUnitParamsList = new ArrayList<>();
            for (Value value : ((Stmt) unit).getInvokeExpr().getArgs()) {
                unitParamsList.add(value.toString());
            }
            String unitParams = unitParamsList.toString();
            if (unitMethod.equals(fromOrTo.getStatementMethod()) && !unitParams.equals(fromOrTo.getStatementParams().toString())) {
                for (String param : unitParamsList) {
                    replacedUnitParamsList.add(localValueMap.getOrDefault(param, param));
                }
                unitParams = replacedUnitParamsList.toString();
            }
//
//            if (isAssignment.equals(fromOrTo.getIsAssignment()) &&
//                    ((isAssignment.equals(true) && leftValue.equals(fromOrTo.getStatementLeftBox())) || isAssignment.equals(false)) &&
//                    (baseInvoker != null && baseInvoker.equals(fromOrTo.getStatementInvoker())) &&
//                    unitMethod.equals(fromOrTo.getStatementMethod()) &&
//                    unitParams.equals(fromOrTo.getStatementParams().toString()))
//            {
//                return sm.getSubSignature().equals(fromOrTo.getMethod()) && sc.toString().equals(fromOrTo.getClassname());
//            }
            if (isAssignment.equals(fromOrTo.getIsAssignment()) && unitMethod.equals(fromOrTo.getStatementMethod()) && unitParams.equals(fromOrTo.getStatementParams().toString()))
            {
                return sm.getSubSignature().equals(fromOrTo.getMethod()) && sc.toString().equals(fromOrTo.getClassname());
            }
        }
        return false;
    }

    public DependenceGraph sliceSDG(DependenceGraph sdg) {
        log.info("Slicing sdg.");
        // Update targets
        Multimap<Unit, Unit> replacedSinkToSourceMap = HashMultimap.<Unit, Unit>create();
        Multimap<Unit, Unit> replacedSourceToSinkMap = HashMultimap.<Unit, Unit>create();
        Set<Unit> replacedTargetListFrom = new HashSet<>();
        Set<Unit> replacedTargetListTo = new HashSet<>();
        if (!appModel.getFromCriteriaSet().isEmpty()) {
            for (Unit sourceUnit : appModel.getFromCriteriaSet()) {
                Unit source = Packs.getInstance().getReplacedNodesOriginalToReplacement().getOrDefault(sourceUnit, sourceUnit);
                replacedTargetListFrom.add(source);
            }
        }
        appModel.setFromCriteriaSet(replacedTargetListFrom);

        if (!appModel.getToCriteriaSet().isEmpty()) {
            for (Unit sinkUnit : appModel.getToCriteriaSet()) {
                Unit sink = Packs.getInstance().getReplacedNodesOriginalToReplacement().getOrDefault(sinkUnit, sinkUnit);
                replacedTargetListTo.add(sink);
            }
        }
        appModel.setToCriteriaSet(replacedTargetListTo);


        if (!appModel.getSinkToSourceUnitMultimap().isEmpty()) {
            for (Unit sinkUnit : appModel.getSinkToSourceUnitMultimap().keySet()) {
                Unit sink = Packs.getInstance().getReplacedNodesOriginalToReplacement().getOrDefault(sinkUnit, sinkUnit);
                for (Unit sourceUnit : appModel.getSinkToSourceUnitMultimap().get(sinkUnit)) {
                    Unit source = Packs.getInstance().getReplacedNodesOriginalToReplacement().getOrDefault(sourceUnit, sourceUnit);
                    replacedSinkToSourceMap.put(sink, source);
                }
            }
        }
        appModel.setSinkToSourceUnitMultimap(replacedSinkToSourceMap);

        if (!appModel.getSourceToSinkUnitMultimap().isEmpty()) {
            for (Unit sourceUnit : appModel.getSourceToSinkUnitMultimap().keySet()) {
                Unit source = Packs.getInstance().getReplacedNodesOriginalToReplacement().getOrDefault(sourceUnit, sourceUnit);
                for (Unit sinkUnit : appModel.getSourceToSinkUnitMultimap().get(sourceUnit)) {
                    Unit sink = Packs.getInstance().getReplacedNodesOriginalToReplacement().getOrDefault(sinkUnit, sinkUnit);
                    replacedSourceToSinkMap.put(source, sink);
                }
            }
        }
        appModel.setSourceToSinkUnitMultimap(replacedSourceToSinkMap);

        // Slicing SDG
        Statistics.getTimer(Statistics.TIMER_Slicing_SDG_Backward_And_Forward).start();
        log.info("Slicing SDG Backward and Forward.");
        DependenceGraph sdgSliced = sdg.clone();
        sliceBackwardAndForward(sdgSliced);
        log.info("Slicing SDG Backward and Forward Finished!");
        Statistics.getTimer(Statistics.TIMER_Slicing_SDG_Backward_And_Forward).stop();

        // considering Context-Sensitive Refinement(CSR)
        // 变量contextSensitiveRefined中包含了所有根据CSR原则被切除的语句
        Set<Unit> contextSensitiveRefined = null;
        Statistics.getTimer(Statistics.TIMER_Slicing_SDG_CSR).start();
        if (Config.getInstance().isBackwardFinish() || Config.getInstance().isBackwardAndForwardFinish()) {
            contextSensitiveRefined = SliceExtension.contextSensitiveRefinement(sdgSliced,
                    appModel.getSinkToSourceUnitMultimap().keySet(),
                    appModel.getSourceUnitForwardSlice());
        }
        Statistics.getTimer(Statistics.TIMER_Slicing_SDG_CSR).stop();

        // Slicing SDG (slice mode only)
        final Set<Unit> toKeep = new HashSet<>(sdgSliced.getAllNodes());
        Statistics.getTimer(Statistics.TIMER_Slicing_SDG_Extra).start();
        if (Config.getInstance().isBackwardFinish() || Config.getInstance().isBackwardAndForwardFinish()) {
            sliceExtras(sdg, toKeep);
            final DependenceGraph sdgSlicedComplete = sdg.clone();
            sdgSlicedComplete.removeNodes(sdgSlicedComplete.notIn(toKeep));
            sdgSliced = sdgSlicedComplete;
            appModel.setSdgSliced(sdgSliced);
        }
        Statistics.getTimer(Statistics.TIMER_Slicing_SDG_Extra).stop();

        // Draw slicing SDG
        if (Config.getInstance().isDrawGraph()) {
            final GraphDrawer gdSDGsliced = new GraphDrawer("SDG", sdg, sdgSliced, appModel.getFromCriteriaSet(), appModel.getToCriteriaSet());
            gdSDGsliced.drawGraph(Config.getInstance().getResultFolder() + Config.getInstance().getAppName() + File.separator + "draw" + File.separator + Config.getInstance().getAppName(), 0);
        }

        return sdgSliced;
    }

    public void sliceBackwardAndForward(DependenceGraph sdgSliced) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        // 执行splitApk方法中耗时的操作
//        Future<?> future = executor.submit(this::sliceBackwardAndForwardInternal);
        Future<?> future = executor.submit(() -> {
            sliceBackwardAndForwardInternal(sdgSliced);
        });

        try {
            future.get(Config.getInstance().getBackward_forward_timeout_sec(), TimeUnit.SECONDS);  // 等待执行完成或超时
        } catch (TimeoutException e) {
            future.cancel(true);  // 超时后尝试取消任务
            log.error("Task timed out");
        } catch (ExecutionException e) {
            // 处理任务执行中的异常
            log.error("Task failed with exception: " + e.getMessage());
        } catch (InterruptedException e) {
            // 处理任务被中断的情况
            Thread.currentThread().interrupt();  // 恢复中断状态
        } finally {
            executor.shutdownNow();  // 确保任务最终被终止
        }
    }

    public void sliceBackwardAndForwardInternalConcurrently(DependenceGraph sdgSliced) {
        Set<Unit> sinkUnits = new HashSet<>(appModel.getSinkToSourceUnitMultimap().keySet());
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        ConcurrentHashMap<Unit, Boolean> toSliceMap = new ConcurrentHashMap<>();
        final SDGBackwardsSlicer slicer = new SDGBackwardsSlicer(sdgSliced, sinkUnits);

        executor.submit(() -> {
            Set<Unit> sliceResult = slicer.slice();
            for (Unit u : sliceResult) {
                toSliceMap.put(u, Boolean.TRUE);
            }
        });

        sdgSliced.removeNodes(sdgSliced.notIn(toSliceMap.keySet()));
        appModel.setSdgSliced(sdgSliced);
        Config.getInstance().setIsBackwardFinish(true);

        Set<Unit> entryPointSet = findEntryPoints(sdgSliced);
        Set<Unit> sourceUnits = new HashSet<>(appModel.getSourceToSinkUnitMultimap().keySet());
        sourceUnits.addAll(entryPointSet);
        ConcurrentHashMap<Unit, Boolean> fromSliceMap = new ConcurrentHashMap<>();
        SDGForwardSlicer slicerWrtFields = new SDGForwardSlicer(sdgSliced, sourceUnits);

        executor.submit(() -> {
            Set<Unit> sliceResult = slicerWrtFields.slice();
            for (Unit u : sliceResult) {
                fromSliceMap.put(u, Boolean.TRUE);
            }
        });

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }

        sdgSliced.removeNodes(sdgSliced.notIn(fromSliceMap.keySet()));
        appModel.setSdgSliced(sdgSliced);
        Config.getInstance().setIsBackwardAndForwardFinish(true);
    }

    public void sliceBackwardAndForwardInternal(DependenceGraph sdgSliced) {
        Set<Unit> sinkUnits = new HashSet<>(appModel.getSinkToSourceUnitMultimap().keySet());
        final SDGBackwardsSlicer slicer = new SDGBackwardsSlicer(sdgSliced, sinkUnits);
        Set<Unit> toSlice = slicer.slice();
        appModel.getSinkUnitBackwardSlice().addAll(toSlice);
        sdgSliced.removeNodes(sdgSliced.notIn(toSlice));
        appModel.setSdgSliced(sdgSliced);
        Config.getInstance().setIsBackwardFinish(true);

        Set<Unit> entryPointSet = findEntryPoints(sdgSliced);
        Set<Unit> sourceUnits = new HashSet<>(appModel.getSourceToSinkUnitMultimap().keySet());
        sourceUnits.addAll(entryPointSet);
        SDGForwardSlicer slicerWrtFields = new SDGForwardSlicer(sdgSliced, sourceUnits);
        Set<Unit> fromSlice = new HashSet<>(slicerWrtFields.slice());
        appModel.getSourceUnitForwardSlice().addAll(fromSlice);
        sdgSliced.removeNodes(sdgSliced.notIn(fromSlice));
        appModel.setSdgSliced(sdgSliced);
        Config.getInstance().setIsBackwardAndForwardFinish(true);
    }

    public void sliceExtras(DependenceGraph sdg, Set<Unit> toKeep) {
        log.info("Extra Slices required?");
        final Set<Unit> toSliceExtra = new HashSet<>();
        final Set<Unit> visited = new HashSet<>();
        for (final Unit unit : toKeep) {
            Set<ReachingDefinition> rds = Packs.getInstance().getDefValuesMap().get(unit);
            if (rds == null && Packs.getInstance().getReplacedNodesReplacementToOriginal().get(unit) != null) {
                rds = Packs.getInstance().getDefValuesMap()
                        .get(Packs.getInstance().getReplacedNodesReplacementToOriginal().get(unit));
            }
            if (!visited.contains(unit) && rds != null) {
                visited.add(unit);

                // Get all used values
                final Set<ValueOrField> useValues = new HashSet<>();
                for (final ValueBox vb : unit.getUseBoxes()) {
                    final Value v = vb.getValue();
                    if ((v instanceof Local || v instanceof Ref)
                            && !(v instanceof ParameterRef || v instanceof ThisRef)) {
                        useValues.add(new ValueOrField(v));
                    }
                }
                if (unit instanceof Stmt) {
                    final Stmt castedUnit = (Stmt) unit;
                    if (castedUnit.containsInvokeExpr()) {
                        if (castedUnit.getInvokeExpr() instanceof InstanceInvokeExpr) {
                            useValues
                                    .add(new ValueOrField(((InstanceInvokeExpr) castedUnit.getInvokeExpr()).getBase()));
                        }
                    }
                }

                // Is defined?
                for (final ValueOrField useValue : useValues) {
                    boolean found = false;
                    for (final ReachingDefinition rd : rds) {
                        if (rd.getValueOrField().equals(useValue)) {
                            for (Unit defUnit : rd.getUnits()) {
                                if (defUnit != null) {
                                    if (Packs.getInstance().getReplacedNodesOriginalToReplacement()
                                            .containsKey(defUnit)) {
                                        defUnit = Packs.getInstance().getReplacedNodesOriginalToReplacement()
                                                .get(defUnit);
                                    }
                                    if (!toKeep.contains(defUnit)) {
                                        toSliceExtra.add(unit);
                                        found = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (found) {
                        break;
                    }
                }
            }
        }

        for (Unit unit : sdg) {
            if (unit instanceof Stmt && ((Stmt) unit).containsInvokeExpr() && ((Stmt) unit).getInvokeExpr() instanceof InstanceInvokeExpr)
            {
                final InstanceInvokeExpr invoke = (InstanceInvokeExpr) ((Stmt) unit).getInvokeExpr();

                if (invoke.getMethodRef().getName().equals("<init>") ||
                        invoke.getMethodRef().getName().equals("<clinit>") ||
                        SootHelper.isCallBackMethod(invoke.getMethodRef()))
                    toSliceExtra.add(unit);

                if (SootHelper.hasActivitySuperClass(invoke.getMethodRef().getDeclaringClass()))
                {
                    if (invoke.getMethodRef().getName().equals("setContentView"))
                    {
                        toSliceExtra.add(unit);
                    }
                }
            }
        }

        if (!toSliceExtra.isEmpty()) {
            final SliceExtension se = new SliceExtension(sdg, true, toKeep, toSliceExtra);
            se.sliceIncorparatedly();
        }
        Config.getInstance().setIsExtraFinish(true);
    }

    public void sliceExtra(DependenceGraph sdg, Set<Unit> toKeep) {
        log.info("Extra Slices required?");
        final Set<Unit> toSliceExtra = new HashSet<>();
        for (Unit unit : sdg) {
            if (unit instanceof Stmt && ((Stmt) unit).containsInvokeExpr() && ((Stmt) unit).getInvokeExpr() instanceof InstanceInvokeExpr)
            {
                final InstanceInvokeExpr invoke = (InstanceInvokeExpr) ((Stmt) unit).getInvokeExpr();

                if (SootHelper.hasActivitySuperClass(invoke.getMethodRef().getDeclaringClass()))
                {
                    if (invoke.getMethodRef().getName().equals("setContentView"))
                    {
                        toSliceExtra.add(unit);
                    }

                    if (invoke.getMethodRef().getName().equals("<init>") ||
                            invoke.getMethodRef().getName().equals("<clinit>") ||
                            SootHelper.isCallBackMethod(invoke.getMethodRef()))
                        toSliceExtra.add(unit);
                }
            }
        }

//        final Set<Unit> visited = new HashSet<>();
//        for (final Unit unit : toSliceExtra) {
//            Set<ReachingDefinition> rds = Packs.getInstance().getDefValuesMap().get(unit);
//            if (rds == null && Packs.getInstance().getReplacedNodesReplacementToOriginal().get(unit) != null) {
//                rds = Packs.getInstance().getDefValuesMap()
//                        .get(Packs.getInstance().getReplacedNodesReplacementToOriginal().get(unit));
//            }
//            if (!visited.contains(unit) && rds != null) {
//                visited.add(unit);
//
//                // Get all used values
//                final Set<ValueOrField> useValues = new HashSet<>();
//                for (final ValueBox vb : unit.getUseBoxes()) {
//                    final Value v = vb.getValue();
//                    if ((v instanceof Local || v instanceof Ref)
//                            && !(v instanceof ParameterRef || v instanceof ThisRef)) {
//                        useValues.add(new ValueOrField(v));
//                    }
//                }
//                if (unit instanceof Stmt) {
//                    final Stmt castedUnit = (Stmt) unit;
//                    if (castedUnit.containsInvokeExpr()) {
//                        if (castedUnit.getInvokeExpr() instanceof InstanceInvokeExpr) {
//                            useValues
//                                    .add(new ValueOrField(((InstanceInvokeExpr) castedUnit.getInvokeExpr()).getBase()));
//                        }
//                    }
//                }
//
//                // Is defined?
//                for (final ValueOrField useValue : useValues) {
//                    for (final ReachingDefinition rd : rds) {
//                        if (rd.getValueOrField().equals(useValue)) {
//                            for (Unit defUnit : rd.getUnits()) {
//                                if (defUnit != null) {
//                                    if (Packs.getInstance().getReplacedNodesOriginalToReplacement()
//                                            .containsKey(defUnit)) {
//                                        defUnit = Packs.getInstance().getReplacedNodesOriginalToReplacement()
//                                                .get(defUnit);
//                                    }
//                                    toKeep.add(defUnit);
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }

        if (!toSliceExtra.isEmpty()) {
            final SliceExtension se = new SliceExtension(sdg, true, toKeep, toSliceExtra);
            se.sliceIncorparatedly();
        }
        Config.getInstance().setIsExtraFinish(true);
    }

    public void sliceCFG(DependenceGraph sdgSliced, DependenceGraph sdg) {
        final CFGSlicer cfgSlicer = new CFGSlicer(sdgSliced, sdg);

        // Slice CFG
        cfgSlicer.slice();

        // Remove dummy main
        if (appModel.getLifeCycleHandler().getDummyMainClass() != null) {
            log.info("Removing dummy main class");
            Scene.v().getApplicationClasses().remove(appModel.getLifeCycleHandler().getDummyMainClass());
        }

        // Add support classes if excluded
        reAddSupportClasses();
    }

    public void sliceCFGWithoutSDG() {
        final CFGSlicer cfgSlicer = new CFGSlicer();

        // Slice CFG
        cfgSlicer.sliceWithoutSDG();

        // Remove dummy main
        if (appModel.getLifeCycleHandler().getDummyMainClass() != null) {
            log.info("Removing dummy main class");
            Scene.v().getApplicationClasses().remove(appModel.getLifeCycleHandler().getDummyMainClass());
        }

        // Add support classes if excluded
        reAddSupportClasses();
    }

    public void reAddSupportClasses() {
        // Load required support classes
        log.info("Loading required support classes");
        for (String className : appModel.getSupportClassSet()) {
            try {
                final SootClass sc = Scene.v().loadClassAndSupport(className);
                sc.setApplicationClass();
                for (SootMethod sm : sc.getMethods()) {
                    if (sm.isConcrete()) {
                        sm.retrieveActiveBody();
                    }
                }
            } catch (final Exception e) {
                log.error("Could not load required support class: " + className);
            }
        }

//        final Set<String> requiredSupportClasses = new HashSet<>();
//        final Set<String> alreadyApplicationClasses = new HashSet<>();
//        for (final SootClass sc : Scene.v().getApplicationClasses()) {
//            alreadyApplicationClasses.add(sc.getName());
//            for (final SootClass suppClass : SootHelper.getAllAccessibleClassesAndInterfaces(sc)) {
//                requiredSupportClasses.add(suppClass.getName());
//            }
//        }
//        for (final String className : requiredSupportClasses) {
//            if (className.startsWith("android.support.")) {
//                if (!alreadyApplicationClasses.contains(className)) {
//                    try {
//                        final SootClass sc = Scene.v().loadClassAndSupport(className);
//                        sc.setApplicationClass();
//                        for (SootMethod sm : sc.getMethods()) {
//                            if (sm.isConcrete()) {
//                                sm.retrieveActiveBody();
//                            }
//                        }
//                    } catch (final Exception e) {
//                        log.error("Could not load required support class: " + className);
//                    }
//                }
//            }
//        }

        // Load required layout classes
//        if (isRunnable) {
//            log.info("Find and parse layout files");
//
//            // Find layout files
//            final Set<String> layoutFiles = new HashSet<>();
//            try (ZipFile zipFile = new ZipFile(inputAPKFile)) {
//                final Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
//                while (zipEntries.hasMoreElements()) {
//                    final String fileName = zipEntries.nextElement().getName();
//                    if (fileName.startsWith("res/layout") && fileName.endsWith(".xml")) {
//                        layoutFiles.add(fileName);
//                    }
//                }
//            } catch (final IOException e) {
//                log.error("Could not read zipped content of APK file: " + appModel.getAppPath());
//            }
//
//            // Parse layout files
//            final ApkFile apkFile = ManifestHelper.getInstance().getApkParserFile(inputAPKFile);
//            final Set<String> allClassesInLayoutFiles = new HashSet<>();
//            for (final String layoutFile : layoutFiles) {
//                try {
//                    allClassesInLayoutFiles.addAll(findLayoutClasses(apkFile.transBinaryXml(layoutFile)));
//                } catch (final IOException e) {
//                    log.error("Could not parse layout file: " + layoutFile);
//                }
//            }
//
//            log.info("Adding classes required due to layouts");
//            for (final String classname : allClassesInLayoutFiles) {
//                final SootClass sc = Scene.v().loadClassAndSupport(classname);
//                if (!Scene.v().getApplicationClasses().contains(sc)) {
//                    Scene.v().getApplicationClasses().add(sc);
////                    log.info("Adding layout class: " + classname);
//                }
//            }
//        }
    }

    public Set<String> findLayoutClasses(String layoutFileContent) {
        final Pattern pattern = Pattern.compile("(<| )([a-zA-Z])+[.]([a-zA-Z.])+(>|/|\\s)");
        final Matcher matcher = pattern.matcher(layoutFileContent);

        final Set<String> listClassnames = new HashSet<>();
        while (matcher.find()) {
            listClassnames.add(matcher.group().replaceAll("[<>/\\s]", ""));
        }
        return listClassnames;
    }

    public void writeOutput() {
//        checkForEmptyChains();
        PackManager.v().writeOutput();
//        boolean retry = true;
//        int retryCounter = 0;
//        while (retry && retryCounter < 100) {
//            if (retryCounter > 0) {
//                try {
//                    // Decoy until fixed (see: https://github.com/soot-oss/soot/pull/1116)
//                    Thread.sleep(10);
//                } catch (final InterruptedException intEx) {
//                    // Catch to ignore InterruptedException related to Soot's writer (which is thrown about 8 times, if thrown at all)
//                }
//            }
//            try {
//                retry = false;
//                retryCounter++;
//                PackManager.v().writeOutput();
//            } catch (final ConcurrentModificationException conEx) {
//                retry = true;
//                System.out.println(
//                        "Problem while writing output occured (Related to known Soot-Bug: https://github.com/soot-oss/soot/pull/1116). Retrying...");
//            }
//        }
    }

    private static void checkForEmptyChains() {
        // Iterate over all the classes in the Scene
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            List<SootMethod> methodsToRemove = new ArrayList<>();
            for (SootMethod method : sootClass.getMethods()) {
                if (!method.isConcrete()) {
                    continue;
                }

                Body body = method.retrieveActiveBody();
                if (body.getUnits().isEmpty()) {
                    methodsToRemove.add(method); // 标记要删除的方法
                }
            }

            // 从类中移除所有已标记的方法
            for (SootMethod methodToRemove : methodsToRemove) {
                sootClass.removeMethod(methodToRemove);
            }
        }
    }

    public static void writeDotFile(CallGraph cg, String dotFilePath) throws IOException {
        FileWriter writer = new FileWriter(dotFilePath);
        writer.write("digraph callgraph {\n");

        // Set node shape to rectangle
        writer.write("node [shape=rectangle];\n");

        for (soot.jimple.toolkits.callgraph.Edge edge : cg) {
            String src = edge.getSrc().method().getSignature();
            String tgt = edge.getTgt().method().getSignature();
            writer.write("\"" + src + "\" -> \"" + tgt + "\";\n");
        }

        writer.write("}\n");
        writer.close();
    }

    public static void convertDotToSvg(String dotFilePath, String svgFilePath) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("dot", "-Tsvg", dotFilePath, "-o", svgFilePath);
        pb.inheritIO();
        Process p = pb.start();

        try {
            p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void ensureDirectoryExists(String filePath) {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
    }

    public void locateSourceAndSinkUnit(SootMethod method, Stmt stmt) {
        for (String source : Config.getInstance().getSourcesAPIList()) {
            if (source.startsWith("<") && source.endsWith(">")) {
                // for example, <android.location.Location: double getLatitude()>
                if (isSourceOrSinkMatched(source, stmt)) {
                    List<Unit> unitList = appModel.getMethodToSourceAPIsMap().get(method.getSignature());
                    if (unitList == null) {
                        unitList = new ArrayList<>();
                    }
                    unitList.add(stmt);
                    appModel.getMethodToSourceAPIsMap().put(method.getSignature(), unitList);
                    appModel.getSourceAPIToMethodMap().put(stmt, method.getSignature());
                    break;
                }
            } else {
                // for example, <android.telephony.TelephonyManager: java.lang.String getDeviceId()> android.permission.READ_PHONE_STATE
                // Regular expression to match the method signature and permission
                String regex = "<(.*?)>\\s+(.*)";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(source);

                if (matcher.find()) {
                    String methodSignature = matcher.group(1);
                    String permission = matcher.group(2);

                    if (!Config.getInstance().isPermissionConsidered() || appModel.getPermissionSet().contains(permission)) {
                        if (isSourceOrSinkMatched(methodSignature, stmt)) {
                            List<Unit> unitList = appModel.getMethodToSourceAPIsMap().get(method.getSignature());
                            if (unitList == null) {
                                unitList = new ArrayList<>();
                            }
                            unitList.add(stmt);
                            appModel.getMethodToSourceAPIsMap().put(method.getSignature(), unitList);
                            appModel.getSourceAPIToMethodMap().put(stmt, method.getSignature());
                            break;
                        }
                    }
                }
            }
        }

        for (String sink : Config.getInstance().getSinksAPIList()) {
            if (sink.startsWith("<") && sink.endsWith(">")) {
                // for example, <android.location.Location: double getLatitude()>
                if (isSourceOrSinkMatched(sink, stmt)) {
                    List<Unit> unitList = appModel.getMethodToSinkAPIsMap().get(method.getSignature());
                    if (unitList == null) {
                        unitList = new ArrayList<>();
                    }
                    unitList.add(stmt);
                    appModel.getMethodToSinkAPIsMap().put(method.getSignature(), unitList);
                    appModel.getSinkAPIToMethodMap().put(stmt, method.getSignature());
                    break;
                }
            } else {
                // for example, <android.telephony.TelephonyManager: java.lang.String getDeviceId()> android.permission.READ_PHONE_STATE
                // Regular expression to match the method signature and permission
                String regex = "<(.*?)>\\s+(.*)";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(sink);

                if (matcher.find()) {
                    String methodSignature = matcher.group(1);
                    String permission = matcher.group(2);

                    if (!Config.getInstance().isPermissionConsidered() || appModel.getPermissionSet().contains(permission)) {
                        if (isSourceOrSinkMatched(methodSignature, stmt)) {
                            List<Unit> unitList = appModel.getMethodToSinkAPIsMap().get(method.getSignature());
                            if (unitList == null) {
                                unitList = new ArrayList<>();
                            }
                            unitList.add(stmt);
                            appModel.getMethodToSinkAPIsMap().put(method.getSignature(), unitList);
                            appModel.getSinkAPIToMethodMap().put(stmt, method.getSignature());
                            break;
                        }
                    }
                } else {
                    log.error("Input string does not match the expected format.");
                }
            }
        }

//        for (String bothAPI : Config.getInstance().getBothAPIList()) {
//            if (matched)
//                break;
//
//            if (bothAPI.contains(stmt.getInvokeExpr().getMethod().getSubSignature())) {
//                List<Unit> unitListSource = appModel.getMethodToSourceAPIsMap().get(method.getSignature());
//                List<Unit> unitListSink = appModel.getMethodToSinkAPIsMap().get(method.getSignature());
//                if (unitListSource == null) {
//                    unitListSource = new ArrayList<>();
//                }
//                unitListSource.add(stmt);
//                appModel.getMethodToSourceAPIsMap().put(method.getSignature(), unitListSource);
//                appModel.getSourceAPIToMethodMap().put(stmt, method.getSignature());
//
//                if (unitListSink == null) {
//                    unitListSink = new ArrayList<>();
//                }
//                unitListSink.add(stmt);
//                appModel.getMethodToSinkAPIsMap().put(method.getSignature(), unitListSink);
//                appModel.getSinkAPIToMethodMap().put(stmt, method.getSignature());
//                matched = true;
//            }
//        }
    }

    public boolean isSourceOrSinkMatched(String sourceOrSink, Stmt stmt) {
        // ignore Intent methods
//        if (sourceOrSink.contains("Intent"))
//            return false;
        // ignore <init> methods
//        if (sourceOrSink.contains("<init>"))
//            return false;
//        // ignore "findViewById(int)" methods
//        if (sourceOrSink.contains("findViewById(int)"))
//            return false;
        if (stmt.getInvokeExpr().getMethod().getSignature().equals(sourceOrSink)) {
            // specialinvoke r0.<org.apache.http.HttpResponse: org.apache.http.HttpEntity getEntity()>;
            return true;
        } else if (stmt.getInvokeExpr().getMethod().getSubSignature().equals(getMethodNameFromSignature(sourceOrSink))) {
            return true;
        } else {
            return false;
        }
    }

    public String getMethodNameFromSignature(String sig) {
        // 定义正则表达式，用于匹配类名、返回值和方法名
        String regex = "(\\S+)\\s*:\\s*(\\S+)\\s*(\\S+\\([^)]*\\))";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(sig);

        // 判断是否找到匹配
        if (matcher.find()) {
            String className = matcher.group(1);  // 类名
            String returnType = matcher.group(2); // 返回值
            String methodName = matcher.group(3); // 方法名
            return returnType + " " + methodName;
        } else {
            log.error("The string format is incorrect:" + sig);
            return null;
        }
    }

    public void printCallGraphEdges(BidirectionalGraph<String> graph) {
        String filePath = "FlowDroidCallGraphEdges.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {

            for (String caller : graph.getOutNeighbors().keySet()) {
                for (String callee : graph.getOutNeighbors().get(caller)) {
                    // 写入每条边的详细信息
                    if (caller != null) {
                        writer.write("Source Method: " + caller);
                    }
                    writer.newLine();
                    writer.write("Target Method: " + callee);
                    writer.newLine();
                    writer.write("--------------------------------------------------");
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<Unit> findEntryPoints(DependenceGraph sdg) {
        Set<Unit> entryPointSet = new HashSet<>();
        // find callback methods
        for (DependenceGraph graph : Packs.getInstance()) {
            if (SootHelper.isCallBackMethod(graph.getMethod())) {
                entryPointSet.add(graph.getEntryNode());
            }
        }

        // find <init> and <clinit> methods
        for (DependenceGraph graph : Packs.getInstance()) {
            if (graph.getMethod().isConstructor() || graph.getMethod().isStaticInitializer()) {
                entryPointSet.add(graph.getEntryNode());
            }
        }
        return entryPointSet;
    }
}
