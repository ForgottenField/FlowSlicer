package FlowSlicer;

import org.xmlpull.v1.XmlPullParserException;
import soot.PackManager;
import soot.Scene;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.methodSummary.data.provider.LazySummaryProvider;
import soot.jimple.infoflow.methodSummary.taintWrappers.SummaryTaintWrapper;
import soot.options.Options;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class FlowdroidHelper {
    public static void runInfoflow(String appPath, String outputDir) throws URISyntaxException, IOException, XmlPullParserException
    {
        SetupApplication app = new SetupApplication(Config.getInstance().getAndroidJar(), appPath);
        app.setTaintWrapper(new SummaryTaintWrapper(new LazySummaryProvider("summariesManual")));
        app.getConfig().getAnalysisFileConfig().setSourceSinkFile("SourcesAndSinks_COVA.txt");
        app.getConfig().getAnalysisFileConfig().setOutputFile(outputDir);
        app.getConfig().setImplicitFlowMode(InfoflowConfiguration.ImplicitFlowMode.AllImplicitFlows);
//        app.getConfig().getCallbackConfig().setCallbackAnalyzer(InfoflowAndroidConfiguration.CallbackAnalyzer.Fast);
//        app.getConfig().getCallbackConfig().setCallbackAnalysisTimeout(300);
//        app.getConfig().setDataFlowTimeout(300);
//        System.out.println(app.getConfig().getMemoryThreshold());
//        app.getConfig().setMemoryThreshold(0.5);
        app.setCallbackFile("AndroidCallbacks.txt");
        app.runInfoflow();
    }
}
