package FlowSlicer.GraphStructure;

import java.util.ArrayList;

public class ClosureCalculator {
    private ArrayList<SCCNode> ans;
    private SCCNode start;

    public ClosureCalculator(SCCNode start){
        this.ans = new ArrayList<>();
        this.start = start;
    }

    public void getClosureByDFS(){
        DFS(start);
        start.setSccClosure(ans);
    }

    private void DFS(SCCNode start){
        ans.add(start);
        if(start.getOutDegree() != 0){
            for (Edge<SCCNode> edge : start.getFromEdgeSet()) {
                SCCNode to = edge.getTo();
                for(SCCNode node : to.getSccClosure()){
                    //避免重复加入已存在的类
                    if(!ans.contains(node)){
                        ans.add(node);
                    }
                }
            }
        }
    }
}
