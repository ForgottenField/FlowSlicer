package FlowDroid;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;

import java.io.File;
import java.io.IOException;
@Slf4j
public class FlowDroidTest2 {
    private String androidDirPath = "../FlowSlicer.RefactorTool/lib";
    private String apkFilePath = "apk/";
    private String sourceSinkFilePath = "SourcesAndSinks.txt";


    @Test
    public void testFlowDroid(){
        long startTime = System.currentTimeMillis(); // 程序开始记录时间
        File dir = new File(apkFilePath);
        File[] files = dir.listFiles();
        for (File file : files) {
            String filename = file.getName();
            log.info(filename);
            testSingleFile(filename);
        }
        long endTime = System.currentTimeMillis(); // 程序结束记录时间
        long TotalTime = endTime - startTime; // 总消耗时间
        log.info("TotalTime: " + TotalTime);
    }


    public static void testSingleFile(String filename) {
        log.info(filename);
        long startTime = System.currentTimeMillis(); // 程序开始记录时间
        FlowDroidTest2 test = new FlowDroidTest2();
        test.run(filename);
        long endTime = System.currentTimeMillis(); // 程序结束记录时间
        long TotalTime = endTime - startTime; // 总消耗时间
        log.info(filename);
        log.info("TotalTime: " + TotalTime);
        if (TotalTime >= 1200000) {
            log.info(filename);
            System.exit(0);
        }
    }
    public void run(String filename) {
        InfoflowAndroidConfiguration conf = new InfoflowAndroidConfiguration();
        // androidDirPath android sdk中platforms目录的路径
        conf.getAnalysisFileConfig().setAndroidPlatformDir(androidDirPath);
        // apkFilePath 要分析的apk的文件路径
        conf.getAnalysisFileConfig().setTargetAPKFile(apkFilePath + filename);
        // sourceSinkFilePath source点与sink点的声明文件
        conf.getAnalysisFileConfig().setSourceSinkFile(sourceSinkFilePath);
        // apk中的dex文件有对方法数量的限制导致实际app中往往是多dex，不作设置将仅分析classes.dex
        conf.setMergeDexFiles(true);
        // 设置AccessPath长度限制，默认为5，设置负数表示不作限制
        conf.getAccessPathConfiguration().setAccessPathLength(-1);
        // 设置Abstraction的path长度限制，设置负数表示不作限制
        conf.getSolverConfiguration().setMaxAbstractionPathLength(-1);
        conf.getCallbackConfig().setCallbackAnalysisTimeout(600);
        conf.setDataFlowTimeout(600);
        conf.getPathConfiguration().setPathReconstructionMode(InfoflowConfiguration.PathReconstructionMode.Precise);
        SetupApplication setup = new SetupApplication(conf);
        // 设置Callback的声明文件（不显式地设置好像FlowDroid会找不到）
        setup.setCallbackFile("AndroidCallbacks.txt");
        try {
            setup.runInfoflow();
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
    }


}
