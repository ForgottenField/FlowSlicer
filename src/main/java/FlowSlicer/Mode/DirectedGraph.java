package FlowSlicer.Mode;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public interface DirectedGraph<N> extends Iterable<N> {
    public List<N> getHeads();

    /** Returns a list of exit points for this graph. */
    public List<N> getTails();

    /**
     * Returns a list of predecessors for the given node in the graph.
     */
    public Set<N> getPredsOf(N s);

    /**
     * Returns a list of successors for the given node in the graph.
     */
    public Set<N> getSuccsOf(N s);

    /**
     * Returns the node count for this graph.
     */
    public int size();

    /**
     * Returns an iterator for the nodes in this graph. No specific ordering of the nodes is guaranteed.
     */
    @Override
    public Iterator<N> iterator();
}
