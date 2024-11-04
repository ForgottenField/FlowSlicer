package FlowSlicer.GraphStructure;

import java.util.ArrayList;

public class DirectedClassGraph extends DirectedGraph<GraphNode> {
    private ArrayList<ArrayList<GraphNode>> sccList; // record every SCC in directed class graph

    public DirectedClassGraph() {
        super();
        this.sccList = new ArrayList<>();
    }

    /**
     根据类名获取对应的顶点，传入的类名可能是后缀
     **/
    public GraphNode getGraphNode(String className) {
        for(GraphNode node : nodeList) {
            if(node.getClassName().endsWith(className)){
                return node;
            }
        }
//        throw new RuntimeException("no GraphNode for " + className);
        return null;
    }

    /**
     * get scc list of directed class graph
     * @return ArrayList<ArrayList<GraphNode>>
     */
    public ArrayList<ArrayList<GraphNode>> getSccList(){
        return this.sccList;
    }

    /**
     * set SCC list of directed class graph
     * @param sccList
     */
    public void setSccList(ArrayList<ArrayList<GraphNode>> sccList){
        this.sccList = sccList;
    }

    public void addEdge(Edge<GraphNode> edge) {
        //新增一条边,直接遍历列表,如果存在这条的起始节点，则将这条边加入。
        Integer from = edge.getFrom().getVertex();
        Integer to = edge.getTo().getVertex();

        for(GraphNode graphNode : nodeList) {
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

    public Edge<GraphNode> getEdge(GraphNode from, GraphNode to) {
        return from.get(to.getVertex());
    }
}
