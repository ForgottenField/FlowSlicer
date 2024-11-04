package FlowSlicer.Mode;

import FlowSlicer.Config;
import FlowSlicer.Global;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.xmlpull.v1.XmlPullParserException;
import polyglot.ast.Switch;
import soot.JastAddJ.Opt;
import soot.Printer;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
@Slf4j
public class LifeCycleHandler {
    private final String apkPath;
    @Getter
    private Collection<SootMethod> dummyMains;
    @Getter
    private SootClass dummyMainClass;
    @Getter
    private CallGraph cg;

    public LifeCycleHandler(String apkPath) {
        this.apkPath = apkPath;
        this.dummyMains = null;
        this.dummyMainClass = null;
        this.cg = null;
    }

    public Collection<SootMethod> getDummyMainMethods() {
        if (this.dummyMains != null) {
            return this.dummyMains;
        }

        // Construct call graph
        final InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
        config.getAnalysisFileConfig().setTargetAPKFile(this.apkPath);
        config.getAnalysisFileConfig().setAndroidPlatformDir(Config.getInstance().getAndroidJar());

//        if (Config.getInstance().getMode().equals(Config.FLOW_SLICE_MODE)) {
//            config.setSootIntegrationMode(InfoflowConfiguration.SootIntegrationMode.UseExistingInstance);
//        } else if (Config.getInstance().getMode().equals(Config.MATCH_SLICE_MODE)) {
//            config.setSootIntegrationMode(InfoflowConfiguration.SootIntegrationMode.CreateNewInstance);
//        }
        config.setSootIntegrationMode(InfoflowConfiguration.SootIntegrationMode.UseExistingInstance);
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);

        switch (Config.getInstance().getCallGraphAlgorithm()) {
            case "CHA":
                config.setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.CHA);
                break;
            case "SPARK":
                config.setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.SPARK);
                break;
            default:
                config.setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.AutomaticSelection);
                break;
        }

        config.setTaintAnalysisEnabled(false);
        config.setExcludeSootLibraryClasses(true);
        config.setImplicitFlowMode(InfoflowConfiguration.ImplicitFlowMode.AllImplicitFlows);
        // Setting the fast callbackAnalyzer may cause some apps to construct callgraph unsuccessfully.
//        config.getCallbackConfig().setCallbackAnalyzer(InfoflowAndroidConfiguration.CallbackAnalyzer.Fast);
        config.getCallbackConfig().setCallbackAnalysisTimeout(180);
        config.setMergeDexFiles(true);

        final SetupApplication sa = new SetupApplication(config);
        Options.v().setPhaseOption("cg", "enabled:true");
        Options.v().set_exclude(SootHelper.getExcludeList());

        sa.constructCallgraph();

        // Get dummy main
        List<SootMethod> dummyMains;
        try {
            final SootMethod dummyMain = sa.getDummyMainMethod();
            this.dummyMainClass = dummyMain.getDeclaringClass();

            // Get dummy mains
            dummyMains = this.dummyMainClass.getMethods();
            for (SootMethod sm : dummyMains) {
                Global.v().getAppModel().getGlobalSootMethodSet().add(sm.getSignature());
//                Global.v().getAppModel().getLocalSootClassSet().add(sm.getSignature());
            }
        } catch (final NullPointerException e) {
            log.error("Dummy main could not be created (by FlowDroid)!");
            dummyMains = new ArrayList<>();
        }

        // Get callgraph
        this.cg = Scene.v().getCallGraph();

        this.dummyMains = dummyMains;
        return this.dummyMains;
    }
}
