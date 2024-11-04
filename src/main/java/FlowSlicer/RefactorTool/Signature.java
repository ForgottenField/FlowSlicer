package FlowSlicer.RefactorTool;


public class Signature {

	public final static String ACTIVITY = "android.app.Activity";

	public final static String ONDESTROY_SUB = "void onDestroy()";

	public final static String EXECUTE = "android.os.AsyncTask execute(java.lang.Object[])";
	
	public final static String EXECUTE_ON_EXECUTOR = "android.os.AsyncTask executeOnExecutor(java.util.concurrent.Executor,java.lang.Object[])";
	
	public final static String CANCEL = "boolean cancel(boolean)";
	
	public final static String IS_CANCELLED = "boolean isCancelled()";
	
	public final static String DO_IN_BACKGROUND = "doInBackground";
	
	public final static String ON_PROGRESS = "onProgressUpdate";
	
	public final static String ON_PostEXECUTE = "onPostExecute";
	
	public final static String Executor_1 = "java.util.concurrent.Executor" ; 	
	
	public final static String Executor_2 = "AsyncTask.SERIAL_EXECUTOR" ; 
	
	public final static String StrongReference = null ; 
	
	public final static String WeakReference = null ; 
	
	public static final String ASYNCTASK = "android.os.AsyncTask";
}
