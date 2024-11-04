package GraphStructure;

import FlowSlicer.GraphStructure.BidirectionalGraph;
import FlowSlicer.Mode.CallGraphUtils;
import org.junit.Test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class testGraphReachability {
    @Test
    public void testSingle() {
        BidirectionalGraph<Integer> graph = new BidirectionalGraph<>();
        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(1, 3);
        graph.addEdge(1, 4);
        graph.addEdge(2, 5);
        graph.addEdge(3, 6);
        graph.addEdge(4, 6);
        graph.addEdge(5, 7);
        graph.addEdge(6, 8);
        graph.addEdge(7, 8);
        graph.addEdge(8, 9);

        List<Integer> topOrder = CallGraphUtils.registerTopologicalOrder(graph);
        List<Integer> reverseTopOrder = CallGraphUtils.registerReverseTopologicalOrder(topOrder);
        CallGraphUtils.ReachabilityQuery reachabilityQuery = new CallGraphUtils.ReachabilityQuery(graph, topOrder, reverseTopOrder, 5, 5, 100);
        Set<Integer> visited = new HashSet<>();
        System.out.println(reachabilityQuery.isReachable(4, 1, visited));
    }

    @Test
    public void testGraphInPaper() {
        BidirectionalGraph<Integer> graph = new BidirectionalGraph<>();
        graph.addEdge(0, 2);
        graph.addEdge(0, 3);
        graph.addEdge(0, 4);
        graph.addEdge(0, 5);
        graph.addEdge(1, 4);
        graph.addEdge(1, 5);
        graph.addEdge(2, 4);
        graph.addEdge(2, 6);
        graph.addEdge(3, 4);
        graph.addEdge(3, 7);
        graph.addEdge(4, 6);
        graph.addEdge(4, 8);
        graph.addEdge(4, 9);
        graph.addEdge(5, 7);
        graph.addEdge(5, 8);
        graph.addEdge(5, 10);
        graph.addEdge(5, 11);
        graph.addEdge(6, 8);

        List<Integer> topOrder = CallGraphUtils.registerTopologicalOrder(graph);
        List<Integer> reverseTopOrder = CallGraphUtils.registerReverseTopologicalOrder(topOrder);
        CallGraphUtils.ReachabilityQuery reachabilityQuery = new CallGraphUtils.ReachabilityQuery(graph, topOrder, reverseTopOrder, 5, 2, 2);
        Set<Integer> visited = new HashSet<>();
        System.out.println(reachabilityQuery.isReachable(5, 6, visited));
    }
}
