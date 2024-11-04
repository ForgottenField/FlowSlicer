package FlowDroid;

import FlowSlicer.FlowdroidHelper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

@Slf4j
public class FlowDroidTest1 {
    private String androidDirPath;
    private String apkFilePath;
    private String sourceSinkFilePath;

    public void sootInit() {
        InfoflowAndroidConfiguration conf = new InfoflowAndroidConfiguration();
        // androidDirPath android sdk中platforms目录的路径
        conf.getAnalysisFileConfig().setAndroidPlatformDir(androidDirPath);
        // apkFilePath 要分析的apk的文件路径
        conf.getAnalysisFileConfig().setTargetAPKFile(apkFilePath);
        // sourceSinkFilePath source点与sink点的声明文件
        conf.getAnalysisFileConfig().setSourceSinkFile(sourceSinkFilePath);
        // apk中的dex文件有对方法数量的限制导致实际app中往往是多dex，不作设置将仅分析classes.dex
        conf.setMergeDexFiles(true);
        // 设置AccessPath长度限制，默认为5，设置负数表示不作限制
//        conf.getAccessPathConfiguration().setAccessPathLength(-1);
        // 设置Abstraction的path长度限制，设置负数表示不作限制
//        conf.getSolverConfiguration().setMaxAbstractionPathLength(-1);
        SetupApplication setup = new SetupApplication(conf);
        // 设置Callback的声明文件（不显式地设置好像FlowDroid会找不到）
        setup.setCallbackFile("AndroidCallbacks.txt");
        try {
            setup.runInfoflow();
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testFlowDroid(){
        long startTime = System.currentTimeMillis(); //程序开始记录时间
        FlowDroidTest1 test = new FlowDroidTest1();
        test.androidDirPath = "../FlowSlicer.RefactorTool/lib";
        test.sourceSinkFilePath = "SourcesAndSinks.txt";

        test.apkFilePath = "apk/com.softnet.prayertime.apk";
        test.apkFilePath = "apk/CSipSimple.apk";
        test.sootInit();
        long endTime = System.currentTimeMillis(); //程序结束记录时间
        long TotalTime = endTime - startTime; //总消耗时间
        log.info("TotalTime: " + TotalTime);
    }
}
