package FlowSlicer.RefactorTool;

import java.util.Vector;

import lombok.Getter;
import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.android.manifest.ProcessManifest;


public class RefactorTool {
	@Getter
	private static String apkName;
	@Getter
	private static String apkPath;

	public static void setApkName(String apkName) {
		RefactorTool.apkName = apkName;
		RefactorTool.apkPath = "apk/"+apkName+".apk";
	}

	public static void main(String[] args) {
		System.out.println("main");
	}
	
	public static void refactor(Vector<Vector<String>> Operations) {
		SootAnalyzer sootAnalyzer = new SootAnalyzer();
		sootAnalyzer.sootInit();
		sootAnalyzer.sootWriteOutput();

		sootAnalyzer.Delete_Method("com.example.testBench.SonInterface1", "myInterface", "void");

		sootAnalyzer.Replace_Method("MainActivity", "onCreate", "setOnClickListener");

		sootAnalyzer.Replace_Method("MainActivity", "onCreate", "setOnClickListener", "setOnTouchListener");

		sootAnalyzer.Replace_Component("com.example.testBench.override.TestInterface1", "com.example.testBench.SonInterface1", "com.example.testBench.SonInterface2");
		
		sootAnalyzer.Add_Component("HelloWorld", "com.example.testBench.MainActivity");
		
		sootAnalyzer.Delete_Method("com.example.testBench.complete.motivatingExample", "analyzeIntent", "void");
		
		sootAnalyzer.Exchange_Component("com.example.testBench.SonInterface1", "com.example.testBench.SonInterface2");
		
		sootAnalyzer.Delete_Component("com.example.testBench.override.FatherClass");
		
		for (Vector<String> Operation : Operations) {
			String Method = Operation.elementAt(0);
			if (Method.equals("AddComp")) {
				String classname = Operation.elementAt(1);
				String prenode = Operation.elementAt(2);
				sootAnalyzer.Add_Component(classname, prenode);
			}
			if (Method.equals("DeleteComp")) {
				String classname = Operation.elementAt(1);
				sootAnalyzer.Delete_Component(classname);
			}
		}

		sootAnalyzer.sootWriteOutput();
	}
}
