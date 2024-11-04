package FlowSlicer.GraphStructure;

import FlowSlicer.Mode.DependenceGraph;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import soot.Unit;

import java.util.HashMap;
import java.util.List;
import java.util.*;
@Getter
@Slf4j
public class BidirectionalGraph<V> {
    private Map<V, Set<V>> outNeighbors;
    private Map<V, Set<V>> inNeighbors;
    private Set<V> nodes;

    public BidirectionalGraph() {
        outNeighbors = new HashMap<>();
        inNeighbors = new HashMap<>();
        nodes = new HashSet<>();
    }

    public void addEdge(V from, V to) {
        if (from != null && to != null) {
            outNeighbors.computeIfAbsent(from, k -> new HashSet<>()).add(to);
            inNeighbors.computeIfAbsent(to, k -> new HashSet<>()).add(from);
            nodes.add(from);
            nodes.add(to);
        } else {
            log.error("The edge: " + from + " -> " + to + " exists null variable!");
        }
    }

    public void addIsolatedNode (V node) {
        if (node != null) {
            nodes.add(node);
            Set<V> outNeighber = new HashSet<>();
            Set<V> inNeighber = new HashSet<>();
            outNeighbors.put(node, outNeighber);
            inNeighbors.put(node, inNeighber);
        } else {
            log.error("The node " + node + " is null!");
        }
    }

    public Set<V> getOutNeighbors(V node) {
        return outNeighbors.getOrDefault(node, new HashSet<>());
    }

    public Set<V> getInNeighbors(V node) {
        return inNeighbors.getOrDefault(node, new HashSet<>());
    }

    public int size() {
        return nodes.size();
    }

    public void mergeWith(BidirectionalGraph<V> graph) {
        // Merge nodes
        this.nodes.addAll(graph.getNodes());

        // Merge outNeighbors
        for (V fromNode : graph.getOutNeighbors().keySet()) {
            if (this.outNeighbors.containsKey(fromNode)) {
                this.outNeighbors.get(fromNode).addAll(graph.getOutNeighbors().get(fromNode));
            } else {
                this.outNeighbors.put(fromNode, graph.getOutNeighbors().get(fromNode));
            }
        }

        // Merge inNeighbors
        for (V fromNode : graph.getInNeighbors().keySet()) {
            if (this.inNeighbors.containsKey(fromNode)) {
                this.inNeighbors.get(fromNode).addAll(graph.getInNeighbors().get(fromNode));
            } else {
                this.inNeighbors.put(fromNode, graph.getInNeighbors().get(fromNode));
            }
        }
    }
}
