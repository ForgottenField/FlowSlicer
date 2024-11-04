package FlowSlicer.Mode;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizV8Engine;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import soot.SootField;
import soot.Unit;
import soot.util.dot.DotGraph;
import soot.util.dot.DotGraphConstants;
import soot.util.dot.DotGraphEdge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GraphDrawer {
    private static final Integer TYPE_INVISIBLE = -42;
    private DependenceGraph graph;
    private Collection<Unit> slicedUnits;
    private Set<Unit> targetListTo;
    private Set<Unit> targetListFrom;
    private DotGraph dot;

    public GraphDrawer(String name, DependenceGraph graph, Set<Unit> targetTo, Set<Unit> targetFrom) {
        this.graph = graph;
        this.slicedUnits = null;
        this.dot = new DotGraph(name);
        this.dot.setNodeShape(DotGraphConstants.NODE_SHAPE_BOX);
        this.targetListFrom = targetFrom;
        this.targetListTo = targetTo;
    }

    public GraphDrawer(String name, DependenceGraph graph, DependenceGraph graphSliced, Set<Unit> targetFrom, Set<Unit> targetTo) {
        this(name, graph, targetTo, targetFrom);
        this.slicedUnits = graphSliced.getAllNodes();
    }

    public GraphDrawer(String name, DependenceGraph graph, Collection<Unit> slicedUnits, Set<Unit> targetFrom, Set<Unit> targetTo) {
        this(name, graph, targetTo, targetFrom);
        this.slicedUnits = slicedUnits;
    }

    public void drawGraph(String output) {
        drawGraph(output, 0);
    }

    public void drawGraph(String output, int level) {
        // Legend
        drawLegendEdge(DependenceGraph.EDGE_TYPE_CONTROL_ENTRY, "entry");
        drawLegendEdge(DependenceGraph.EDGE_TYPE_CONTROL, "control");
        drawLegendEdge(DependenceGraph.EDGE_TYPE_EXCEPTION, "exception");
        drawLegendEdge(DependenceGraph.EDGE_TYPE_DATA, "data");
        drawLegendEdge(DependenceGraph.EDGE_TYPE_FIELD_DATA, "field");
        drawLegendEdge(DependenceGraph.EDGE_TYPE_STATIC_FIELD_DATA, "staticField");
        final Set<Integer> types = new HashSet<>();
        types.add(DependenceGraph.EDGE_TYPE_DATA);
        types.add(DependenceGraph.EDGE_TYPE_CONTROL);
        drawLegendEdge(types, "control & data");
        drawLegendEdge(DependenceGraph.EDGE_TYPE_CALL, "call");
        drawLegendEdge(DependenceGraph.EDGE_TYPE_PARAMETER_IN, "parameterIN");
        drawLegendEdge(DependenceGraph.EDGE_TYPE_PARAMETER_OUT, "parameterOUT");
        drawLegendEdge(DependenceGraph.EDGE_TYPE_SUMMARY, "summary");
        drawLegendEdge(DependenceGraph.EDGE_TYPE_CALLBACK, "callback");
        drawLegendEdge(DependenceGraph.EDGE_TYPE_CALLBACK_DEFINITION, "callbackDEF");
        drawLegendEdge(DependenceGraph.EDGE_TYPE_CALLBACK_ALTERNATIVE, "callbackALT");
        drawLegendEdge(DependenceGraph.EDGE_TYPE_ICC, "icc");
        drawLegendSliceNode();
        drawLegendExtraNode();
        drawLegendFieldFilteringNode();
        drawLegendContextSensitiveRefinementNode();

        // Generate all edges (nodes are generated implicitly)
        final Set<Integer> edgesAdded = new HashSet<>();
        for (final Unit from : this.graph) {
            for (final Unit to : this.graph.getPredsOf(from)) {
                final Integer edgeString = new Edge(to, from).hashCode();
                if (!edgesAdded.contains(edgeString)) {
                    drawEdge(to, from);
                    edgesAdded.add(edgeString);
                }
            }
            for (final Unit to : this.graph.getSuccsOf(from)) {
                final Integer edgeString = new Edge(from, to).hashCode();
                if (!edgesAdded.contains(edgeString)) {
                    drawEdge(from, to);
                    edgesAdded.add(edgeString);
                }
            }
        }

        // Mark nodes to be sliced
        markSlicingNodes();

        // Render image
        final File dotFile = new File(output + ".dot");
        final File svgFile = new File(output + ".svg");
        try {
            Graphviz.useEngine(new GraphvizV8Engine());

            if (dotFile.getParentFile() != null && !dotFile.getParentFile().exists()) {
                dotFile.getParentFile().mkdirs();
            }

            final FileOutputStream os = new FileOutputStream(dotFile);
            this.dot.render(os, level);
            os.close();

            final FileInputStream is = new FileInputStream(dotFile);
            final MutableGraph g = new Parser().read(is);

            Graphviz.fromGraph(g).render(Format.SVG).toFile(svgFile);
            is.close();
        } catch (final Exception e) {
            System.out.println("Could not output debugging graph: " + svgFile.getAbsolutePath());
            System.out.println(e);
        }
    }

    private void drawLegendEdge(int type, String label) {
        final Set<Integer> types = new HashSet<>();
        types.add(type);
        drawLegendEdge(types, label);
    }

    private void drawLegendEdge(Set<Integer> types, String label) {
        drawEdge(label + ">", "<" + label, types);
        this.dot.getNode(label + ">").setAttribute("style", "dashed");
        this.dot.getNode("<" + label).setAttribute("style", "dashed");

        types.clear();
        types.add(TYPE_INVISIBLE);
        drawEdge("<" + label, this.graph.nodeToString(this.graph.getRoot()), types);
    }

    private void drawLegendSliceNode() {
        final String label = "In Slice";
        this.dot.getNode(label).setAttribute("fontcolor", "white");
        this.dot.getNode(label).setAttribute("fillcolor", "black");
        this.dot.getNode(label).setStyle(DotGraphConstants.NODE_STYLE_FILLED);

        final Set<Integer> types = new HashSet<>();
        types.add(TYPE_INVISIBLE);
        drawEdge(label, this.graph.nodeToString(this.graph.getRoot()), types);
    }

    private void drawLegendExtraNode() {
        final String label = "In Extra-Slice";
        this.dot.getNode(label).setAttribute("fillcolor", "gray");
        this.dot.getNode(label).setStyle(DotGraphConstants.NODE_STYLE_FILLED);

        final Set<Integer> types = new HashSet<>();
        types.add(TYPE_INVISIBLE);
        drawEdge(label, this.graph.nodeToString(this.graph.getRoot()), types);
    }

    private void drawLegendFieldFilteringNode() {
        final String label = "Field-Filtered";
        this.dot.getNode(label).setAttribute("color", "brown");
        this.dot.getNode(label).setAttribute("penwidth", "3");

        final Set<Integer> types = new HashSet<>();
        types.add(TYPE_INVISIBLE);
        drawEdge(label, this.graph.nodeToString(this.graph.getRoot()), types);
    }

    private void drawLegendContextSensitiveRefinementNode() {
        final String label = "Context-Sensitive-Refinement";
        this.dot.getNode(label).setAttribute("color", "brown");
        this.dot.getNode(label).setAttribute("style", "dashed");
        this.dot.getNode(label).setAttribute("penwidth", "3");

        final Set<Integer> types = new HashSet<>();
        types.add(TYPE_INVISIBLE);
        drawEdge(label, this.graph.nodeToString(this.graph.getRoot()), types);
    }

    private void drawEdge(Unit from, Unit to) {
        drawEdge(this.graph.nodeToString(from), this.graph.nodeToString(to), this.graph.getEdgeTypes(from, to),
                Packs.getInstance().getFieldEdgeLabel(new Edge(from, to)));
    }

    private void drawEdge(String from, String to, Set<Integer> types) {
        drawEdge(from, to, types, null);
    }

    private void drawEdge(String from, String to, Set<Integer> types, SootField label) {
        final DotGraphEdge edge = this.dot.drawEdge(from, to);
        if (label != null) {
            edge.setAttribute("fontcolor", "blue");
            edge.setLabel("[" + label.getName() + "]");
        }
        if (this.graph instanceof DependenceGraph) {
            if (types != null && types.contains(TYPE_INVISIBLE)) {
                edge.setAttribute("color", "white");
                edge.setAttribute("penwidth", "0");
                edge.setAttribute("arrowsize", "0");
            } else {
                if (types != null) {
                    if (types.contains(DependenceGraph.EDGE_TYPE_PARAMETER_IN)) {
                        edge.setAttribute("color", "purple");
                        edge.setAttribute("style", "dashed");
                    } else if (types.contains(DependenceGraph.EDGE_TYPE_PARAMETER_OUT)) {
                        edge.setAttribute("color", "purple");
                        edge.setAttribute("style", "dotted");
                    } else if (types.contains(DependenceGraph.EDGE_TYPE_CONTROL)
                            && types.contains(DependenceGraph.EDGE_TYPE_DATA)) {
                        edge.setAttribute("color", "darkgreen");
                        edge.setAttribute("fillcolor", "blue");
                    } else if (types.contains(DependenceGraph.EDGE_TYPE_CONTROL_ENTRY)) {
                        edge.setAttribute("color", "darkgreen");
                        edge.setAttribute("fillcolor", "white");
                    } else if (types.contains(DependenceGraph.EDGE_TYPE_CONTROL)) {
                        edge.setAttribute("color", "darkgreen");
                    } else if (types.contains(DependenceGraph.EDGE_TYPE_EXCEPTION)) {
                        edge.setAttribute("color", "darkgreen");
                        edge.setAttribute("style", "dashed");
                    } else if (types.contains(DependenceGraph.EDGE_TYPE_DATA)
                            && types.contains(DependenceGraph.EDGE_TYPE_FIELD_DATA)) {
                        edge.setAttribute("color", "blue");
                        edge.setAttribute("style", "dashed");
                    } else if (types.contains(DependenceGraph.EDGE_TYPE_DATA)
                            && types.contains(DependenceGraph.EDGE_TYPE_STATIC_FIELD_DATA)) {
                        edge.setAttribute("color", "blue");
                        edge.setAttribute("style", "dotted");
                    } else if (types.contains(DependenceGraph.EDGE_TYPE_DATA)) {
                        edge.setAttribute("color", "blue");
                        edge.setAttribute("fillcolor", "white");
                    } else if (types.contains(DependenceGraph.EDGE_TYPE_FIELD_DATA)) {
                        edge.setAttribute("color", "blue");
                        edge.setAttribute("style", "dashed");
                        edge.setAttribute("fillcolor", "white");
                    } else if (types.contains(DependenceGraph.EDGE_TYPE_STATIC_FIELD_DATA)) {
                        edge.setAttribute("color", "blue");
                        edge.setAttribute("style", "dotted");
                        edge.setAttribute("fillcolor", "white");
                    } else if (types.contains(DependenceGraph.EDGE_TYPE_CALL)) {
                        edge.setAttribute("color", "purple");
                    } else if (types.contains(DependenceGraph.EDGE_TYPE_SUMMARY)) {
                        edge.setAttribute("color", "brown");
                        edge.setAttribute("style", "dashed");
                    } else if (types.contains(DependenceGraph.EDGE_TYPE_CALLBACK)) {
                        edge.setAttribute("color", "gray");
                        edge.setAttribute("style", "dashed");
                    } else if (types.contains(DependenceGraph.EDGE_TYPE_CALLBACK_DEFINITION)) {
                        edge.setAttribute("color", "gray");
                    } else if (types.contains(DependenceGraph.EDGE_TYPE_CALLBACK_ALTERNATIVE)) {
                        edge.setAttribute("color", "gray");
                        edge.setAttribute("style", "dotted");
                    } else if (types.contains(DependenceGraph.EDGE_TYPE_ICC)) {
                        edge.setAttribute("color", "orange");
                    } else {
                        edge.setAttribute("color", "red");
                    }
                } else {
                    edge.setAttribute("color", "red");
                }
                edge.setAttribute("penwidth", "2");
                edge.setAttribute("arrowsize", "2");
            }
        }
    }

    private void markSlicingNodes() {
        for (final Unit nodeUnit : this.graph.getAllNodes()) {
            if (this.targetListFrom.contains(nodeUnit)){
                this.dot.getNode(this.graph.nodeToString(nodeUnit)).setAttribute("fillcolor", "red");
                this.dot.getNode(this.graph.nodeToString(nodeUnit)).setAttribute("penwidth", "3");
            } else if (this.targetListTo.contains(nodeUnit)){
                this.dot.getNode(this.graph.nodeToString(nodeUnit)).setAttribute("fillcolor", "orange");
                this.dot.getNode(this.graph.nodeToString(nodeUnit)).setAttribute("penwidth", "3");
            } else {
                this.dot.getNode(this.graph.nodeToString(nodeUnit)).setAttribute("penwidth", "1");
            }
        }
        if (this.slicedUnits != null) {
            for (final Unit nodeUnit : this.slicedUnits) {
                if (!this.targetListTo.contains(nodeUnit) && !this.targetListFrom.contains(nodeUnit)) {
                    this.dot.getNode(this.graph.nodeToString(nodeUnit)).setAttribute("fontcolor", "white");
                    this.dot.getNode(this.graph.nodeToString(nodeUnit)).setAttribute("fillcolor", "black");
                }
                this.dot.getNode(this.graph.nodeToString(nodeUnit)).setStyle(DotGraphConstants.NODE_STYLE_FILLED);
            }
        }
    }
}
