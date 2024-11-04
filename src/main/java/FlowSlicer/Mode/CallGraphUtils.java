package FlowSlicer.Mode;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import FlowSlicer.Config;
import FlowSlicer.Global;
import FlowSlicer.GraphStructure.BidirectionalGraph;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import heros.solver.CountingThreadPoolExecutor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;

import java.util.*;
import java.util.concurrent.*;

import static java.util.stream.Collectors.toList;
@Getter
@Slf4j
public class CallGraphUtils {
    public static BidirectionalGraph<String> buildBidirectionalGraph(CallGraph callGraph) {
        BidirectionalGraph<String> graph = new BidirectionalGraph<>();
        for (Edge edge : callGraph) {
            if (edge.getSrc() != null && edge.srcUnit() != null) {
                String src = edge.getSrc().method().getSignature();
                String tgt = edge.getTgt().method().getSignature();
                graph.addEdge(src, tgt);
            }
        }
//        Global.v().getAppModel().setCallGraphBidirectional(graph);
        return graph;
    }

    public static BidirectionalGraph<Integer> buildSCCGraph(BidirectionalGraph<String> graph, Map<String, Integer> nodeToSCC, List<Set<String>> sccComponents) {
        BidirectionalGraph<Integer> sccGraph = new BidirectionalGraph<>();

        for (Set<String> scc : sccComponents) {
            boolean hasOuterEdge = false;
            for (String node : scc) {
                for (String neighbor : graph.getOutNeighbors(node)) {
                    if (!scc.contains(neighbor)) {
                        hasOuterEdge = true;
                        Integer fromSCC = nodeToSCC.get(node);
                        Integer toSCC = nodeToSCC.get(neighbor);
                        sccGraph.addEdge(fromSCC, toSCC);
                    }
                }
            }
            if (!hasOuterEdge) {
                for (String node : scc) {
                    Integer index = nodeToSCC.get(node);
                    sccGraph.addIsolatedNode(index);
                    break;
                }
            }
        }
        Global.v().getAppModel().setSccGraphBidirectional(sccGraph);
        return sccGraph;
    }

    public static List<Integer> registerTopologicalOrder(BidirectionalGraph<Integer> graph) {
        List<Integer> topOrder = new ArrayList<>();
        Map<Integer, Integer> inDegree = new HashMap<>();
        for (Integer node : graph.getNodes()) {
            inDegree.put(node, graph.getInNeighbors(node).size());
        }
        Queue<Integer> queue = new LinkedList<>();
        for (Integer node : inDegree.keySet()) {
            if (inDegree.get(node) == 0) {
                queue.add(node);
            }
        }
        while (!queue.isEmpty()) {
            Integer current = queue.poll();
            topOrder.add(current);

            for (Integer neighbor : graph.getOutNeighbors(current)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.add(neighbor);
                }
            }
        }
        Global.v().getAppModel().setSccGraphTopologicalOrder(topOrder);
        return topOrder;
    }

    public static List<Integer> registerReverseTopologicalOrder (List<Integer> topOrder) {
        List<Integer> reverseTopOrder = new ArrayList<>(topOrder);
        Collections.reverse(reverseTopOrder);
        Global.v().getAppModel().setSccGraphReverseTopologicalOrder(reverseTopOrder);
        return reverseTopOrder;
    }

    public static Multimap<String, String> findReachabilityByIPPlus(BidirectionalGraph<String> graph)
    {
        // Compute SCCs
        SCCFinder sccFinder = new SCCFinder(graph);
        Map<String, Integer> nodeToSCC = sccFinder.getNodeToSCC();
        List<Set<String>> sccComponents = sccFinder.getSccComponents();

        // Build SCC graph
        BidirectionalGraph<Integer> sccGraph = CallGraphUtils.buildSCCGraph(graph, nodeToSCC, sccComponents);
        List<Integer> topOrder = CallGraphUtils.registerTopologicalOrder(sccGraph);
        List<Integer> reverseTopOrder = CallGraphUtils.registerReverseTopologicalOrder(topOrder);

        ReachabilityQuery reachabilityQuery = new ReachabilityQuery(sccGraph, topOrder, reverseTopOrder, 5, 5, 100);
        Multimap<String, String> allReachablePairs = HashMultimap.<String, String>create();

        // Step1: pre-match reachable source-sink sootmethod pairs
//        Set<String> hubMethodSet = new HashSet<>(Config.getInstance().isPartialSDGConstruction() ? Global.v().getAppModel().getLocalSootClassSet() : Global.v().getAppModel().getGlobalSootClassSet());
//        try{
//            ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
//
//            List<Callable<Void>> tasks = new ArrayList<>();
//
//            // Iterate over sources and sinks and create tasks
//            for (String source : Global.v().getAppModel().getMethodToSourceAPIsMap().keySet()) {
//                for (String sink : Global.v().getAppModel().getMethodToSinkAPIsMap().keySet()) {
//                    tasks.add(() -> {
//                        for (String hub : graph.getNodes()) {
//                            if (nodeToSCC.containsKey(source) && nodeToSCC.containsKey(sink) && nodeToSCC.containsKey(hub)) {
//                                int fromSCCNum = nodeToSCC.get(source);
//                                int toSCCNum = nodeToSCC.get(sink);
//                                int hubSCCNum = nodeToSCC.get(hub);
//                                if (reachabilityQuery.isReachable(hubSCCNum, toSCCNum) &&
//                                        (reachabilityQuery.isReachable(fromSCCNum, hubSCCNum) || reachabilityQuery.isReachable(hubSCCNum, fromSCCNum))) {
//
//                                    allReachablePairs.compute(source, (key, targetList) -> {
//                                        if (targetList == null) {
//                                            targetList = new HashSet<>();
//                                        }
//                                        targetList.add(sink);
//                                        return targetList;
//                                    });
//                                }
//                            }
//                        }
//                        return null;
//                    });
//                }
//            }
//
//            // Submit all tasks to the executor service
//            List<Future<Void>> futures = executorService.invokeAll(tasks);
//
//            // Wait for all tasks to complete
//            for (Future<Void> future : futures) {
//                future.get();
//            }
//
//            // Shutdown the executor service
//            executorService.shutdown();
//        } catch (Exception e) {
//            log.error(String.valueOf(e));
//        }

        for (String source : Global.v().getAppModel().getMethodToSourceAPIsMap().keySet()) {
            for (String sink : Global.v().getAppModel().getMethodToSinkAPIsMap().keySet()) {
                if (nodeToSCC.containsKey(source) && nodeToSCC.containsKey(sink)) {
                    int fromSCCNum = nodeToSCC.get(source);
                    int toSCCNum = nodeToSCC.get(sink);
                    if (reachabilityQuery.isReachable(fromSCCNum, toSCCNum))
                    {
                        allReachablePairs.put(source, sink);
                    }
                }
            }
        }

        for (String hub : graph.getNodes()) {
            for (String sink : graph.getOutNeighbors(hub)) {
                for (String source : graph.getOutNeighbors(hub)) {
                    if (Global.v().getAppModel().getMethodToSourceAPIsMap().containsKey(source) &&
                            Global.v().getAppModel().getMethodToSinkAPIsMap().containsKey(sink))
                    {
                        allReachablePairs.put(source, sink);
                    }
                }
            }
        }

        return allReachablePairs;
    }

    private static void merge(Collection<Integer> a, Collection<Integer> b, int num) {
        Set<Integer> merged = new HashSet<>(a);
        merged.addAll(b);
        List<Integer> sorted = new ArrayList<>(merged);
        Collections.sort(sorted);
        if (sorted.size() > num) {
            sorted = sorted.subList(0, num);
        }
        a.clear();
        a.addAll(sorted);
    }

    public static class IndependentPermutations {
        private Map<Integer, Integer> permutation;
        private Random random;

        public IndependentPermutations(Set<Integer> nodes) {
            permutation = new HashMap<>();
            random = new Random();
            List<Integer> nodesList = new ArrayList<>(nodes);
            Collections.shuffle(nodesList, random);
            for (Integer node : nodesList) {
                permutation.put(node, nodesList.get(node));
            }
            Global.v().getAppModel().setIndependentPermutation(permutation);
        }

        public Integer getPermutation(Integer node) {
            return permutation.get(node);
        }
    }

    @Getter
    public static class IPConstruct {
        private Map<Integer, List<Integer>> Lin;
        private Map<Integer, List<Integer>> Lout;
        private int k;

        public IPConstruct(BidirectionalGraph<Integer> sccGraph, int k, IndependentPermutations permutations, List<Integer> topOrder, List<Integer> reverseTopOrder) {
            this.k = k;
            Lin = new HashMap<>();
            Lout = new HashMap<>();
            for (Integer node : sccGraph.getNodes()) {
                Lin.put(node, new ArrayList<>(Collections.singletonList(permutations.getPermutation(node))));
                Lout.put(node, new ArrayList<>(Collections.singletonList(permutations.getPermutation(node))));
            }
            computeLabels(sccGraph, topOrder, reverseTopOrder);
        }

        private void computeLabels(BidirectionalGraph<Integer> sccGraph, List<Integer> topOrder, List<Integer> reverseTopOrder) {
            for (Integer u : topOrder) {
                for (Integer w : sccGraph.getInNeighbors(u)) {
                    merge(Lin.get(u), Lin.get(w), k);
                }
            }

            for (Integer u : reverseTopOrder) {
                for (Integer w : sccGraph.getOutNeighbors(u)) {
                    merge(Lout.get(u), Lout.get(w), k);
                }
            }
        }

        public List<Integer> getLin(int node) {
            return Lin.get(node);
        }

        public List<Integer> getLout(int node) {
            return Lout.get(node);
        }
    }

    public static class LevelLabelConstruct {
        private Map<Integer, Integer> upLevel;
        private Map<Integer, Integer> downLevel;

        public LevelLabelConstruct(BidirectionalGraph<Integer> sccGraph, List<Integer> topOrder, List<Integer> reverseTopOrder) {
            upLevel = new HashMap<>();
            downLevel = new HashMap<>();
            computeLevels(sccGraph, topOrder, reverseTopOrder);
        }

        private void computeLevels(BidirectionalGraph<Integer> sccGraph, List<Integer> topOrder, List<Integer> reverseTopOrder) {
            for (Integer node : reverseTopOrder) {
                int maxUp = 0;
                for (Integer neighbor : sccGraph.getOutNeighbors(node)) {
                    maxUp = Math.max(maxUp, upLevel.get(neighbor) + 1);
                }
                upLevel.put(node, maxUp);
            }

            for (Integer node : topOrder) {
                int maxDown = 0;
                for (Integer neighbor : sccGraph.getInNeighbors(node)) {
                    maxDown = Math.max(maxDown, downLevel.get(neighbor) + 1);
                }
                downLevel.put(node, maxDown);
            }
        }

        public int getUpLevel(int node) {
            return upLevel.get(node);
        }

        public int getDownLevel(int node) {
            return downLevel.get(node);
        }
    }

    @Getter
    public static class HVConstruct {
        private Map<Integer, Set<Integer>> HV;
        private Set<Integer> hugeVertices;
        private int h;
        private int mu;

        public HVConstruct(BidirectionalGraph<Integer> sccGraph, List<Integer> topOrder, int h, int mu) {
            HV = new HashMap<>();
            hugeVertices = new HashSet<>();
            this.h = h;
            this.mu = mu;
            computeHVLabels(sccGraph, topOrder, h, mu);
        }

        private void computeHVLabels(BidirectionalGraph<Integer> sccGraph, List<Integer> topOrder, int h, int mu) {
            for (Integer node : sccGraph.getNodes()) {
                Set<Integer> hvSet = new HashSet<>();
                if (sccGraph.getOutNeighbors(node).size() > mu) {
                    hugeVertices.add(node);
                    hvSet.add(node);
                }
                HV.put(node, hvSet);
            }

            for (Integer u : topOrder) {
                for (Integer w : sccGraph.getInNeighbors(u)) {
                    if (!HV.get(u).isEmpty() || !HV.get(w).isEmpty())
                        merge(HV.get(u), HV.get(w), h);
                }
            }
        }

        public Set<Integer> getHVLabel(int node) {
            return HV.get(node);
        }
    }

    public static class ReachabilityQuery {
        private BidirectionalGraph<Integer> graph;
        private IndependentPermutations permutations;
        private IPConstruct ipConstruct;
        private LevelLabelConstruct levelLabels;
        private HVConstruct hvConstruct;

        public ReachabilityQuery(BidirectionalGraph<Integer> sccGraph, List<Integer> topOrder, List<Integer> reverseTopOrder,
                                 int k, int h, int mu)
        {
            graph = sccGraph;
            permutations = new IndependentPermutations(sccGraph.getNodes());
            ipConstruct = new IPConstruct(sccGraph, k, permutations, topOrder, reverseTopOrder);
            levelLabels = new LevelLabelConstruct(sccGraph, topOrder, reverseTopOrder);
            hvConstruct = new HVConstruct(sccGraph, topOrder, h, mu);
        }

        public boolean isReachable(int u, int v) {
            Set<Integer> visited = new HashSet<>();
            return isReachable(u, v, visited);
        }

        public boolean isReachable(int u, int v, Set<Integer> visited) {
            visited.add(u);
            if (u == v)
                return true;
            if (partialOrderGreater(ipConstruct.getLout(u), ipConstruct.getLout(v))
                    || partialOrderGreater(ipConstruct.getLin(v), ipConstruct.getLin(u))) {
                return false;
            }
            if (levelLabels.getUpLevel(u) <= levelLabels.getUpLevel(v)
                || levelLabels.getDownLevel(u) >= levelLabels.getDownLevel(v)) {
                return false;
            }
            if (graph.getOutNeighbors(u).size() > hvConstruct.getMu()) {
                if (hvConstruct.getHVLabel(v).contains(u))
                    return true;
                if (hvConstruct.getHVLabel(v).size() < hvConstruct.getH())
                    return false;
                for (Integer w : hvConstruct.getHVLabel(v)) {
                    if (graph.getOutNeighbors(u).size() > graph.getOutNeighbors(w).size())
                        return false;
                }
            }
            for (Integer w : graph.getOutNeighbors(u)) {
                if (!visited.contains(w)) {
                    if (isReachable(w, v, visited))
                        return true;
                }
            }
            return false;
        }

        public boolean partialOrderGreater (List<Integer> a, List<Integer> b) {
            int max = Collections.max(a);
            // 求集合 b - a
            List<Integer> reduce = b.stream().filter(item -> !a.contains(item)).collect(toList());
            if (!reduce.isEmpty()) {
                for (Integer node : reduce) {
                    if (node < max) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
