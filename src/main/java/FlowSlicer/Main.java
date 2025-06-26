package FlowSlicer;
import FlowSlicer.Analyzer.ManifestAnalyzer;
import FlowSlicer.Mode.*;
import FlowSlicer.RefactorTool.SootAnalyzer;
import FlowSlicer.RefactorTool.SplitAPK;
import FlowSlicer.XMLObject.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.cli.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.xmlpull.v1.XmlPullParserException;
import soot.MethodSubSignature;
import soot.PackManager;
import soot.jimple.ThisRef;
import soot.jimple.toolkits.callgraph.VirtualEdgesSummaries;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class Main {
    public static void main(String[] args) throws XmlPullParserException, URISyntaxException, IOException {
        // Analyze args
        CommandLine cmd = getCommandLine(args);
        if (cmd == null) {
            log.error("Cannot get command line input!");
            return;
        }
        analyzeArgs(cmd);

        // start Splitting input APP
        startAnalyze();

        log.info("Android APP Split Finish...\n");
        System.exit(0);
    }

    /**
     * Get command line
     *
     * @param cmdArgs Command line arguments
     * @return CommandLine
     */
    private static CommandLine getCommandLine(String[] cmdArgs) {
        CommandLineParser parser = new DefaultParser();
        try {
            return parser.parse(getOptions(), cmdArgs, false);
        } catch (ParseException e) {
            log.error("Parsing command line failed. Reason: " + e.getMessage());
            return null;
        }
    }

    /**
     * construct the structure of options
     *
     * @return
     */
    private static Options getOptions() {
        Options options = new Options();

        /** basic options **/
        options.addOption("h", "help", false, "-h: Show the help information.");
        options.addOption("c", "config", true, "-c [default: config/config.json]: Path to config.json ");
        options.addOption("n", "name", true, "-n : Set the name of the apk under analysis.");
        options.addOption("p", "path", true, "-p [default: apk/apkName.apk]: Set the relative path of the apk under analysis.");
        options.addOption("j", "androidJar", true, "-j [default: lib/platforms]: Set the path of android jar platform.");
        options.addOption("m", "mode", true, "-m "
                        + "FlowSliceMode: Slice apk according to input slice criteria (Flowdroid analysis results are mandatory).\n"
                        + "MatchSliceMode: Slice apk according to matches in the default SourcesAndSinks lists.\n"
                        + "FlowDroidMode: Run flowdroid only.\n"
        );
        options.addOption("fr", "flowDroidResult", true, "-fr [default: flowdroid/apkname/result.xml]: Set the path of flowdroid analysis result (needed only in FlowSliceMode).");
        options.addOption("fo","flowDroidOutput", true, "-fo [default: flowdroid/apkname/output.xml]: Set the path of flowdroid analysis output (needed only in FlowSliceMode).");
        options.addOption("fp","flowDroidProduct", true, "-fp [default: flowdroid/apkname/product.xml]: Set the path of flowdroid analysis product (needed only in MatchSliceMode).");
        options.addOption("sd", "stubDroidDir", true, "-sd [default: StubDroid/StubDroidSummaries]: Set the path of StubDroid Summaries Dir.");
        options.addOption("sdm", "stubDroidManualDir", true, "-sdm [default: StubDroid/StubDroidSummaries/manual]: Set the path of StubDroid Summaries Manual Dir.");
        options.addOption("ve", "virtualEdgesFile", true, "-ve [default: config/virtualedges.xml]: Set the path of Virtual Edges file.");

        /** analysis config **/
        options.addOption("pc", "permissionConsidered", false, "-pc: Set that permissions are not considered when pre-matching source-sink invocations.");
        options.addOption("r", "random", false, "-r: Set that the slicing criteria are selected randomly from one source and one sink invocation.");
        options.addOption("t", "time", true, "-t [default: 60]: Set the max running time (min).");
        options.addOption("cg", "callGraphAlgorithm", true, "-cg [default: CHA]: Set algorithm for CG, CHA or SPARK.");
        options.addOption("ICC", true, "-ICC [default: ICCResult/]: Set the file path of ICC analysis results(this tool supports ICCBot only).");
        options.addOption("cm", "dependencyGraphConstructionMode", true, "-cm [default: GlobalConstructionMode] "
                + "PartialConstructionMode: Construct the System Dependency Graph partially.\n"
                + "GlobalConstructionMode: Construct the System Dependency Graph globally.\n"
        );

        /** output **/
        options.addOption("o", "output", true, "-o [default: outputDir]: Set the output folder of sliced apk.");
        options.addOption("of", "outputFormat", true, "-of [default: APK]: Set the output format of sliced apk (APK or JIMPLE or CLASS).");
        options.addOption("d", "draw", false, "-d : draw the dependency graph of sliced app.");
        return options;
    }

    /**
     * analyze args and store information to MyConfig
     *
     * @param cmd
     */
    private static void analyzeArgs(CommandLine cmd) {
        if (null == cmd){
            log.error("The read command line is null!");
            System.exit(-1);
        }

        if (cmd.hasOption("h")) {
            printHelp(getOptions());
            System.exit(0);
        }

        /** run config **/
        Config config = Config.getInstance();

        config.setJimple(true);
        String appName = cmd.getOptionValue("n", "Merge1");
//        String appName = cmd.getOptionValue("n", "com.bagtuahaga.armennsbb-0090110FF907D05FBA448A4769CBB4BDC8F9DE881F786DB69CE3EE324C267D9D");
        if (appName.endsWith(".apk")) {
            appName = appName.substring(0, appName.length() - 4);
        }
        config.setAppName(appName);
        String appPath = cmd.getOptionValue("p", "apk" + File.separator + appName + ".apk");
        if (!appPath.startsWith("apk")) {
            appPath = "apk" + File.separator + appPath;
        }
        config.setAppPath(appPath);
        config.setAndroidJar(cmd.getOptionValue("j", "lib" + File.separator + "platforms") + File.separator);

        String mode = cmd.getOptionValue("m", Config.MATCH_SLICE_MODE);
        config.setMode(cmd.getOptionValue("m", mode));

        String flowDroidResultFile = cmd.getOptionValue("fr", "flowdroid" + File.separator + Config.getInstance().getAppName() + File.separator + "result.xml");
        config.setFlowDroidResultPath(flowDroidResultFile);
        String flowDroidOutputFile = cmd.getOptionValue("fo", "flowdroid" + File.separator + Config.getInstance().getAppName() + File.separator + "output.xml");
        config.setFlowDroidOutputPath(flowDroidOutputFile);
        String flowDroidProductFile = cmd.getOptionValue("fp", "flowdroid" + File.separator + Config.getInstance().getAppName() + File.separator + "product.xml");
        config.setFlowDroidProductPath(flowDroidProductFile);
        String stubDroidDir = cmd.getOptionValue("sd", "StubDroid" + File.separator + "StubDroidSummaries");
        config.setStubDroidDir(stubDroidDir);
        String stubDroidManualDir = cmd.getOptionValue("sd", "StubDroid" + File.separator + "StubDroidSummaries" + File.separator + "manual");
        config.setStubDroidManualDir(stubDroidManualDir);
        String virtualEdgesFile = cmd.getOptionValue("ve", "config" + File.separator + "virtualedges.xml");
        config.setVirtualEdgesPath(virtualEdgesFile);

        String constructionMode = cmd.getOptionValue("cm", Config.PARTIAL_SDG_CONSTRUCTION_MODE);
        if (constructionMode.equals(Config.GLOBAL_SDG_CONSTRUCTION_MODE)) {
            config.setGlobalSdgConstruction(true);
        } else if (constructionMode.equals(Config.PARTIAL_SDG_CONSTRUCTION_MODE)) {
            config.setPartialSDGConstruction(true);
        } else {
            log.error("Wrong input SDG construction mode!");
            System.exit(-1);
        }

        String outputDir = cmd.getOptionValue("o", "outputDir" + File.separator + mode + File.separator);
        config.setResultFolder(outputDir);
        String outputFormat = cmd.getOptionValue("of", Config.OUTPUT_FORMAT_APK);
        config.setOutputFormat(outputFormat);

        // Create path directories
        try {
            Path flowDroidResultPath = Paths.get(flowDroidResultFile).getParent();
            Path flowDroidOutputPath = Paths.get(flowDroidOutputFile).getParent();
            Path flowDroidProductPath = Paths.get(flowDroidProductFile).getParent();
            Path outputPath = Paths.get(outputDir).getParent();
            if (Files.notExists(flowDroidResultPath)) {
                Files.createDirectories(flowDroidResultPath);
            }
            if (Files.notExists(flowDroidOutputPath)) {
                Files.createDirectories(flowDroidOutputPath);
            }
            if (Files.notExists(flowDroidProductPath)) {
                Files.createDirectories(flowDroidProductPath);
            }
            if (Files.notExists(outputPath)) {
                Files.createDirectories(outputPath);
            }
        } catch (IOException e) {
            log.error("An error occurred: " + e.getMessage());
        }

        if (cmd.hasOption("pc"))
            config.setIsPermissionConsidered(false);
        if (cmd.hasOption("r"))
            config.setIsRandomCriteria(true);
        int timeLimit = Integer.parseInt(cmd.getOptionValue("time", "90"));
        config.setTimeLimit(timeLimit);
        config.setMaxPathNumber(Integer.parseInt(cmd.getOptionValue("maxPathNumber", "100")));
        config.setMaxFunctionExpandNumber(Integer.parseInt(cmd.getOptionValue("maxFunctionExpandNumber", "10")));
        config.setMaxObjectSummarySize(Integer.parseInt(cmd.getOptionValue("maxObjectSummarySize", "1000")));
        config.setCallGraphAlgorithm(cmd.getOptionValue("cg", "CHA"));

        String ICCFileDir = cmd.getOptionValue("ICC", "ICCResult" + File.separator);
        config.setICCFilePath(ICCFileDir);

        if (cmd.hasOption("d"))
            config.setDrawGraph(true);

        if (!cmd.hasOption("n")) {
            log.error("Please input the apk name use -n.");
            printHelp(getOptions());
        }

        // Load Analyze FlowSlicer.Config
        String analyzeConfigPath = "config" + File.separator + "config.json";
        if (cmd.hasOption("config")) {
            analyzeConfigPath = cmd.getOptionValue("config");
        }
        Path configFilePath = Paths.get(analyzeConfigPath);
        if (!Files.exists(configFilePath)) {
            log.error("Failed to load analyze config json: File not exist");
            System.exit(0);
        }

        JSONObject analyzeConfig = null;
        try {
            analyzeConfig = JSON.parseObject(String.join("\n", Files.readAllLines(configFilePath)));
        } catch (IOException e) {
            log.error("Failed to load analyze config json: IOException", e);
            System.exit(0);
        }

        List<String> appliedConfigList = new ArrayList<>();
        // Initialize SootHelper.defaultExcludes
        JSONArray excludeArr = analyzeConfig.getJSONArray("SootHelper.defaultExcludes");
        if (excludeArr == null) {
            log.error("Cannot read default excludes from config file.");
            return;
        }
        List<String> excludeList = excludeArr.toJavaList(String.class);
        Config.getInstance().setDefaultExcludes(excludeList);
        appliedConfigList.add("SootHelper.defaultExcludes");

        // Initialize Callback list
        JSONArray callbacksArr = analyzeConfig.getJSONArray("AppModel.AndroidCallbacks");
        if (callbacksArr == null) {
            log.error("Cannot read callbacks list from config file.");
            return;
        }
        List<String> callbacksList = callbacksArr.toJavaList(String.class);
        Config.getInstance().setCallbacksList(callbacksList);
        appliedConfigList.add("AppModel.AndroidCallbacks");

        // Initialize Sources and Sinks API list under MatchSliceMode
        JSONArray sourcesArr = analyzeConfig.getJSONArray("AppModel.SourceAPIs");
        if (sourcesArr == null) {
            log.error("Cannot read sources list from config file.");
            return;
        }
        List<String> sourcesList = sourcesArr.toJavaList(String.class);
        Config.getInstance().setSourcesAPIList(sourcesList);
        appliedConfigList.add("AppModel.SourceAPIs");

        JSONArray sinksArr = analyzeConfig.getJSONArray("AppModel.SinkAPIs");
        if (sinksArr == null) {
            log.error("Cannot read sources list from config file.");
            return;
        }
        List<String> sinksList = sinksArr.toJavaList(String.class);
        Config.getInstance().setSinksAPIList(sinksList);
        appliedConfigList.add("AppModel.SinkAPIs");

//            JSONArray bothArr = analyzeConfig.getJSONArray("AppModel.BothSourceAndSinkAPIs");
//            if (bothArr == null) {
//                log.error("Cannot read both sources and sinks list from config file.");
//                return;
//            }
//            List<String> bothList = bothArr.toJavaList(String.class);
//            Config.getInstance().setBothAPIList(bothList);
//            appliedConfigList.add("AppModel.BothSourceAndSinkAPIs");

        log.info("Applied analyze config list: " + appliedConfigList);

        // Load virtualEdges file
        VirtualEdgesSummaries virtualEdgesSummary = new VirtualEdgesSummaries();
        for (VirtualEdgesSummaries.VirtualEdge edge : virtualEdgesSummary.getAllVirtualEdges()) {
            if (edge.getSource() instanceof VirtualEdgesSummaries.InstanceinvokeSource) {
                MethodSubSignature sourceMethod = ((VirtualEdgesSummaries.InstanceinvokeSource) edge.getSource()).getSubSignature();
                for (VirtualEdgesSummaries.VirtualEdgeTarget target : edge.getTargets()) {
                    MethodSubSignature targetMethod = target.getTargetMethod();
                    Global.v().getAppModel().getVirtualEdgesMultiMap().put(sourceMethod.getNumberedSubSig().getString(), targetMethod.getNumberedSubSig().getString());
                }
            } else if (edge.getSource() instanceof VirtualEdgesSummaries.StaticinvokeSource) {
                String sourceMethod = ((VirtualEdgesSummaries.StaticinvokeSource) edge.getSource()).getSignature();
                for (VirtualEdgesSummaries.VirtualEdgeTarget target : edge.getTargets()) {
                    MethodSubSignature targetMethod = target.getTargetMethod();
                    Global.v().getAppModel().getVirtualEdgesMultiMap().put(sourceMethod, targetMethod.getNumberedSubSig().getString());
                }
            }
        }
//        for (VirtualEdgesSummaries.VirtualEdge edge : virtualEdgesSummary.getAllVirtualEdges()) {
//            if (edge.getSource() instanceof VirtualEdgesSummaries.InstanceinvokeSource) {
//                MethodSubSignature sourceMethod = ((VirtualEdgesSummaries.InstanceinvokeSource) edge.getSource()).getSubSignature();
//                for (VirtualEdgesSummaries.VirtualEdgeTarget target : edge.getTargets()) {
//                    if (target instanceof VirtualEdgesSummaries.InvocationVirtualEdgeTarget) {
//                        MethodSubSignature targetMethod = ((VirtualEdgesSummaries.InvocationVirtualEdgeTarget)target).getTargetMethod();
//                        Global.v().getAppModel().getVirtualEdgesMultiMap().put(sourceMethod.getNumberedSubSig().getString(), targetMethod.getNumberedSubSig().getString());
//                    }
//                }
//            } else if (edge.getSource() instanceof VirtualEdgesSummaries.StaticinvokeSource) {
//                String sourceMethod = ((VirtualEdgesSummaries.StaticinvokeSource) edge.getSource()).getSignature();
//                for (VirtualEdgesSummaries.VirtualEdgeTarget target : edge.getTargets()) {
//                    if (target instanceof VirtualEdgesSummaries.InvocationVirtualEdgeTarget) {
//                        MethodSubSignature targetMethod = ((VirtualEdgesSummaries.InvocationVirtualEdgeTarget)target).getTargetMethod();
//                        Global.v().getAppModel().getVirtualEdgesMultiMap().put(sourceMethod, targetMethod.getNumberedSubSig().getString());
//                    }
//                }
//            }
//        }
    }

    /**
     * start the analyze of app
     */
    public static void startAnalyze() throws XmlPullParserException, URISyntaxException, IOException
    {
        if (Config.getInstance().getMode().equals(Config.FLOW_SLICE_MODE)) {
            startFlowSliceModeAnalysis();
        } else if (Config.getInstance().getMode().equals(Config.MATCH_SLICE_MODE)) {
            startMatchSliceModeAnalysis();
        } else if (Config.getInstance().getMode().equals(Config.FLOW_DROID_MODE)) {
            startFlowDroidModeAnalysis();
        } else if (Config.getInstance().getMode().equals(Config.JIMPLE_MODE)) {
            startJimpleModeAnalysis();
        } else {
            log.error("error input mode!");
            System.exit(-1);
        }

        log.info("---------------------------------------");
        log.info("Analyzing " + Config.getInstance().getAppName() + " Finish...\n");

        if (Config.getInstance().getMode().equals(Config.MATCH_SLICE_MODE) && Config.getInstance().isSlicingCriteriaFound()) {
            // Report Part
            final StringBuilder sb = new StringBuilder("Slicing result for \"" + Config.getInstance().getAppName() + "\":\n");
            for (String str : Statistics.getInstance().getTimers().keySet()) {
                sb.append(str + Statistics.getTimer(str).getTimeAsString() + "\n");
            }
            sb.append(Statistics.getInstance().getFlowDroidTimer1().getTitle() + Statistics.getInstance().getFlowDroidTimer1().getTimeAsString() + "\n");
            sb.append(Statistics.getInstance().getFlowSlicerTimer().getTitle() + Statistics.getInstance().getFlowSlicerTimer().getTimeAsString() + "\n");
            sb.append(Statistics.getInstance().getFlowDroidTimer2().getTitle() + Statistics.getInstance().getFlowDroidTimer2().getTimeAsString() + "\n");

            // Report Slicing performance
            DecimalFormat df = new DecimalFormat("0.00");
            int num_nodes_before_slice = Global.v().getAppModel().getNodesBeforeSlicing();
            int num_nodes_after_slice = Global.v().getAppModel().getNodesAfterSlicing();
            double percentage = Double.valueOf(df.format(((double)(num_nodes_before_slice - num_nodes_after_slice)/(double)num_nodes_before_slice)* 100));
            sb.append("After slicing, our tool slices ").append(num_nodes_before_slice - num_nodes_after_slice).append(" nodes in the SDG.\nSlicing percentage: ").append(percentage).append("%(").append(num_nodes_before_slice - num_nodes_after_slice).append("/").append(num_nodes_before_slice).append(")\n");
            log.info(sb.toString());

            String reportPath = "AppReport.txt";
            File reportFile = new File(reportPath);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportFile, true))) {
                writer.write(sb.toString());
            }

            // write information into .txt file
            List<String> appData = getAppData();
            updateTxtFile(appData);
        } else if (Config.getInstance().getMode().equals(Config.FLOW_SLICE_MODE) && Config.getInstance().isSlicingCriteriaFound()) {
            // Report Part
            final StringBuilder sb = new StringBuilder("Slicing result for \"" + Config.getInstance().getAppName() + "\":\n");
            sb.append(Statistics.getInstance().getFlowDroidTimer1().getTitle() + Statistics.getInstance().getFlowDroidTimer1().getTimeAsString() + "\n");
            sb.append(Statistics.getInstance().getFlowSlicerTimer().getTitle() + Statistics.getInstance().getFlowSlicerTimer().getTimeAsString() + "\n");
            sb.append(Statistics.getInstance().getFlowDroidTimer2().getTitle() + Statistics.getInstance().getFlowDroidTimer2().getTimeAsString() + "\n");
            DecimalFormat df = new DecimalFormat("0.00");
            int num_nodes_before_slice = Global.v().getAppModel().getNodesBeforeSlicing();
            int num_nodes_after_slice = Global.v().getAppModel().getNodesAfterSlicing();
            double percentage = Double.valueOf(df.format(((double)(num_nodes_before_slice - num_nodes_after_slice)/(double)num_nodes_before_slice)* 100));
            sb.append("After slicing, our tool slices ").append(num_nodes_before_slice - num_nodes_after_slice).append(" nodes in the SDG.\nSlicing percentage: ").append(percentage).append("%(").append(num_nodes_before_slice - num_nodes_after_slice).append("/").append(num_nodes_before_slice).append(")\n");
            log.info(sb.toString());
            String reportPath = "AppReport.txt";
            File reportFile = new File(reportPath);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportFile, true))) {
                writer.write(sb.toString());
            }

            List<String> appData = new ArrayList<>();
            appData.add(Config.getInstance().getAppName());
            appData.add(String.valueOf(Global.v().getAppModel().getNodesBeforeSlicing()));
            appData.add(String.valueOf(Global.v().getAppModel().getNodesAfterSlicing()));
            appData.add(Statistics.getInstance().getFlowDroidTimer1().getTimeAsString());
            appData.add(Statistics.getInstance().getFlowSlicerTimer().getTimeAsString());
            appData.add(Statistics.getInstance().getFlowDroidTimer2().getTimeAsString());
            XMLHelper.parseFlowDroidXmlFile(Config.getInstance().getFlowDroidResultPath(), Global.v().getFlowDroidResult1());
            appData.add(String.valueOf(Global.v().getFlowDroidResult1().getLeakNum()));
            XMLHelper.parseFlowDroidXmlFile(Config.getInstance().getFlowDroidProductPath(), Global.v().getFlowDroidResult2());
            appData.add(String.valueOf(Global.v().getFlowDroidResult2().getLeakNum()));
            updateTxtFile(appData);
        } else if (Config.getInstance().getMode().equals(Config.FLOW_DROID_MODE)) {
            String reportPath = "FlowDroidReport.txt";
            File reportFile = new File(reportPath);
            final StringBuilder sb = new StringBuilder("FlowDroid result for \"" + Config.getInstance().getAppName() + "\":\n");
            sb.append("whether to timeout: " + Config.getInstance().isOriginalFlowDroidTimeOut() + "\n");
            if (Config.getInstance().isOriginalFlowDroidTimeOut()) {
                sb.append("leaks found: 0 " + "\n\n");
            } else {
                sb.append(Statistics.getInstance().getFlowDroidTimer1().getTimeAsString() + "\n");
                XMLHelper.parseFlowDroidXmlFile(Config.getInstance().getFlowDroidResultPath(), Global.v().getFlowDroidResult1());
                sb.append("leaks found: " + Global.v().getFlowDroidResult1().getLeakNum() + "\n\n");
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportFile, true))) {
                writer.write(sb.toString());
            }
        }
    }

    public static void startFlowSliceModeAnalysis() throws XmlPullParserException, URISyntaxException, IOException
    {
        log.info("Using Mode " + Config.FLOW_SLICE_MODE);

        log.info("Analyzing " + Config.getInstance().getAppName());

        // run FlowDroid before slicing
        startOriginalFlowDroidAnalysis();

        // run FlowSlicer
        Statistics.getInstance().getFlowSlicerTimer().start();
        SootAnalyzer sootAnalyzer = new SootAnalyzer();
        sootAnalyzer.sootInit();
        ManifestAnalyzer manifestAnalyzer = new ManifestAnalyzer();
        manifestAnalyzer.analyze();
        SplitAPK splitter = new SplitApkInExclusionMode();
        splitter.splitApk();
        Statistics.getInstance().getFlowSlicerTimer().stop();

        // run FlowDroid after slicing
        startFinalFlowDroidAnalysis();
    }

    public static void startMatchSliceModeAnalysis() throws XmlPullParserException, URISyntaxException, IOException
    {
        log.info("Using Mode " + Config.MATCH_SLICE_MODE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            try {
                Statistics.getInstance().getFlowSlicerTimer().start();
                SootAnalyzer sootAnalyzer = new SootAnalyzer();
                sootAnalyzer.sootInit();
                ManifestAnalyzer manifestAnalyzer = new ManifestAnalyzer();
                manifestAnalyzer.analyze();
                SplitAPK splitter = new SplitApkInExclusionMode();
                splitter.splitApk();
                Statistics.getInstance().getFlowSlicerTimer().stop();

                Statistics.getInstance().getFlowDroidTimer2().start();
                FlowdroidHelper.runInfoflow(Config.getInstance().getResultFolder() + Config.getInstance().getAppName() + File.separator + "apk" + File.separator + Config.getInstance().getAppName() + ".apk", Config.getInstance().getFlowDroidProductPath());
            } catch (IOException | URISyntaxException | XmlPullParserException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            future.get(Config.getInstance().getTotal_timeout_sec(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            Config.getInstance().setIsTotalTimeOut(true);
            log.error("Total Task timed out");
        } catch (ExecutionException e) {
            log.error("Task failed with exception: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }

        Statistics.getInstance().getFlowDroidTimer2().stop();

        // run FlowDroid after slicing
//        startFinalFlowDroidAnalysis();
    }

    public static void startFlowDroidModeAnalysis() throws XmlPullParserException, URISyntaxException, IOException
    {
        log.info("Using Mode " + Config.FLOW_DROID_MODE);

        // run FlowSlicer
        startOriginalFlowDroidAnalysis();
    }

    public static void startOriginalFlowDroidAnalysis() throws XmlPullParserException, URISyntaxException, IOException
    {
        log.info("Using Mode " + Config.FLOW_DROID_MODE);

        // run FlowSlicer
        Statistics.getInstance().getFlowDroidTimer1().start();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            try {
                FlowdroidHelper.runInfoflow(Config.getInstance().getAppPath(), Config.getInstance().getFlowDroidResultPath());
            } catch (URISyntaxException | XmlPullParserException | IOException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            future.get(Config.getInstance().getOriginal_flowdroid_timeout_sec(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            Config.getInstance().setIsOriginalFlowDroidTimeOut(true);
            log.error("Original_flowdroid Task timed out");
        } catch (ExecutionException e) {
            log.error("Task failed with exception: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }

        Statistics.getInstance().getFlowDroidTimer1().stop();
    }

    public static void startFinalFlowDroidAnalysis() throws XmlPullParserException, URISyntaxException, IOException
    {
        log.info("Using Mode " + Config.FLOW_DROID_MODE);

        // run FlowSlicer
        Statistics.getInstance().getFlowDroidTimer2().start();
        FlowdroidHelper.runInfoflow(Config.getInstance().getResultFolder() + Config.getInstance().getAppName() + File.separator + "apk" + File.separator + Config.getInstance().getAppName() + ".apk", Config.getInstance().getFlowDroidProductPath());
        Statistics.getInstance().getFlowDroidTimer2().stop();
    }

    public static void startJimpleModeAnalysis() {
        log.info("Using Mode " + Config.JIMPLE_MODE);

        SootAnalyzer sootAnalyzer = new SootAnalyzer();
        sootAnalyzer.sootInit();
        PackManager.v().runPacks();
        sootAnalyzer.sootWriteOutput();
        System.exit(0);
    }

    @NotNull
    private static List<String> getAppData() {
        List<String> appData = new ArrayList<>();
        appData.add(Config.getInstance().getAppName());
//        appData.add(Config.getInstance().getAppPath());
        appData.add(String.valueOf(Global.v().getAppModel().getNodesBeforeSlicing()));
        appData.add(String.valueOf(Global.v().getAppModel().getNodesAfterSlicing()));
        appData.add(Statistics.getInstance().getFlowSlicerTimer().getTimeAsString());
//        if (Config.getInstance().getMode().equals(Config.FLOW_SLICE_MODE)) {
//            appData.add(Statistics.getInstance().getFlowDroidTimer1().getTimeAsString());
//        } else {
//            appData.add(String.valueOf(0));
//        }
        appData.add(Statistics.getInstance().getFlowDroidTimer2().getTimeAsString());

//        if (Config.getInstance().getMode().equals(Config.FLOW_SLICE_MODE)) {
//            appData.add(String.valueOf(Global.v().getFlowDroidResult1().getSinkToSourcesMap().keySet().size()));//以sink的数量代表leak的数量
//        } else {
//            appData.add(String.valueOf(0));
//        }

        if (Config.getInstance().getMode().equals(Config.FLOW_SLICE_MODE)) {
            XMLHelper.parseFlowDroidXmlFile(Config.getInstance().getFlowDroidOutputPath(), Global.v().getFlowDroidResult2());
        } else {
            XMLHelper.parseFlowDroidXmlFile(Config.getInstance().getFlowDroidProductPath(), Global.v().getFlowDroidResult2());
        }
        appData.add(String.valueOf(Global.v().getFlowDroidResult2().getLeakNum()));

        appData.add(String.valueOf(Config.getInstance().isBackwardFinish()));
        appData.add(String.valueOf(Config.getInstance().isBackwardAndForwardFinish()));
        appData.add(String.valueOf(Config.getInstance().isExtraFinish()));
        appData.add(String.valueOf(Config.getInstance().isSlicerTimeOut()));
        appData.add(String.valueOf(Config.getInstance().isTotalTimeOut()));
        return appData;
    }

    private static void updateTxtFile(List<String> appData) throws IOException {
        String txtFilePath = "ApplicationData_" + Config.getInstance().getMode() + "_" + Config.getInstance().getAppName() + ".txt";
        File txtFile = new File(txtFilePath);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(txtFile, true))) {
            if (!txtFile.exists()) {
                writer.write("AppName, Statements Before Slicing, Statements After Slicing," +
                        "Slice Time(s), FlowDroid Time After Slicing(s), leaks after slicing, " +
                        "whether is Backward finished, whether is Backward and Forward finished, whether is Extra finished, " +
                        "whether is Slicer timeout, whether is final flowdroid timeout\n");
            }

            StringBuilder dataLine = new StringBuilder();
            for (String data : appData) {
                dataLine.append(data).append(", ");
            }
            dataLine.setLength(dataLine.length() - 2);
            dataLine.append("\n");

            writer.write(dataLine.toString());
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(120);

        formatter.printHelp("java -jar [jarFile] [options] [-n] [-m]\n" +
                "E.g., -n Loop1 -m MatchSliceMode", options);
    }
}
