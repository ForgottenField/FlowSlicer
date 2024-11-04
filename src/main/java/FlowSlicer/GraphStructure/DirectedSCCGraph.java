package FlowSlicer.GraphStructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectedSCCGraph extends DirectedGraph<SCCNode> {
    private final List<SCCNode> topoNodeList;
    private Map<GraphNode, Integer> graphNodeToSCCVertexMap;

    public DirectedSCCGraph() {
        super();
        this.topoNodeList = new ArrayList<>();
        this.graphNodeToSCCVertexMap = new HashMap<>();
    }

    /**
     * get the topological sort of SCCNode in DAG
     * @return List<SCCNode>
     */
    public List<SCCNode> getTopoNodeList(){
        return this.topoNodeList;
    }

    public void addEdge(Edge<SCCNode> edge) {
        //新增一条边,直接遍历列表,如果存在这条的起始节点，则将这条边加入。
        Integer from = edge.getFrom().getVertex();
        Integer to = edge.getTo().getVertex();

        for(SCCNode graphNode : nodeList) {
            Integer vertex = graphNode.getVertex();

            if(from.equals(vertex)){
                graphNode.addFromEdge(edge);
            }

            if(to.equals(vertex)){
                graphNode.addToEdge(edge);
            }
        }

        this.edgeList.add(edge);
        this.edgeNum++;
    }

    public Edge<SCCNode> getEdge(SCCNode from, SCCNode to) {
        return from.get(to.getVertex());
    }

    public Map<GraphNode, Integer> getGraphNodeToSCCVertexMap () {
        return this.graphNodeToSCCVertexMap;
    }
}
