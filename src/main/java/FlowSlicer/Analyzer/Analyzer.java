package FlowSlicer.Analyzer;

import FlowSlicer.AppModel;
import FlowSlicer.Global;

public abstract class Analyzer {
    public AppModel appModel;

    public Analyzer() {
        this.appModel = Global.v().getAppModel();
    }

    public abstract void analyze();
}
