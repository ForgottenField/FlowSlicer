package FlowSlicer;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import soot.Body;

import java.util.List;

@Getter
public class Config {
    public static final String FLOW_SLICE_MODE = "FlowSliceMode";
    public static final String MATCH_SLICE_MODE = "MatchSliceMode";
    public static final String FLOW_DROID_MODE = "FlowDroidMode";
    public static final String JIMPLE_MODE = "JimpleMode";
    public static final String PARTIAL_SDG_CONSTRUCTION_MODE = "PartialConstructionMode";
    public static final String GLOBAL_SDG_CONSTRUCTION_MODE = "GlobalConstructionMode";
    public static final String OUTPUT_FORMAT_APK = "APK";
    public static final String OUTPUT_FORMAT_JIMPLE = "JIMPLE";
    private Config() {
    }
    private boolean isJimple = true;
    private boolean isDrawGraph = false;
    private boolean isPartialSDGConstruction = false;
    private boolean isGlobalSDGConstruction = false;
    private boolean isPermissionConsidered = true;
    private boolean isRandomCriteria = false;
    private String androidVersion;
    private String resultFolder;
    private String outputFormat;
    private String appName;
    private String appPath;
    private String mode;
    private String callGraphAlgorithm;
    private List<String> defaultExcludes;
    private List<String> callbacksList;
    private List<String> sourcesAPIList;
    private List<String> sinksAPIList;
    private List<String> bothAPIList;
    private String ICCFilePath;
    private String flowDroidResultPath;
    private String flowDroidOutputPath;
    private String flowDroidProductPath;
    private String stubDroidDir;
    private String stubDroidManualDir;
    private String virtualEdgesPath;
    private int timeLimit;
    private int maxPathNumber;
    private int maxFunctionExpandNumber;
    private int maxObjectSummarySize;
    private String androidJar;
    private boolean stopFlag = false;
    private long original_flowdroid_timeout_min = 120;
    private long original_flowdroid_timeout_sec = original_flowdroid_timeout_min * 60;
    private long slicer_timeout_min = 60;
    private long slicer_timeout_sec = slicer_timeout_min * 60;
//    private long final_flowdroid_timeout_min = 60;
//    private long final_flowdroid_timeout_sec = final_flowdroid_timeout_min * 60;
    private long backward_forward_timeout_min = 30;
    private long backward_forward_timeout_sec = backward_forward_timeout_min * 60;
    private long total_timeout_min = 120;
    private long total_timeout_sec = total_timeout_min * 60;
    private boolean isSlicerTimeout = false;
    private boolean isOriginalFlowDroidTimeout = false;
    private boolean isFinalFlowDroidTimeout = false;
    private boolean isBackwardFinish = false;
    private boolean isBackwardAndForwardFinish = false;
    private boolean isExtraFinish = false;
    private boolean isTotalTimeout = false;

    private boolean isSootAnalyzeFinish;
    private boolean isManifestClientFinish;
    private boolean isFragmentClientFinish;
    private boolean isCallGraphClientFinish;
    private boolean isStaticValueAnalyzeFinish;
    private boolean isOracleConstructionClientFinish;
    private boolean isSlicingCriteriaFound = false;

    private static class SingletonInstance {
        private static final Config instance = new Config();
    }

    public static Config getInstance() {
        return SingletonInstance.instance;
    }

    public boolean isJimple() {
        return isJimple;
    }

    public void setJimple(boolean isJimple) {
        this.isJimple = isJimple;
    }

    public boolean isDrawGraph() {
        return isDrawGraph;
    }

    public void setDrawGraph(boolean isDrawGraph){
        this.isDrawGraph = isDrawGraph;
    }

    public boolean isPartialSDGConstruction() {
        return isPartialSDGConstruction;
    }
    public boolean isPermissionConsidered() {
        return isPermissionConsidered;
    }

    public void setIsPermissionConsidered(boolean isPermissionConsidered) {
        this.isPermissionConsidered = isPermissionConsidered;
    }

    public boolean isRandomCriteria() {
        return isRandomCriteria;
    }

    public void setIsRandomCriteria(boolean isRandomCriteria) {
        this.isRandomCriteria = isRandomCriteria;
    }

    public void setPartialSDGConstruction(boolean isPartialSDGConstruction){
        this.isPartialSDGConstruction = isPartialSDGConstruction;
    }

    public boolean isGlobalSDGConstruction() {
        return isGlobalSDGConstruction;
    }

    public void setGlobalSdgConstruction(boolean isGlobalSDGConstruction){
        this.isGlobalSDGConstruction = isGlobalSDGConstruction;
    }

    public void setAndroidVersion(String androidVersion) {
        this.androidVersion = androidVersion;
    }

    public void setResultFolder(String resultFolder) {
        this.resultFolder = resultFolder;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setAppPath(String appPath) {
        this.appPath = appPath;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    /**
     * @param androidJar the androidJar to set
     */
    public void setAndroidJar(String androidJar) {
        this.androidJar = androidJar;
    }

    /**
     * @param maxPathNumber the maxPathNumber to set
     */
    public void setMaxPathNumber(int maxPathNumber) {
        this.maxPathNumber = maxPathNumber;
    }

    /**
     * @return the isManifestAnalyzeFinish
     */
    public boolean isManifestAnalyzeFinish() {
        return isManifestClientFinish;
    }

    /**
     * @param isManifestAnalyzeFinish the isManifestAnalyzeFinish to set
     */
    public void setManifestAnalyzeFinish(boolean isManifestAnalyzeFinish) {
        this.isManifestClientFinish = isManifestAnalyzeFinish;
    }

    /**
     * @return the isFragementAnalyzeFinish
     */
    public boolean isFragmentAnalyzeFinish() {
        return isFragmentClientFinish;
    }

    /**
     * @param isFragmentAnalyzeFinish the isFragmentAnalyzeFinish to set
     */
    public void setFragmentAnalyzeFinish(boolean isFragmentAnalyzeFinish) {
        this.isFragmentClientFinish = isFragmentAnalyzeFinish;
    }

    /**
     * @return the isCallGraphAnalyzeFinish
     */
    public boolean isCallGraphAnalyzeFinish() {
        return isCallGraphClientFinish;
    }

    /**
     * @param isCallGraphAnalyzeFinish the isCallGraphAnalyzeFinish to set
     */
    public void setCallGraphAnalyzeFinish(boolean isCallGraphAnalyzeFinish) {
        this.isCallGraphClientFinish = isCallGraphAnalyzeFinish;
    }

    /**
     * @return the isOracleConstructionClientFinish
     */
    public boolean isOracleConstructionClientFinish() {
        return isOracleConstructionClientFinish;
    }

    /**
     * @param isOracleConstructionClientFinish the isOracleConstructionClientFinish to set
     */
    public void setOracleConstructionClientFinish(boolean isOracleConstructionClientFinish) {
        this.isOracleConstructionClientFinish = isOracleConstructionClientFinish;
    }

    /**
     * @return the isStaticValueAnalyzeFinish
     */
    public boolean isStaticValueAnalyzeFinish() {
        return isStaticValueAnalyzeFinish;
    }

    /**
     * @param isStaticValueAnalyzeFinish the isStaticValueAnalyzeFinish to set
     */
    public void setStaticValueAnalyzeFinish(boolean isStaticValueAnalyzeFinish) {
        this.isStaticValueAnalyzeFinish = isStaticValueAnalyzeFinish;
    }

    /**
     * @return the isSootAnalyzeFinish
     */
    public boolean isSootAnalyzeFinish() {
        return isSootAnalyzeFinish;
    }

    /**
     * @param isSootAnalyzeFinish the isSootAnalyzeFinish to set
     */
    public void setSootAnalyzeFinish(boolean isSootAnalyzeFinish) {
        this.isSootAnalyzeFinish = isSootAnalyzeFinish;
    }

    /**
     * @return the stopFlag
     */
    public boolean isStopFlag() {
        return stopFlag;
    }

    /**
     * @param stopFlag the stopFlag to set
     */
    public void setStopFlag(boolean stopFlag) {
        this.stopFlag = stopFlag;
    }

    /**
     * @param callGraphAlgorithm the callGraphAlgorithm to set
     */
    public void setCallGraphAlgorithm(String callGraphAlgorithm) {
        this.callGraphAlgorithm = callGraphAlgorithm;
    }

    public void setDefaultExcludes(List<String> defaultExcludes){
        this.defaultExcludes = defaultExcludes;
    }

    public void setCallbacksList(List<String> callbacksList){
        this.callbacksList = callbacksList;
    }

    public void setSourcesAPIList(List<String> sourcesAPIList) { this.sourcesAPIList = sourcesAPIList; }

    public void setSinksAPIList(List<String> sinksAPIList) { this.sinksAPIList = sinksAPIList; }

    public void setBothAPIList (List<String> bothAPIList) {
        this.bothAPIList = bothAPIList;
    }

    public void setICCFilePath(String iccFilePath){
        this.ICCFilePath = iccFilePath;
    }

    public void setFlowDroidResultPath(String flowDroidResultPath){
        this.flowDroidResultPath = flowDroidResultPath;
    }

    public void setFlowDroidOutputPath(String flowDroidOutputPath){
        this.flowDroidOutputPath = flowDroidOutputPath;
    }

    public void setFlowDroidProductPath(String flowDroidProductPath){
        this.flowDroidProductPath = flowDroidProductPath;
    }

    public void setStubDroidDir(String stubDroidDir) {
        this.stubDroidDir = stubDroidDir;
    }

    public void setStubDroidManualDir(String stubDroidManualDir) {
        this.stubDroidManualDir = stubDroidManualDir;
    }

    public void setVirtualEdgesPath(String virtualEdgesPath) {
        this.virtualEdgesPath = virtualEdgesPath;
    }

    /**
     * @param maxFunctionExpandNumber the maxFunctionExpandNumber to set
     */
    public void setMaxFunctionExpandNumber(int maxFunctionExpandNumber) {
        this.maxFunctionExpandNumber = maxFunctionExpandNumber;
    }

    /**
     * @param maxObjectSummarySize the maxObjectSummarySize to set
     */
    public void setMaxObjectSummarySize(int maxObjectSummarySize) {
        this.maxObjectSummarySize = maxObjectSummarySize;
    }

    public void setSlicingCriteriaFound(boolean slicingCriteriaFound) {
        this.isSlicingCriteriaFound = slicingCriteriaFound;
    }

    public boolean isSlicingCriteriaFound() {
        return isSlicingCriteriaFound;
    }

    public void setIsSlicerTimeOut(boolean isSlicerTimeOut) {
        this.isSlicerTimeout = isSlicerTimeOut;
    }

    public boolean isSlicerTimeOut() {
        return isSlicerTimeout;
    }

    public void setIsOriginalFlowDroidTimeOut(boolean isOriginalFlowDroidTimeOut) {
        this.isOriginalFlowDroidTimeout = isOriginalFlowDroidTimeOut;
    }

    public boolean isOriginalFlowDroidTimeOut() {
        return isOriginalFlowDroidTimeout;
    }

    public void setIsFinalFlowDroidTimeOut(boolean isFinalFlowDroidTimeOut) {
        this.isFinalFlowDroidTimeout = isFinalFlowDroidTimeOut;
    }

    public boolean isFinalFlowDroidTimeOut() {
        return isFinalFlowDroidTimeout;
    }

    public void setIsTotalTimeOut(boolean isTotalTimeOut) {
        this.isTotalTimeout = isTotalTimeOut;
    }

    public boolean isTotalTimeOut() {
        return isTotalTimeout;
    }

    public void setIsBackwardFinish(boolean isBackwardFinish) {
        this.isBackwardFinish = isBackwardFinish;
    }

    public boolean isBackwardFinish() {
        return isBackwardFinish;
    }

    public void setIsBackwardAndForwardFinish(boolean isBackwardAndForwardFinish) {
        this.isBackwardAndForwardFinish = isBackwardAndForwardFinish;
    }

    public boolean isBackwardAndForwardFinish() {
        return isBackwardAndForwardFinish;
    }

    public void setIsExtraFinish(boolean isExtraFinish) {
        this.isExtraFinish = isExtraFinish;
    }

    public boolean isExtraFinish() {
        return isExtraFinish;
    }
}
