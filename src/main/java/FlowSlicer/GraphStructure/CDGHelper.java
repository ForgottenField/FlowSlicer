package FlowSlicer.GraphStructure;

import FlowSlicer.Config;
import FlowSlicer.Global;
import FlowSlicer.GraphStructure.DirectedClassGraph;
import gnu.trove.impl.sync.TSynchronizedShortByteMap;
import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.*;

import javax.print.DocFlavor;
import java.util.*;
@Slf4j
public class CDGHelper {
    public static DirectedClassGraph constructCDG () {
        //1 记录安卓应用的组件图信息
        //1.1 创建图顶点
        int vertex = 0;
        int number = 0;
        int statementNum = 0;
        DirectedClassGraph directedClassGraph = new DirectedClassGraph();
        for(SootClass sootClass : Scene.v().getApplicationClasses())
        {
            String className = sootClass.getName();
            String attribute = "Application Class";
            directedClassGraph.addVertex(new GraphNode(vertex++, className, attribute));

            Global.v().getAppModel().getGlobalSootClassSet().add(className);

            for (SootMethod sm : sootClass.getMethods()) {
                Global.v().getAppModel().getGlobalSootMethodSet().add(sm.getSignature());
                number++;

                try{
                    if (sm.isConcrete()) {
                        Body body = sm.retrieveActiveBody();
                    }

                } catch (Exception e) {
                    System.out.println(sm.getSignature() + " -> " + e);
                }

//                if (body != null) {
//                    statementNum += body.getUnits().size();
//                }

            }
        }
        Global.v().getAppModel().setSootClassNum(vertex);
        Global.v().getAppModel().setSootMethodNum(number);
        Global.v().getAppModel().setGlobalStmtNum(statementNum);

        for (SootClass sootClass : Scene.v().getLibraryClasses()) {
            String className = sootClass.getName();
            if (className.startsWith("android.support.")) {
                Global.v().getAppModel().getSupportClassSet().add(className);
                for (SootMethod sm : sootClass.getMethods()) {
                    Global.v().getAppModel().getSupportMethodSet().add(sm.getName());
                }
            }
        }

        for (SootClass sootClass : Scene.v().getPhantomClasses()) {
            String className = sootClass.getName();
            if (className.startsWith("android.support.")) {
                Global.v().getAppModel().getSupportClassSet().add(className);
                for (SootMethod sm : sootClass.getMethods()) {
                    Global.v().getAppModel().getSupportMethodSet().add(sm.getName());
                }
            }
        }

        //1.2 修改组件顶点的属性
        for(String component : Global.v().getAppModel().getActivities())
        {
            GraphNode componentNode = directedClassGraph.getGraphNode(component);
            if (componentNode != null) {
                componentNode.setAttribute("Activity");
            }
        }
        for(String component : Global.v().getAppModel().getServices())
        {
            GraphNode componentNode = directedClassGraph.getGraphNode(component);
            if (componentNode != null) {
                componentNode.setAttribute("Service");
            }
        }
        for(String component : Global.v().getAppModel().getProviders())
        {
            GraphNode componentNode = directedClassGraph.getGraphNode(component);
            if (componentNode != null) {
                componentNode.setAttribute("Provider");
            }
        }
        for(String component : Global.v().getAppModel().getReceivers())
        {
            GraphNode componentNode = directedClassGraph.getGraphNode(component);
            if (componentNode != null) {
                componentNode.setAttribute("Receiver");
            }
        }

        //1.3 创建边，边的左端点依赖于右端点
        for(GraphNode from : directedClassGraph.getNodeList())
        {
            //记录继承关系,from为子类，to为父类
            getExtendsRelation(from, directedClassGraph);
            //记录接口实现关系,from为实现类，to为接口
            getImplementRelation(from, directedClassGraph);
            //记录外部类关系,from是to的内部类
            getOuterClassRelation(from, directedClassGraph);
            //记录关联关系,to作为from中属性的一员
            getAssociationRelation(from, directedClassGraph);
            //记录调用关系，from调用to中的方法
            getInvokeRelation(from, directedClassGraph);
        }

        //2 记录安卓应用组件图对应的闭包信息
        //2.1 采用Tarjan's strongly connected components algorithm找出图中的强连通分量
        SCCGenerator sccGenerator = new SCCGenerator(directedClassGraph);
        directedClassGraph.setSccList(sccGenerator.tarjan());

        return directedClassGraph;
    }

    public static DirectedSCCGraph constructDSCCG (DirectedClassGraph directedClassGraph) {
        //2.2 基于找出的强连通分量构建有向无环图DAG
        int sccVertex = 0;
        DirectedSCCGraph DAG = new DirectedSCCGraph();

        //每个SCC内部的类不存在重复，所有SCC中类的总和为图的DCG的端点数
        for(ArrayList<GraphNode> list : directedClassGraph.getSccList())
        {
            //构造sccNode端点
            SCCNode sccNode = new SCCNode(sccVertex, list);
            for (GraphNode node : list) {
                DAG.getGraphNodeToSCCVertexMap().put(node, sccVertex);
            }
            DAG.addVertex(sccNode);
            sccVertex ++;
        }

        for (SCCNode fromSCCNode : DAG.getNodeList()) {
            for (GraphNode fromGraphNode : fromSCCNode.getSccList()) {
                //判断SCCNode中是否存在组件节点
                if (fromGraphNode.getAttribute().equals("Activity")) {
                    fromSCCNode.setHasActivityTrue();
                    fromSCCNode.getComponentList().add(fromGraphNode);
                }

                for (Edge<GraphNode> edge : fromGraphNode.getFromEdgeSet()) {
                    GraphNode toGraphNode = edge.getTo();
                    //自环边忽略
                    if (fromGraphNode.equals(toGraphNode)) {
                        continue;
                    }

                    //指向SCC内部的边忽略
                    boolean isInnerEdge = false;
                    for (GraphNode node : fromSCCNode.getSccList()) {
                        if (node.equals(toGraphNode)) {
                            isInnerEdge = true;
                            break;
                        }
                    }
                    if (isInnerEdge) {
                        continue;
                    }

                    //根据原图中指向SCC外部的边建立DAG中SCCNode之间的边
                    getSCCEdge(DAG, edge, fromSCCNode);
                }
            }
        }
//        getTopoList(DAG);
        return DAG;
    }

    public static HashSet<String> constructClosure (DirectedClassGraph cdg, DirectedSCCGraph dag, Set<String> sourceAndSinkSet) {
        ArrayList<String> closureList = new ArrayList<>();

        ArrayList<Integer> sccVertexList = new ArrayList<>();
        for (String classname : sourceAndSinkSet) {
            GraphNode node = cdg.getGraphNode(classname);
            Integer sccVertex = dag.getGraphNodeToSCCVertexMap().get(node);
            sccVertexList.add(sccVertex);
        }
        HashSet<Integer> sccVertexSet = new HashSet<>(sccVertexList);
        HashSet<SCCNode> sccNodeSet = new HashSet<>();
        for (Integer sccVertex : sccVertexSet) {
            SCCNode sccNode = dag.getGraphNode(sccVertex);
            sccNodeSet.add(sccNode);
        }

        Set<SCCNode> reachableFromInput = new HashSet<>();
        Set<SCCNode> reachableToInput = new HashSet<>();

        // Find nodes reachable from inputNodes
        for (SCCNode node : sccNodeSet) {
            dfs(node, dag, reachableFromInput, new HashSet<>(), false);
        }

        // Reverse the edges
        Map<SCCNode, List<SCCNode>> reverseGraph = new HashMap<>();
        for (SCCNode node : dag.getNodeList()) {
            reverseGraph.put(node, new ArrayList<>());
        }

        for (Edge<SCCNode> edge : dag.getEdgeList()) {
            reverseGraph.get(edge.getTo()).add(edge.getFrom());
        }

        // Find nodes that can reach inputNodes
        for (SCCNode node : sccNodeSet) {
            reverseDfs(node, reachableToInput, new HashSet<>(), reverseGraph);
        }

        // Combine the two sets
        reachableFromInput.addAll(reachableToInput);
        for (SCCNode node : reachableFromInput) {
            for (GraphNode graphNode : node.getSccList()) {
                closureList.add(graphNode.getClassName());
            }
        }

        return new HashSet<>(closureList);
    }

    /**
     * 记录图端点的继承关系
     * @param from
     * @param directedClassGraph
     * @return
     */
    public static void getExtendsRelation(GraphNode from, DirectedClassGraph directedClassGraph) {
        SootClass sootClass = Scene.v().getSootClass(from.getClassName());
        if (sootClass.hasSuperclass()) {
            SootClass superClass = sootClass.getSuperclass();
            GraphNode to = directedClassGraph.getGraphNode(superClass.getName());
            if(to != null){
                Edge<GraphNode> edge = new Edge<>(from,to);
                directedClassGraph.addEdge(edge);
                edge.setRelationTypeTrue("ExtendsRelation");
                edge.addWeight(RDmap.getInstance().findWeightFromRelation("ExtendsRelation"));
            }
        }
    }

    /**
     * 记录图端点的接口实现关系
     * @param from
     * @param directedClassGraph
     * @return
     */
    public static void getImplementRelation(GraphNode from, DirectedClassGraph directedClassGraph){
        SootClass sootClass = Scene.v().getSootClass(from.getClassName());
        if (sootClass.hasSuperclass()) {
            for(SootClass interfaceClass : sootClass.getInterfaces()){
                GraphNode to = directedClassGraph.getGraphNode(interfaceClass.getName());
                if(to != null){
                    Edge<GraphNode> edge = directedClassGraph.getEdge(from,to);
                    if(edge == null){
                        edge = new Edge<>(from,to);
                        directedClassGraph.addEdge(edge);
                    }
                    edge.setRelationTypeTrue("ImplementRelation");
                    edge.addWeight(RDmap.getInstance().findWeightFromRelation("ImplementRelation"));
                }
            }
        }
    }

    /**
     * 记录图端点的外部类关系
     * @param from
     * @param directedClassGraph
     * @return
     */
    public static void getOuterClassRelation(GraphNode from, DirectedClassGraph directedClassGraph){
        //字符串匹配 or 构建sootclass寻找outerclass域？
        SootClass sootClass = Scene.v().getSootClass(from.getClassName());
        SootClass outerClass = sootClass.getOuterClassUnsafe();
        if(outerClass != null){
            GraphNode to = directedClassGraph.getGraphNode(outerClass.getName());
            if (to != null) {
                Edge<GraphNode> edge = directedClassGraph.getEdge(from,to);
                if(edge == null){
                    edge = new Edge<>(from,to);
                    directedClassGraph.addEdge(edge);
                }
                edge.setRelationTypeTrue("OuterClassRelation");
                edge.addWeight(RDmap.getInstance().findWeightFromRelation("OuterClassRelation"));
            }
        }
    }

    /**
     * 记录图端点的关联关系
     * @param from
     * @param directedClassGraph
     * @return
     */
    public static void getAssociationRelation(GraphNode from, DirectedClassGraph directedClassGraph){
        SootClass sootClass = Scene.v().getSootClass(from.getClassName());
        for(SootField sootField : sootClass.getFields()){
            String fieldName = sootField.getSubSignature().split(" ")[0];
            //对于基本数据类型的情况不予遍历
            if(isFieldNameTrivial(fieldName))
                continue;
            GraphNode to = directedClassGraph.getGraphNode(fieldName);
            if(to != null){
                Edge<GraphNode> edge = directedClassGraph.getEdge(from,to);
                if(edge == null){
                    edge = new Edge<>(from,to);
                    edge.getAssociationInfoTable().put(fieldName,1);
                    directedClassGraph.addEdge(edge);
                }else{
                    if(edge.getAssociationInfoTable().get(fieldName) == null){
                        edge.getAssociationInfoTable().put(fieldName,1);
                    }else{
                        int value = edge.getAssociationInfoTable().get(fieldName);
                        edge.setAssociationInfoTable(fieldName,++value);
                    }
                }
                edge.setRelationTypeTrue("AssociationRelation");
                edge.addWeight(RDmap.getInstance().findWeightFromRelation("AssociationRelation"));
            }
        }
    }

    /**
     * 记录图端点的调用关系
     * @param from
     * @param directedClassGraph
     * @return
     */
    public static void getInvokeRelation(GraphNode from, DirectedClassGraph directedClassGraph){
//        Scene.v().getCallGraph()
        SootClass sootClass = Scene.v().getSootClass(from.getClassName());
        for(SootMethod sootMethod : sootClass.getMethods()){
            if(sootMethod.isConcrete()){
                Body body = sootMethod.retrieveActiveBody();
                Iterator<Unit> units = body.getUnits().snapshotIterator();
                while(units.hasNext()){
                    Unit unit = units.next();
                    Stmt stmt = (Stmt)unit;
                    if(stmt.containsInvokeExpr()){ //包含JAssignStmt与JInvokeStmt
                        InvokeExpr invokeExpr = stmt.getInvokeExpr();
                        List<SootMethod> calleeList = resolve(invokeExpr, directedClassGraph);
                        if (calleeList == null) {
//                            log.error("The CHA algorithm does not find any method invoke for unit" + unit +  "\n");
                            continue;
                        }
                        for (SootMethod callee : calleeList) {
                            // 构建CHA call graph
                            soot.jimple.toolkits.callgraph.Edge chaEdge = new soot.jimple.toolkits.callgraph.Edge(sootMethod, stmt, callee);
                            Global.v().getAppModel().getChaCallGraph().addEdge(chaEdge);

                            // 构建局部依赖图CDG
                            GraphNode to = directedClassGraph.getGraphNode(callee.getDeclaringClass().getName());
                            if(to != null){
                                Edge<GraphNode> edge = directedClassGraph.getEdge(from,to);
                                if(edge == null){
                                    edge = new Edge<>(from,to);
                                    edge.getInvokeInfoTable().put("InvokeTimes", String.valueOf(1));
                                    directedClassGraph.addEdge(edge);
                                }else{
                                    if(edge.getInvokeInfoTable().get("InvokeTimes") == String.valueOf(0)){
                                        edge.getInvokeInfoTable().put("InvokeTimes", String.valueOf(1));
                                    }else{
                                        int value = Integer.parseInt(edge.getInvokeInfoTable().get("InvokeTimes"));
                                        edge.getInvokeInfoTable().put("InvokeTimes", String.valueOf(++value));
                                    }
                                }
                                edge.getInvokeInfoTable().put("InvokeType",invokeExpr.toString().split(" ")[0]);
                                edge.getInvokeInfoTable().put("MethodName",callee.getName());
                                edge.getInvokeInfoTable().put("ReturnType",callee.getReturnType().toString());
                                edge.getInvokeInfoTable().put("Parameters",callee.getParameterTypes().toString());
                                edge.setRelationTypeTrue("InvokeRelation");
                                edge.addWeight(RDmap.getInstance().findWeightFromRelation("InvokeRelation"));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * According to https://haotianmichael.github.io/2021/05/10/NJU%E9%9D%99%E6%80%81%E7%A8%8B%E5%BA%8F%E5%88%86%E6%9E%90-2-CHA-Analysis/
     * @param invokeExpr
     * @return calleeList
     */

    public static List<SootMethod> resolve(InvokeExpr invokeExpr, DirectedClassGraph directedClassGraph) {
        List<SootMethod> calleeList = new ArrayList<>();
        SootMethod targetMethod = invokeExpr.getMethod();
        if (invokeExpr instanceof StaticInvokeExpr) {
            calleeList.add(targetMethod);
            return calleeList;
        } else if (invokeExpr instanceof SpecialInvokeExpr) {
            SootClass classType = targetMethod.getDeclaringClass();
            SootMethod dispatchedMethod = dispatch(classType, targetMethod, directedClassGraph);
            if (dispatchedMethod != null) {
                calleeList.add(dispatchedMethod);
            }
            return calleeList;
        } else if (invokeExpr instanceof VirtualInvokeExpr) {
            Local receiver = (Local) ((InstanceInvokeExpr) invokeExpr).getBase();
            Type receiverType = receiver.getType();
            if (receiverType instanceof RefType) {
                SootClass receiverClass = ((RefType) receiverType).getSootClass();
                if (receiverClass != null) {
                    if (!receiverClass.isInterface()) {
                        for (SootClass subclass : Scene.v().getActiveHierarchy().getSubclassesOfIncluding(receiverClass)) {
                            SootMethod dispatchedMethod = dispatch(subclass, targetMethod, directedClassGraph);
                            if (dispatchedMethod != null){
                                calleeList.add(dispatchedMethod);
                            }
                        }
                    } else {
                        // receiverClass.isInterface()
                        List<SootClass> implementers = new ArrayList<>(Scene.v().getActiveHierarchy().getImplementersOf(receiverClass));
                        for (SootClass implementer : implementers) {
                            SootMethod dispatchedMethod = dispatch(implementer, targetMethod, directedClassGraph);
                            if (dispatchedMethod != null) {
                                calleeList.add(dispatchedMethod);
                            }
                        }
                    }
                }
            }
            return calleeList;
        } else if (invokeExpr instanceof InterfaceInvokeExpr) {
            SootClass interfaceType = targetMethod.getDeclaringClass();
            if (interfaceType != null && interfaceType.isInterface()) {
                List<SootClass> implementers;
                //避免getImplementersOf方法抛出异常
                try {
                    implementers = Scene.v().getActiveHierarchy().getImplementersOf(interfaceType);
                } catch (java.lang.NullPointerException e) {
                    return null;
                }

                if (implementers != null) {
                    for (SootClass implementer : implementers) {
                        SootMethod dispatchedMethod = dispatch(implementer, targetMethod, directedClassGraph);
                        if (dispatchedMethod != null) {
                            calleeList.add(dispatchedMethod);
                        }
                    }
                }
            }
            return calleeList;
        } else {
            // invokeExpr instanceof DynamicInvokeExpr
            // Handling of invokedynamic requires knowledge of bootstrap methods and lambdas.
            // This example does not cover full handling of invokedynamic due to its complexity.
            // Here we assume invokedynamic resolves to some known targets, typically requires external analysis.
            // Add your dynamic resolution logic here.
            return null;
        }
    }

    /**
     *
     * @param sootClass
     * @param targetMethod
     * @return
     */
    private static SootMethod dispatch(SootClass sootClass, SootMethod targetMethod, DirectedClassGraph directedClassGraph) {
        for (SootMethod method : sootClass.getMethods()) {
            if (method.getName().equals(targetMethod.getName())
                    && method.getParameterTypes().equals(targetMethod.getParameterTypes())
                    && method.getReturnType().equals(targetMethod.getReturnType())) {
                return method;
            }
        }

        // 如果在当前类中无法找到匹配的方法，到当前类的父类中继续寻找，直到找到为止
        GraphNode targetGraphNode = directedClassGraph.getGraphNode(sootClass.getName());
        if (targetGraphNode != null) {
            for (GraphNode graphNode : directedClassGraph.getNodeList()) {
                if (graphNode.getVertex().equals(targetGraphNode.getVertex())) {
                    for (Edge<GraphNode> edge : graphNode.getFromEdgeSet()) {
                        if (edge.isExtendsEdge()) {
                            SootClass superSootClass = Scene.v().getSootClassUnsafe(edge.getTo().getClassName());
                            if (superSootClass != null) {
                                dispatch(superSootClass, targetMethod, directedClassGraph);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 判断fieldName是否为基本数据结构
     * @param fieldName
     * @return
     */
    public static Boolean isFieldNameTrivial(String fieldName)
    {
        if(!fieldName.endsWith("int")){
            if(!fieldName.endsWith("float")){
                if(!fieldName.endsWith("long")){
                    if(!fieldName.endsWith("[]")){
                        if(!fieldName.endsWith("Integer")){
                            if(!fieldName.endsWith("double")){
                                if(!fieldName.endsWith("boolean")){
                                    if(!fieldName.contains("java")){
                                        if(!fieldName.endsWith("char")){
                                            if(!fieldName.endsWith("short")){
                                                if(!fieldName.endsWith("byte")){
                                                    return false;
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
        return true;
    }

    public static void getSCCEdge(DirectedSCCGraph DAG, Edge<GraphNode> edge, SCCNode fromSCCNode)
    {
        SCCNode toSCCNode = null;
        boolean finished = false;
        for(SCCNode sccNode : DAG.getNodeList()){
            //fromSCCNode节点不需要遍历
            if(sccNode.equals(fromSCCNode)){
                continue;
            }

            for(GraphNode node : sccNode.getSccList()){
                if(node.equals(edge.getTo())){
                    toSCCNode = sccNode;
                    finished = true;
                    break;
                }
            }
            if(finished){
                break;
            }
        }

        if(toSCCNode != null){
            Edge<SCCNode> sccEdge = DAG.getEdge(fromSCCNode, toSCCNode);
            if(sccEdge == null){
                sccEdge = new Edge<>(fromSCCNode, toSCCNode, edge.getWeight());
                DAG.addEdge(sccEdge);
            }else {
                sccEdge.addWeight(edge.getWeight());
            }
        }
    }

    public static void getTopoList(DirectedSCCGraph DAG)
    {
        List<SCCNode> topoNodeList = new ArrayList<>();
        int[] inDegreeArray = new int[DAG.getNodeNum()];
        int queueNum = 0;
        for(SCCNode node : DAG.getNodeList()){
            if(node.getInDegree() == 0){
                topoNodeList.add(node);
                DAG.getTopoNodeList().add(node);
            }
            inDegreeArray[node.getVertex()] = node.getInDegree();
        }
        while(!topoNodeList.isEmpty()){
            SCCNode from = topoNodeList.remove(0);
            for(Edge<SCCNode> edge : from.getFromEdgeSet()){
                SCCNode to = edge.getTo();
                inDegreeArray[to.getVertex()]--;
                if(inDegreeArray[to.getVertex()] == 0){
                    topoNodeList.add(to);
                    DAG.getTopoNodeList().add(to);
                }
            }
            queueNum++;
        }

        if(queueNum == DAG.getNodeNum()){
            System.out.println("图结构中不含有环");
            Collections.reverse(DAG.getTopoNodeList());
        }else{
            System.out.println("图结构中含有环");
            DAG.getTopoNodeList().clear();
        }
    }

    private static void dfs(SCCNode node, DirectedSCCGraph dag, Set<SCCNode> visited, Set<SCCNode> seen, boolean reverse) {
        if (!seen.contains(node)) {
            seen.add(node);
            visited.add(node);
            for (Edge<SCCNode> edge : dag.getEdgeList()) {
                if (edge.getFrom().equals(node) && !visited.contains(edge.getTo())) {
                    dfs(edge.getTo(), dag, visited, seen, reverse);
                }
            }
        }
    }

    private static void reverseDfs(SCCNode node, Set<SCCNode> visited, Set<SCCNode> seen, Map<SCCNode, List<SCCNode>> reverseGraph) {
        if (!seen.contains(node)) {
            seen.add(node);
            visited.add(node);
            for (SCCNode neighbour : reverseGraph.get(node)) {
                if (!visited.contains(neighbour)) {
                    reverseDfs(neighbour, visited, seen, reverseGraph);
                }
            }
        }
    }
}
