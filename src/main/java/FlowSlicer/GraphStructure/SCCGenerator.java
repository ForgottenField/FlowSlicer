package FlowSlicer.GraphStructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public class SCCGenerator {
    private DirectedClassGraph directedClassGraph;
    private ArrayList<ArrayList<GraphNode>> sccList;
    private int[] DFN;//DFN(u)记录节点u的搜索次序(时间戳)
    private int[] LOW;//LOW(u)记录u或u的子树能够追溯到的最早的栈中节点的次序
    private int index;
    private Stack<GraphNode> stack;
    private boolean[] isInStack;

    public SCCGenerator(DirectedClassGraph directedClassGraph){
        this.directedClassGraph = directedClassGraph;
        this.sccList = new ArrayList<>();
        this.DFN = new int[directedClassGraph.getNodeNum()];
        this.LOW = new int[directedClassGraph.getNodeNum()];
        this.index = 0;
        this.stack = new Stack<>();
        this.isInStack = new boolean[directedClassGraph.getNodeNum()];

        //DFN与LOW数组初始值赋为-1
        Arrays.fill(DFN,-1);
        Arrays.fill(LOW,-1);
    }

    public ArrayList<ArrayList<GraphNode>> tarjan(){
        for(int i = 0; i < directedClassGraph.getNodeNum(); ++i){
            if(DFN[i] == -1){
                strongConnect(i);
            }
        }
        return sccList;
    }

    public void strongConnect(int curNode){
        DFN[curNode] = LOW[curNode] = index++;

        // push v onto stack
        GraphNode fromNode = directedClassGraph.getGraphNode(curNode);
        stack.push(fromNode);
        isInStack[curNode] = true;

        // Consider successors of v
        for (Edge<GraphNode> edge : fromNode.getFromEdgeSet()){
            GraphNode toNode = edge.getTo();
            int succNode = toNode.getVertex();
            if(DFN[succNode] == -1){
                // Successor has not yet been visited; recurse on it
                strongConnect(succNode);
                LOW[curNode] = Math.min(LOW[curNode],LOW[succNode]);
            }else if(isInStack[succNode]){
                // Successor w is in stack S and hence in the current SCC
                // If w is not on stack, then (v, w) is an edge pointing to an SCC already found and must be ignored
                // Note: The next line may look odd - but is correct.
                // It says w.index not w.lowlink; that is deliberate and from the original paper
                LOW[curNode] = Math.min(LOW[curNode],DFN[succNode]);
            }
        }

        // If v is a root node, pop the stack and generate an SCC
        ArrayList<GraphNode> scc = new ArrayList<>();
        if(LOW[curNode] == DFN[curNode]){
            int j = -1;
            while (curNode != j){
                GraphNode node = stack.pop();
                j = node.getVertex();
                isInStack[j] = false;
                scc.add(node);
            }
            sccList.add(scc);
        }
    }
}

