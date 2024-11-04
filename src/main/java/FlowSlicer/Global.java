package FlowSlicer;

import FlowSlicer.XMLObject.FlowDroidResultModel;
import lombok.Getter;
import soot.Unit;

import java.util.ArrayList;
@Getter
public class Global {
    private static final Global instance = new Global();
    private AppModel appModel;
    private FlowDroidResultModel flowDroidResult1;
    private FlowDroidResultModel flowDroidResult2;

    public static Global v() {
        return instance;
    }

    /**
     * initialize the Global instance
     */
    private Global() {
        appModel = new AppModel();
        flowDroidResult1 = new FlowDroidResultModel();
        flowDroidResult2 = new FlowDroidResultModel();
//        a3eModel = new A3EModel();
//        storyModel = new StoryModel();
//        iC3Model = new IC3Model();
//        iC3DialDroidModel = new IC3Model();
//        fragmentModel = new FragmentModel();
//        iCTGModel = new CTGModel();
//        setGatorModel(new GatorModel());
//        setInstrumentList(new ArrayList<Unit>());
//        labeledOracleModel = new LabeledOracleModel();
    }
}
