package FlowSlicer;

import FlowSlicer.Mode.DependenceGraph;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class Statistics {
    public static final String TIMER_BUILDING_PDGS = "PDG construction: ";
    public static final String TIMER_Finding_SlicingCriteria = "Finding Slicing Criteria: ";
    public static final String TIMER_Updating_SlicingCriteria = "Updating Slicing Criteria: ";
    public static final String TIMER_Handling_Exceptions = "Handling Exceptions: ";
    public static final String TIMER_Building_Dummymain = "Building Dummymain: ";
    public static final String TIMER_Locating_Source_Sink = "Locating Source and Sink: ";
    public static final String TIMER_Reachability_Query = "Reachability Query: ";
    public static final String TIMER_Constructing_Local_Graph = "Constructing Local Graph: ";
    public static final String TIMER_Adding_Callbacks = "Adding Callbacks: ";
    public static final String TIMER_BUILDING_SDG = "SDG construction: ";
    public static final String TIMER_Slicing_SDG = "SDG Slicing: ";
    public static final String TIMER_Slicing_SDG_Backward_And_Forward = "SDG Slicing Backward and Forward: ";
    public static final String TIMER_Slicing_SDG_CSR = "SDG Slicing CSR: ";
    public static final String TIMER_Slicing_SDG_Extra = "SDG Extra: ";
    public static final String TIMER_Slicing_CFG = "CFG Slicing: ";
    public static String[] COUNTER_SDG_TYPED_EDGES;
    private boolean recordStatistics;
    @Getter
    private Map<String, Timer> timers;
    @Getter
    private Timer flowSlicerTimer;
    @Getter
    private Timer flowDroidTimer1;
    @Getter
    private Timer flowDroidTimer2;
    private Map<String, Counter> counters;
    @Getter
    private static Statistics instance = new Statistics();

    private Statistics() {
        this.recordStatistics = true;
        this.timers = new HashMap<>();
        this.counters = new HashMap<>();
        this.flowSlicerTimer = new Timer("FlowSlicer in total: ");
        this.flowDroidTimer1 = new Timer("FlowDroid in total(before slicing): ");
        this.flowDroidTimer2 = new Timer("FlowDroid in total(after slicing): ");

        COUNTER_SDG_TYPED_EDGES = new String[14];
        COUNTER_SDG_TYPED_EDGES[DependenceGraph.EDGE_TYPE_CONTROL_ENTRY] = "Control-Entry: ";
        COUNTER_SDG_TYPED_EDGES[DependenceGraph.EDGE_TYPE_CONTROL] = "Control: ";
        COUNTER_SDG_TYPED_EDGES[DependenceGraph.EDGE_TYPE_DATA] = "Data: ";
        COUNTER_SDG_TYPED_EDGES[DependenceGraph.EDGE_TYPE_PARAMETER_IN] = "Parameter-In: ";
        COUNTER_SDG_TYPED_EDGES[DependenceGraph.EDGE_TYPE_PARAMETER_OUT] = "Paramenter-Out: ";
        COUNTER_SDG_TYPED_EDGES[DependenceGraph.EDGE_TYPE_CALL] = "Call: ";
        COUNTER_SDG_TYPED_EDGES[DependenceGraph.EDGE_TYPE_SUMMARY] = "Summary: ";
        COUNTER_SDG_TYPED_EDGES[DependenceGraph.EDGE_TYPE_FIELD_DATA] = "Field-Data: ";
        COUNTER_SDG_TYPED_EDGES[DependenceGraph.EDGE_TYPE_STATIC_FIELD_DATA] = "Static-Field-Data: ";
        COUNTER_SDG_TYPED_EDGES[DependenceGraph.EDGE_TYPE_CALLBACK] = "Callback: ";
        COUNTER_SDG_TYPED_EDGES[DependenceGraph.EDGE_TYPE_CALLBACK_DEFINITION] = "Callback-Definition: ";
        COUNTER_SDG_TYPED_EDGES[DependenceGraph.EDGE_TYPE_CALLBACK_ALTERNATIVE] = "Callback-Alternative: ";
        COUNTER_SDG_TYPED_EDGES[DependenceGraph.EDGE_TYPE_EXCEPTION] = "Exception: ";
        COUNTER_SDG_TYPED_EDGES[DependenceGraph.EDGE_TYPE_ICC] = "ICC: ";
    }
    public static Timer getTimer(String title) {
        if (!instance.timers.containsKey(title)) {
            instance.timers.put(title, new Timer(title));
        }
        return instance.timers.get(title);
    }

    public static Counter getCounter(String title) {
        if (!instance.counters.containsKey(title)) {
            instance.counters.put(title, new Counter());
        }
        return instance.counters.get(title);
    }
}
