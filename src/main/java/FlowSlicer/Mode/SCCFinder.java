package FlowSlicer.Mode;
import FlowSlicer.GraphStructure.BidirectionalGraph;
import lombok.Getter;
import soot.Unit;

import java.util.*;
@Getter
public class SCCFinder {
    private BidirectionalGraph<String> graph;
    private List<Set<String>> sccComponents;
    private Map<String, Integer> nodeToSCC;
    private int index;
    private Stack<String> stack;
    private Map<String, Integer> indexes;
    private Map<String, Integer> lowLinks;
    private Set<String> onStack;

    public SCCFinder(BidirectionalGraph<String> graph) {
        this.graph = graph;
        this.sccComponents = new ArrayList<>();
        this.nodeToSCC = new HashMap<>();
        this.index = 0;
        this.stack = new Stack<>();
        this.indexes = new HashMap<>();
        this.lowLinks = new HashMap<>();
        this.onStack = new HashSet<>();
        computeSCCs();
    }

    private void computeSCCs() {
        for (String node : graph.getNodes()) {
            if (!indexes.containsKey(node)) {
                tarjan(node);
            }
        }
    }

    private void tarjan(String node) {
        indexes.put(node, index);
        lowLinks.put(node, index);
        index++;
        stack.push(node);
        onStack.add(node);

        for (String neighbor : graph.getOutNeighbors(node)) {
            if (!indexes.containsKey(neighbor)) {
                tarjan(neighbor);
                lowLinks.put(node, Math.min(lowLinks.get(node), lowLinks.get(neighbor)));
            } else if (onStack.contains(neighbor)) {
                lowLinks.put(node, Math.min(lowLinks.get(node), indexes.get(neighbor)));
            }
        }

        if (lowLinks.get(node).equals(indexes.get(node))) {
            Set<String> scc = new HashSet<>();
            String w;
            do {
                w = stack.pop();
                onStack.remove(w);
                scc.add(w);
                nodeToSCC.put(w, sccComponents.size());
            } while (!w.equals(node));
            sccComponents.add(scc);
        }
    }
}
