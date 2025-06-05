package FlowSlicer.Mode;

import FlowSlicer.*;
import FlowSlicer.DataStructure.parser.ADQLParser;
import FlowSlicer.DataStructure.parser.ParseException;
import FlowSlicer.GraphStructure.*;
import FlowSlicer.GraphStructure.Edge;
import FlowSlicer.RefactorTool.SplitAPK;
import FlowSlicer.XMLObject.Reference;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import heros.solver.CountingThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.VirtualEdgesSummaries;
import soot.options.Options;
import soot.util.queue.QueueReader;

import java.io.*;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.*;

@Slf4j
public class SplitApkInExclusionMode extends SplitAPK {
    public SplitApkInExclusionMode() {
        super();
    }

    @Override
    public void splitApk() throws IOException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        // 执行splitApk方法中耗时的操作
        Future<?> future = executor.submit(this::splitApkInternal);

        try {
            future.get(Config.getInstance().getSlicer_timeout_sec(), TimeUnit.SECONDS);  // 等待执行完成或超时
        } catch (TimeoutException e) {
            future.cancel(true);  // 超时后尝试取消任务
            Config.getInstance().setIsSlicerTimeOut(true);
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

        // Slice CFG according to SDG
        if (Config.getInstance().isSlicingCriteriaFound()) {
            Statistics.getTimer(Statistics.TIMER_Slicing_CFG).start();
            if (appModel.getSdg() != null && appModel.getSdgSliced() != null) {
                appModel.setNodesBeforeSlicing(appModel.getSdg().getAllNodes().size());
//                appModel.setMethodsBeforeSlicing(appModel.getSootMethodNum());
                sliceCFG(appModel.getSdgSliced(), appModel.getSdg());
                appModel.setNodesAfterSlicing(appModel.getSdgSliced().getAllNodes().size());
            } else if (appModel.getSdg() != null && appModel.getLocalSootClassSet() != null && !appModel.getLocalSootClassSet().isEmpty()) {
                appModel.setNodesBeforeSlicing(appModel.getGlobalStmtNum());
//                appModel.setMethodsBeforeSlicing(appModel.getSootMethodNum());
                sliceCFGWithoutSDG();
            } else {
                // even without LocalSootClassSet
                log.error("The analysis is timeout. It does not slice anything");
            }
            Statistics.getTimer(Statistics.TIMER_Slicing_CFG).stop();
        }

        // Write output
        writeOutput();
    }

    public void splitApkInternal() {
        String ICCFilePath = Config.getInstance().getICCFilePath();
        ICCEdgesHandler.parseICCFile(ICCFilePath);

        // locate sources and sinks through unit matching
        Statistics.getTimer(Statistics.TIMER_Locating_Source_Sink).start();
        int threadNum = Options.v().num_threads();
        if (threadNum < 1) {
            threadNum = Runtime.getRuntime().availableProcessors();
        }
        CountingThreadPoolExecutor executor =
                new CountingThreadPoolExecutor(threadNum, threadNum, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        for (SootClass sc : Scene.v().getApplicationClasses()) {
            executor.execute(() -> {
                for (SootMethod sm : sc.getMethods()) {
                    if (!sm.isConcrete()) {
                        continue;
                    }
                    Body body = SootHelper.getActiveBodyIfMethodExists(sm);
                    if (body == null) {
                        continue;
                    }
                    Iterator<Unit> units = body.getUnits().snapshotIterator();
                    while (units.hasNext()) {
                        Unit unit = units.next();
                        Stmt stmt = (Stmt) unit;
                        // 对于包含invoke表达式的unit，匹配其是否调用了Sources和Sinks列表中的API
                        if (stmt != null && stmt.containsInvokeExpr()) {
                            locateSourceAndSinkUnit(sm, stmt);
                        }
                    }
                }
            });
        }

        try {
            executor.awaitCompletion();
            executor.shutdown();
        } catch (InterruptedException e) {
            // Something went horribly wrong
            throw new RuntimeException("Could not wait for pack threads to finish: " + e.getMessage(), e);
        }

        Throwable exception = executor.getException();
        if (exception != null) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else {
                throw new RuntimeException(exception);
            }
        }
        Statistics.getTimer(Statistics.TIMER_Locating_Source_Sink).stop();

        // Construct Class Graph and my CallGraph(CHA)
        DirectedClassGraph cdg = CDGHelper.constructCDG();
        // Construct Class Graph and my CallGraph(CHA)
        Statistics.getTimer(Statistics.TIMER_Constructing_Local_Graph).start();
        DirectedSCCGraph dag = CDGHelper.constructDSCCG(cdg);
        HashSet<String> closureSet = CDGHelper.constructClosure(cdg, dag, appModel.getInputClassSet());
        appModel.setLocalSootClassSet(closureSet);
        Statistics.getTimer(Statistics.TIMER_Constructing_Local_Graph).stop();

        // Setup PDG generation and Build PDGs (Run Soot)
        Statistics.getTimer(Statistics.TIMER_BUILDING_PDGS).start();
        PackManager.v().getPack("jtp").add(new Transform("jtp.PDGTransformer", new PDGTransformer(appModel.getGlobalSootClassSet())));
        PackManager.v().runPacks();
        Statistics.getTimer(Statistics.TIMER_BUILDING_PDGS).stop();

//        String sourceFile = "source_apis.json";
//        String sinkFile = "sink_apis.json";
//        Utility.exportSourceSinkInfo(appModel.getMethodToSourceAPIsMap(), appModel.getMethodToSinkAPIsMap(), sourceFile, sinkFile);

        // Set TargetTo, TargetFrom, and SinkToSourceUnitMultiMap
        Statistics.getTimer(Statistics.TIMER_Finding_SlicingCriteria).start();
        Set<Unit> targetListFrom = new HashSet<>();
        Set<Unit> targetListTo = new HashSet<>();
        if (Config.getInstance().isRandomCriteria()) {
            // Randomly select a source and a sink as the slicing criteria
            Random random = new Random();
            List<String> sourceMethods = new ArrayList<>(appModel.getMethodToSourceAPIsMap().keySet());
            List<String> sinkMethods = new ArrayList<>(appModel.getMethodToSinkAPIsMap().keySet());
            if (!sourceMethods.isEmpty() && !sinkMethods.isEmpty()) {
                String randomSourceMethod = sourceMethods.get(random.nextInt(sourceMethods.size()));
                String randomSinkMethod = sinkMethods.get(random.nextInt(sinkMethods.size()));

                // 查找对应的SootMethod
                SootMethod sourceSootMethod = Scene.v().getMethod(randomSourceMethod);
                if (sourceSootMethod != null) {
                    appModel.getInputClassSet().add(sourceSootMethod.getDeclaringClass().getName());
                }

                SootMethod sinkSootMethod = Scene.v().getMethod(randomSinkMethod);
                if (sinkSootMethod != null) {
                    appModel.getInputClassSet().add(sinkSootMethod.getDeclaringClass().getName());
                }

                // 获取source和sink statements列表
                List<Unit> sourceUnits = appModel.getMethodToSourceAPIsMap().get(randomSourceMethod);
                List<Unit> sinkUnits = appModel.getMethodToSinkAPIsMap().get(randomSinkMethod);

                if (sourceUnits != null && !sourceUnits.isEmpty() && sinkUnits != null && !sinkUnits.isEmpty()) {
                    // 随机选择一个source和一个sink statement
                    Unit randomSourceUnit = sourceUnits.get(random.nextInt(sourceUnits.size()));
                    Unit randomSinkUnit = sinkUnits.get(random.nextInt(sinkUnits.size()));

                    // 将随机选择的source和sink添加到目标列表
                    targetListFrom.add(randomSourceUnit);
                    targetListTo.add(randomSinkUnit);

                    // 将source和sink添加到多重映射中
                    appModel.getSinkToSourceUnitMultimap().put(randomSinkUnit, randomSourceUnit);
                    appModel.getSourceToSinkUnitMultimap().put(randomSourceUnit, randomSinkUnit);
                }
            }
        } else if (Config.getInstance().isSourceSinkInSameMethod()) {
            List<String> sourceMethods = new ArrayList<>(appModel.getMethodToSourceAPIsMap().keySet());
            List<String> sinkMethods = new ArrayList<>(appModel.getMethodToSinkAPIsMap().keySet());
            List<String> sameMethodList = new ArrayList<>();
            if (!sourceMethods.isEmpty() && !sinkMethods.isEmpty()) {
                for (String sourceMethodName : sourceMethods) {
                    for (String sinkMethodName : sinkMethods) {
                        if (sourceMethodName.equals(sinkMethodName)) {
                            sameMethodList.add(sourceMethodName);
                        }
                    }
                }
            }
            System.out.println(sameMethodList);
            System.out.println("\n");

            String methodName = "<com.plh.gofastlauncher.MainActivity: void onResume()>";
            List<Unit> sourceUnit = appModel.getMethodToSourceAPIsMap().get(methodName);
            List<Unit> sinkUnit = appModel.getMethodToSinkAPIsMap().get(methodName);
            System.out.println("\n");
            SootMethod sourceSootMethod = Scene.v().getMethod(methodName);
            if (sourceSootMethod != null) {
                appModel.getInputClassSet().add(sourceSootMethod.getDeclaringClass().getName());
            }
            targetListFrom.addAll(sourceUnit);
            targetListTo.addAll(sinkUnit);
//            for (Unit sink : sinkUnit) {
//                for (Unit source : sourceUnit) {
//                    appModel.getSinkToSourceUnitMultimap().put(sink, source);
//                    appModel.getSourceToSinkUnitMultimap().put(source, sink);
//                }
//            }
        } else if (Config.getInstance().isAllSourceAndSinkConsidered()) {
            for (String sourceMethod : appModel.getMethodToSourceAPIsMap().keySet()) {
                for (String sinkMethod : appModel.getMethodToSinkAPIsMap().keySet()) {
                    SootMethod sourceSootMethod = Scene.v().getMethod(sourceMethod);
                    if (sourceSootMethod != null) {
                        appModel.getInputClassSet().add(sourceSootMethod.getDeclaringClass().getName());
                    }
                    SootMethod sinkSootMethod = Scene.v().getMethod(sinkMethod);
                    if (sinkSootMethod != null) {
                        appModel.getInputClassSet().add(sinkSootMethod.getDeclaringClass().getName());
                    }

                    List<Unit> sourceUnit = appModel.getMethodToSourceAPIsMap().get(sourceMethod);
                    List<Unit> sinkUnit = appModel.getMethodToSinkAPIsMap().get(sinkMethod);
                    targetListFrom.addAll(sourceUnit);
                    targetListTo.addAll(sinkUnit);
//                    for (Unit sink : sinkUnit) {
//                        for (Unit source : sourceUnit) {
//                            appModel.getSinkToSourceUnitMultimap().put(sink, source);
//                            appModel.getSourceToSinkUnitMultimap().put(source, sink);
//                        }
//                    }
                }
            }
        } else {
            // set slicing criteria according to DSL scripts
            try {
                ADQLParser.parse();
            } catch (FileNotFoundException | ParseException e) {
                throw new RuntimeException(e);
            }

            if (appModel.getFlowQuerySinkStmt() != null && appModel.getFlowQuerySourceStmt() != null) {
                // to be added
                ;
            } else if (appModel.getFlowQuerySinkMethod() != null && appModel.getFlowQuerySourceMethod() != null){
                for (SootMethod sm : SootHelper.findMethods(appModel.getFlowQuerySinkMethod())) {
                    if (sm.isConcrete()) {
                        Unit sourceUnit = SootHelper.getFirstUnitOfMethod(sm);
                        if (sourceUnit != null) {
                            targetListFrom.add(sourceUnit);
                        }
                    }
                }
                for (SootMethod sm : SootHelper.findMethods(appModel.getFlowQuerySourceMethod())) {
                    if (sm.isConcrete()) {
                        Unit sinkUnit = SootHelper.getFirstUnitOfMethod(sm);
                        if (sinkUnit != null) {
                            targetListTo.add(sinkUnit);
                        }
                    }
                }
            }
        }
        appModel.setFromCriteriaSet(targetListFrom);
        appModel.setToCriteriaSet(targetListTo);
        Statistics.getTimer(Statistics.TIMER_Finding_SlicingCriteria).stop();

        if (!appModel.getFromCriteriaSet().isEmpty() && !appModel.getToCriteriaSet().isEmpty()) {
            log.info("Successfully find Slicing Targets\n");
            Config.getInstance().setSlicingCriteriaFound(true);
        } else {
            log.error("Cannot read any slicing criteria!\n");
        }

        if (Config.getInstance().isSlicingCriteriaFound()) {
            // Handle Exceptions
            Statistics.getTimer(Statistics.TIMER_Handling_Exceptions).start();
            handleExceptions();
            Statistics.getTimer(Statistics.TIMER_Handling_Exceptions).stop();

            // Build Dummy Main (FlowDroid)
            Statistics.getTimer(Statistics.TIMER_Building_Dummymain).start();
            buildDummyMainPDG();
            Statistics.getTimer(Statistics.TIMER_Building_Dummymain).stop();

            // Add Callback edges
            Statistics.getTimer(Statistics.TIMER_Adding_Callbacks).start();
            addCallBackEdges();
            Statistics.getTimer(Statistics.TIMER_Adding_Callbacks).stop();

            // extend the CallGraph with virtual edges
            for (SootClass sc : Scene.v().getApplicationClasses()) {
                for (SootMethod caller : sc.getMethods()) {
                    if (!caller.isConcrete()) {
                        continue;
                    }
                    Body body = SootHelper.getActiveBodyIfMethodExists(caller);
                    if (body == null) {
                        continue;
                    }
                    Iterator<Unit> units = body.getUnits().snapshotIterator();
                    while (units.hasNext()) {
                        Unit unit = units.next();
                        Stmt stmt = (Stmt) unit;
                        if (stmt != null && stmt.containsInvokeExpr()) {
                            // match caller and callee in constructed virtualEdgesMultimap
                            for (String source : appModel.getVirtualEdgesMultiMap().keySet()) {
                                if (stmt.getInvokeExpr().getMethod().getSubSignature().equals(source)) {
                                    for (String target : appModel.getVirtualEdgesMultiMap().get(source)) {
                                        for (SootMethod callee : SootHelper.findMethodsBySubSig(target)) {
                                            soot.jimple.toolkits.callgraph.Edge edge = new soot.jimple.toolkits.callgraph.Edge(caller, stmt, callee);
                                            appModel.getLifeCycleHandler().getCg().addEdge(edge);
//                                            System.out.println("Adding virtual edge: " + caller + " -> " + callee);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

//            BidirectionalGraph<String> graph = CallGraphUtils.buildBidirectionalGraph(appModel.getLifeCycleHandler().getCg());
//            BidirectionalGraph<String> chaGraph = CallGraphUtils.buildBidirectionalGraph(appModel.getChaCallGraph());
//            graph.mergeWith(chaGraph);
////            printCallGraphEdges(graph);
//
//            // expand callgraph to deal with callback methods of non-component class
//            for (GraphNode sc : cdg.getNodeList()) {
//                if (!sc.isComponentClass()) {
//                    SootClass sootClass = Scene.v().getSootClassUnsafe(sc.getClassName());
//                    if (SootHelper.isCallBackClass(sootClass)) {
//                        String initMethodName = null;
//                        Set<String> callbackMethodNameSet = new HashSet<>();
//                        for (SootMethod sm : sootClass.getMethods()) {
//                            if (sm.isConstructor()) {
//                                initMethodName = sm.getSignature();
//                                continue;
//                            }
//                            if (SootHelper.isCallBackMethod(sm)) {
//                                callbackMethodNameSet.add(sm.getSignature());
//                            }
//                        }
//
//                        for (String callbackMethodName : callbackMethodNameSet) {
//                            graph.addEdge(initMethodName, callbackMethodName);
//                        }
//                    }
//                }
//            }
//
//            // find paths between sources and sinks
//            Statistics.getTimer(Statistics.TIMER_Reachability_Query).start();
//            Multimap<String, String> allReachablePairs = CallGraphUtils.findReachabilityByIPPlus(graph);
//
//            if (!allReachablePairs.isEmpty()) {
//                Set<Unit> updatedTargetListFrom = new HashSet<>();
//                Set<Unit> updatedTargetListTo = new HashSet<>();
//                Multimap<Unit, Unit>  updatedSinkToSourceUnitMultimap = HashMultimap.<Unit, Unit>create();
//                Multimap<Unit, Unit>  updatedSourceToSinkUnitMultimap = HashMultimap.<Unit, Unit>create();
//                // update every pair
//                for (String sourceMethod : allReachablePairs.keySet()) {
//                    for (String sinkMethod : allReachablePairs.get(sourceMethod)) {
//                        SootMethod sourceSootMethod = Scene.v().getMethod(sourceMethod);
//                        SootMethod sinkSootMethod = Scene.v().getMethod(sinkMethod);
//                        if (sourceSootMethod != null && sinkSootMethod != null) {
//                            List<Unit> sourceUnit = appModel.getMethodToSourceAPIsMap().get(sourceMethod);
//                            List<Unit> sinkUnit = appModel.getMethodToSinkAPIsMap().get(sinkMethod);
//                            updatedTargetListFrom.addAll(sourceUnit);
//                            updatedTargetListTo.addAll(sinkUnit);
//                            for (Unit sink : sinkUnit) {
//                                for (Unit source : sourceUnit) {
//                                    updatedSinkToSourceUnitMultimap.put(sink, source);
//                                    updatedSourceToSinkUnitMultimap.put(source, sink);
//                                }
//                            }
//                        }
//                    }
//                }
//
//                // update the pair with most targets
////                String maxKey = null;
////                int max = 0;
////                for (String sourceMethod : allReachablePairs.keySet()) {
////                    int size = allReachablePairs.get(sourceMethod).size();
////                    if (size > max) {
////                        max = size;
////                        maxKey = sourceMethod;
////                    }
////                }
////
////                for (String sinkMethod : allReachablePairs.get(maxKey)) {
////                    SootMethod sourceSootMethod = Scene.v().getMethod(maxKey);
////                    SootMethod sinkSootMethod = Scene.v().getMethod(sinkMethod);
////                    if (sourceSootMethod != null && sinkSootMethod != null) {
////                        List<Unit> sourceUnit = appModel.getMethodToSourceAPIsMap().get(maxKey);
////                        List<Unit> sinkUnit = appModel.getMethodToSinkAPIsMap().get(sinkMethod);
////                        updatedTargetListFrom.addAll(sourceUnit);
////                        updatedTargetListTo.addAll(sinkUnit);
////                        for (Unit sink : sinkUnit) {
////                            for (Unit source : sourceUnit) {
////                                updatedSinkToSourceUnitMultimap.put(sink, source);
////                                updatedSourceToSinkUnitMultimap.put(source, sink);
////                            }
////                        }
////                    }
////                }
//
//                if (!updatedTargetListFrom.isEmpty() && !updatedTargetListTo.isEmpty() &&
//                        !updatedSourceToSinkUnitMultimap.isEmpty() && !updatedSinkToSourceUnitMultimap.isEmpty())
//                {
//                    appModel.setFromCriteriaSet(updatedTargetListFrom);
//                    appModel.setToCriteriaSet(updatedTargetListTo);
//                    appModel.setSourceToSinkUnitMultimap(updatedSourceToSinkUnitMultimap);
//                    appModel.setSinkToSourceUnitMultimap(updatedSinkToSourceUnitMultimap);
//                }
//            }
//            Statistics.getTimer(Statistics.TIMER_Reachability_Query).stop();

            // Build SDG
            Statistics.getTimer(Statistics.TIMER_BUILDING_SDG).start();
            final PDGConnector connector = new PDGConnector();
            DependenceGraph sdg = connector.buildSDG();
            Statistics.getTimer(Statistics.TIMER_BUILDING_SDG).stop();

            // Read ICCFile
            ICCEdgesHandler.addInputEdges(sdg);

            sdg.getReturnNodes().clear();
            sdg.setParameterNode(null);

            // Slice SDG
            Statistics.getTimer(Statistics.TIMER_Slicing_SDG).start();
            DependenceGraph sdgSliced = sliceSDG(sdg);
//            appModel.setSdgSliced(sdgSliced);
            Statistics.getTimer(Statistics.TIMER_Slicing_SDG).stop();
        }
    }
}
