package FlowSlicer.RefactorTool;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Pattern;
import java.io.*;
@Slf4j
public class Configuration {
	public Vector<Vector<String>> Operations;
	
	public void writeConf(Vector<Vector<String>> Operations, String filePath) {
		this.Operations = Operations;
		try {
			BufferedWriter write = new BufferedWriter(new FileWriter(filePath));
			for (Vector<String> Operation : Operations) {
				String Method = "Method=" + Operation.elementAt(0);
				String Parameters = "Parameters=";
				for (int i = 1; i < Operation.size(); i++) {
					if (i != Operation.size() - 1) Parameters += Operation.elementAt(i) + ",";
					else Parameters += Operation.elementAt(i);
				}
				write.write(Method + " " + Parameters + "\n");
            }
			write.close();
        }
        catch (Exception e) {
            log.info("Fail to Write Configuration.");
            e.printStackTrace();
        }
	}
	
	public void readConf(String filePath){
		Operations = new Vector<Vector<String>>();
        try {
        	String encoding = "GBK";
            File file = new File(filePath);
            if (file.isFile() && file.exists()) {
                InputStreamReader read = new InputStreamReader(new FileInputStream(file), encoding);
                BufferedReader bufferedReader = new BufferedReader(read);
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    Vector<String> Operation = new Vector<String>();
                    String[] str = line.split(" ");
                    String Method = str[0].split("=")[1];
                    String[] Parameters = str[1].split("=")[1].split(",");
                    Operation.add(Method);
                    for (String Parameter : Parameters) {
                    	Operation.add(Parameter);
                    }
                    Operations.add(Operation);
                }
                read.close();
            }
            else{
            	log.info("The Configuration Cannot Be Found.");
            }
        }
        catch (Exception e) {
            log.info("Fail to Read Configuration.");
            e.printStackTrace();
        }
    }
	
	public void checkConf() {
		for (Vector<String> Operation : Operations) {
			if (Operation.size() <= 1) errorConf(Operation);

			String Method = Operation.elementAt(0);
			if (Method.equals("MoveComp")) {
				if (Operation.size() != 3) errorConf(Operation);
				if (!checkCompExist(Operation.elementAt(1))) errorConf(Operation);
				if (!checkDigit(Operation.elementAt(2))) errorConf(Operation);
			}
			else if (Method.equals("Move")) {
				if (Operation.size() != 3) errorConf(Operation);
				if (!checkCompExist(Operation.elementAt(1))) errorConf(Operation);
				if (!checkCompExist(Operation.elementAt(2))) errorConf(Operation);
				if (checkPreNode(Operation.elementAt(2), Operation.elementAt(1))) errorConf(Operation);
			}
			else if (Method.equals("DeleteComp")) {
				if (Operation.size() != 2) errorConf(Operation);
				if (!checkCompExist(Operation.elementAt(1))) errorConf(Operation);
			}
			else if (Method.equals("AddComp")) {
				if (Operation.size() != 3) errorConf(Operation);
				if (checkCompExist(Operation.elementAt(1))) errorConf(Operation);
				if (!checkCompExist(Operation.elementAt(2))) errorConf(Operation);
			}
			else if (Method.equals("ExchangeComp")) {
				if (Operation.size() != 3) errorConf(Operation);
				if (!checkCompExist(Operation.elementAt(1))) errorConf(Operation);
				if (!checkCompExist(Operation.elementAt(2))) errorConf(Operation);
			}
			else if (Method.equals("ReplaceComp")) {
				if (Operation.size() != 3) errorConf(Operation);
				if (!checkCompExist(Operation.elementAt(1))) errorConf(Operation);
				if (!checkCompExist(Operation.elementAt(2))) errorConf(Operation);
			}
			else errorConf(Operation);
		}
	}
	
	public boolean checkDigit(String number) {
		Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
		if (!pattern.matcher(number).matches()) return false;
		if (Integer.parseInt(number) <= 0) return false;
		return true;
	}
	
	public boolean checkPreNode(String father, String son) {
		// todo
		return true;
	}
	
	public boolean checkCompExist(String comp) {
		// todo
		return true;
	}
	
	public void errorConf(Vector<String> Operation) {
		log.info("Error in Configuration.");
		printConf(Operation);
		System.exit(0);
	}
	
	public void printConf(Vector<String> Operation) {
		String Method = Operation.elementAt(0);
		String Parameters = "(";
		for (int i = 1; i < Operation.size(); i++) {
			if (i != Operation.size() - 1) Parameters += Operation.elementAt(i) + ", ";
			else Parameters += Operation.elementAt(i) + ")";
		}
		log.info(Method + " " + Parameters);
	}
	
	public Vector<Vector<String>> getConf() {
		return Operations;
	}
}
