package FlowSlicer.GraphStructure;

import java.util.ArrayList;
import java.util.List;

public class DirectedGraph<V> {
    protected List<V> nodeList;
    protected List<Edge<V>> edgeList;
    protected int nodeNum;
    protected int edgeNum;

    public DirectedGraph() {
        this.nodeList = new ArrayList<>();
        this.edgeList = new ArrayList<>();
        this.nodeNum = 0;
        this.edgeNum = 0;
    }

    public void addVertex(V node) {
        this.nodeList.add(node);
        nodeNum++;
    }

    public V getGraphNode(int index) {
        return nodeList.get(index);
    }

    public List<V> getNodeList(){
        return this.nodeList;
    }

    public int getNodeNum(){
        return nodeNum;
    }

    public List<Edge<V>> getEdgeList(){
        return this.edgeList;
    }

    public int getEdgeNum(){
        return edgeNum;
    }

    public void setNodeNum(int nodeNum){
        this.nodeNum = nodeNum;
    }
}
