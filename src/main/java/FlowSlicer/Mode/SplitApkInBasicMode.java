package FlowSlicer.Mode;

import FlowSlicer.Config;
import FlowSlicer.Global;
import FlowSlicer.GraphStructure.CDGHelper;
import FlowSlicer.GraphStructure.DirectedClassGraph;
import FlowSlicer.GraphStructure.DirectedSCCGraph;
import FlowSlicer.RefactorTool.SplitAPK;
import FlowSlicer.Statistics;
import FlowSlicer.XMLObject.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import soot.*;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Getter
public class SplitApkInBasicMode extends SplitAPK {
    public SplitApkInBasicMode() {
        super();
    }

    @Override
    public void splitApk(){
        // Read ICCFile
        // to-do:
        // ICC parsing needs to be re-implemented
        String ICCFilePath = Config.getInstance().getICCFilePath();
        ICCEdgesHandler.parseICCFile(ICCFilePath);

        // Find slicing targets
        Statistics.getTimer(Statistics.TIMER_Finding_SlicingCriteria).start();
        XMLHelper.parseFlowDroidXmlFile(Config.getInstance().getFlowDroidResultPath(), Global.v().getFlowDroidResult1());

        findSlicingTargets(Global.v().getFlowDroidResult1().getSourceList(),
                Global.v().getFlowDroidResult1().getSinkList(),
                Global.v().getFlowDroidResult1().getSinkToSourcesMap());

        if(!appModel.getToCriteriaSet().isEmpty() && !appModel.getFromCriteriaSet().isEmpty() &&
                !appModel.getSinkToSourceUnitMultimap().isEmpty() && !appModel.getSourceToSinkUnitMultimap().isEmpty()){
            log.info("Successfully find Slicing Targets\n");
        } else {
            log.error("Cannot read any slicing criteria!\n");
            System.exit(0);
        }
        Statistics.getTimer(Statistics.TIMER_Finding_SlicingCriteria).stop();

        // Construct Class Graph and avoid doing a whole-case analysis
        DirectedClassGraph cdg = CDGHelper.constructCDG();
        DirectedSCCGraph dag = CDGHelper.constructDSCCG(cdg);
        Set<String> inputClassSet = new HashSet<>();
        for (Reference source : Global.v().getFlowDroidResult1().getSourceList()) {
            String className = source.getClassname();
            if (!SootHelper.isOrdinaryLibraryClass(className)) {
                inputClassSet.add(className);
            }
        }
        for (Reference sink : Global.v().getFlowDroidResult1().getSinkList()) {
            String className = sink.getClassname();
            if (!SootHelper.isOrdinaryLibraryClass(className)) {
                inputClassSet.add(className);
            }
        }
        HashSet<String> closureSet = CDGHelper.constructClosure(cdg, dag, inputClassSet);
        appModel.setLocalSootClassSet(closureSet);

//        for (String removedClassName : appModel.getGlobalSootClassSet()) {
//            if (!closureSet.contains(removedClassName)) {
//                SootClass removedSootClass = Scene.v().getSootClass(removedClassName);
//                Chain<SootClass> classChain = Scene.v().getApplicationClasses();
//                Scene.v().removeClass(removedSootClass);
//                Chain<SootClass> classChainAfterRemove = Scene.v().getApplicationClasses();
//            }
//        }

        // Setup PDG generation and Build PDGs (Run Soot)
        Statistics.getTimer(Statistics.TIMER_BUILDING_PDGS).start();
        if (Config.getInstance().isPartialSDGConstruction()) {
            PackManager.v().getPack("jtp").add(new Transform("jtp.PDGTransformer", new PDGTransformer(appModel.getLocalSootClassSet())));
        } else {
            PackManager.v().getPack("jtp").add(new Transform("jtp.PDGTransformer", new PDGTransformer(appModel.getGlobalSootClassSet())));
        }
        PackManager.v().runPacks();
        Statistics.getTimer(Statistics.TIMER_BUILDING_PDGS).stop();

        // Handle Exceptions
        Statistics.getTimer(Statistics.TIMER_Handling_Exceptions).start();
        handleExceptions();
        Statistics.getTimer(Statistics.TIMER_Handling_Exceptions).stop();

        // Build Dummy Main (FlowDroid)
        Statistics.getTimer(Statistics.TIMER_Building_Dummymain).start();
        buildDummyMainPDG();
        Statistics.getTimer(Statistics.TIMER_Building_Dummymain).stop();

        // Add Callback edges
        Statistics.getTimer(Statistics.TIMER_Adding_Callbacks).start();
        addCallBackEdges();
        Statistics.getTimer(Statistics.TIMER_Adding_Callbacks).stop();

        // Build SDG
        Statistics.getTimer(Statistics.TIMER_BUILDING_SDG).start();
        final PDGConnector connector = new PDGConnector();
        final DependenceGraph sdg = connector.buildSDG();
        Statistics.getTimer(Statistics.TIMER_BUILDING_SDG).stop();

        ICCEdgesHandler.addInputEdges(sdg);
        appModel.setNodesBeforeSlicing(sdg.getAllNodes().size());

        sdg.getReturnNodes().clear();
        sdg.setParameterNode(null);

        // Slice SDG
        Statistics.getTimer(Statistics.TIMER_Slicing_SDG).start();
        DependenceGraph sdgSliced = sliceSDG(sdg);
        Statistics.getTimer(Statistics.TIMER_Slicing_SDG).stop();

        // Slice CFG according to SDG
        Statistics.getTimer(Statistics.TIMER_Slicing_CFG).start();
        sliceCFG(sdgSliced, sdg);
        Statistics.getTimer(Statistics.TIMER_Slicing_CFG).stop();

        // Write output
        writeOutput();

        // Handle output
        switch (Config.getInstance().getOutputFormat()) {
            case "APK":
                signAndMoveToOutput();
                break;
            case "CLASS":
                convertToClass();
                break;
            case "JIMPLE":
                moveJimpleOutput();
                break;
        }

        // Export Jimple of android app
//        try {
//            FileOutputStream out = new FileOutputStream(apkName + "_Method_jimple.txt",true);
//            for (SootMethod sm : pdgMap.keySet()){
//                String content = sm.getDeclaringClass().getName() + ": " + sm.getSubSignature() + "\n";
//                byte[] bytes = content.getBytes();
//                out.write(bytes);
//            }
//        }catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        // Draw induced SDG
        // Set the induced subgraph of SDG as the graph to be drawn
//        Set<SootMethod> visibleMethodList = new HashSet<>();
//        for (SootClass sc : Scene.v().getApplicationClasses()){
//            if (sc.getShortName().equals("arXivDB")){
//                visibleMethodList.add(sc.getMethod("boolean insertFeed(java.lang.String,java.lang.String,java.lang.String,int,int)"));
//            }
//            if (sc.getShortName().equals("RSSListWindow")){
//                visibleMethodList.add(sc.getMethod("void favoritePressed(android.view.View)"));
//            }
//        }
//        for (SootMethod dummyMainMethod : this.lifeCycleHandler.getDummyMainClass().getMethods()){
//            if (dummyMainMethod.getSubSignature().equals("void dummyMainMethod(java.lang.String[])")){
//                visibleMethodList.add(dummyMainMethod);
//            }
//            String method = dummyMainMethod.getSubSignature().split(" ")[0];
//            if (method.endsWith(".arXiv") || method.endsWith(".RSSListWindow")){
//                visibleMethodList.add(dummyMainMethod);
//            }
//        }
//        final DependenceGraph induced_sdg = getInducedSDG(sdg, visibleMethodList);
//
//        if (isDrawGraph) {
//            final GraphDrawer graphDrawer = new GraphDrawer("SDG", induced_sdg);
////            graphDrawer.setHighlightIn(sliceItself);
////            graphDrawer.setHighlightOut1(sliceIgnoringFields);
////            graphDrawer.setHighlightOut2(contextSensitiveRefined);
//            graphDrawer.drawGraph("myOutput/FlowSlicer.SplitApkByProgramSlice/" + apkName, 0);
//        }
    }


    private void signAndMoveToOutput() {
        // Move file
//        final File outputApkFile = new File(apkDir, apkName + "_sliced");
//        try {
//            final File sootOutputApkFile = new File(apkDir, apkName);
//            Files.move(sootOutputApkFile.toPath(), outputApkFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
//            System.out.println("Output .apk file written: " + outputApkFile.getAbsolutePath());
//        } catch (final IOException e) {
//            System.out.println("Could not write output .apk file.");
//        }

        // Signing
//        if (isSign) {
//            final APKSigner signer = new APKSigner(new File(FlowSlicer.Config.getInstance().zipalignPath),
//                    new File(FlowSlicer.Config.getInstance().apksignerPath));
//            if (signer.sign(outputApkFile)) {
//                System.out.println(outputApkFile.getAbsolutePath() + " signed successfully!");
//            } else {
//                System.out.println("Failed signing: " + outputApkFile.getAbsolutePath());
//            }
//        }
    }

    private void convertToClass() {
//        Log.msg("Converting to .class output", Log.NORMAL);
//        if (Log.getLogLevel() >= Log.DEBUG) {
//            Log.setSilence(false);
//        } else {
//            Log.setSilence(Log.SILENCE_LEVEL_MSG);
//        }
//
//        G.reset();
//
//        Options.v().set_output_format(Options.output_format_class);
//        Options.v().set_src_prec(Options.src_prec_jimple);
//
//        final File outputClassFile = getOutputFile();
//        if (outputClassFile.getParentFile() != null) {
//            Options.v().set_output_dir(outputClassFile.getAbsolutePath());
//        } else {
//            Options.v().set_output_dir(".");
//        }
//
//        Options.v().set_soot_classpath(getClasspath());
//        Options.v().set_process_dir(Collections.singletonList(Parameters.TEMP_OUTPUT_DIR.getAbsolutePath()));
//
//        soot.Main.main(new String[] { Parameters.getInstance().getInputApkFile().getName().substring(0,
//                Parameters.getInstance().getInputApkFile().getName().lastIndexOf(".class")) });
//
//        Log.setSilence(false);
//        Log.msg("Output .class file written: " + outputClassFile.getAbsolutePath(), Log.IMPORTANT);
    }

    private void moveJimpleOutput() {
//        Log.msg("Moving output", Log.NORMAL);
//        try {
//            Files.move(Parameters.TEMP_OUTPUT_DIR.toPath(), getOutputDir().toPath(),
//                    StandardCopyOption.REPLACE_EXISTING);
//            Log.msg("Output .jimple files written to: " + getOutputDir().getAbsolutePath(), Log.IMPORTANT);
//        } catch (final IOException e) {
//            Log.error("Could not write output .jimple files." + Log.getExceptionAppendix(e));
//        }
    }
}
