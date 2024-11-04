package FlowSlicer.RefactorTool;

import javax.swing.*; 
import java.awt.*; 
import java.awt.event.ActionEvent; 
import java.awt.event.ActionListener;
import java.util.Vector;

public class ConfigurationUI extends JFrame implements ActionListener {
	private JButton Add, Delete, Complete;
	private JComboBox SelectMethod;
	private JTextField Parameters;
	private JTextArea Configuration;
	private JPanel Menu, Text;
	private String[] Methods = {"MoveComp", "Move", "DeleteComp", "AddComp", "ExchangeComp",
								"ReplaceComp", "MoveFrag", "DeleteFrag", "AddFrag", "ExchangeFrag",
								"ReplaceFrag", "ReplaceMtd", "DeleteMtd"};
	
	private Vector<Vector<String>> Operations;
	private Configuration conf;
	private String filePath;
	
	public boolean complete;
	
	ConfigurationUI(String filePath) {
		complete = false;
		
		this.filePath = filePath;
		conf = new Configuration();
		conf.readConf(filePath);
		Operations = conf.getConf();
		
		Add = new JButton("Add");
		Delete = new JButton("Delete");
		Complete = new JButton("Complete");
		Add.addActionListener(this);
		Delete.addActionListener(this);
		Complete.addActionListener(this);
		
		SelectMethod = new JComboBox(Methods);
		Parameters = new JTextField(50);
		Configuration = new JTextArea(100, 90);
		Configuration.setEditable(false);
		Configuration.setText(showConf());
		
		Menu = new JPanel();
		Menu.setSize(1000, 100);
		Text = new JPanel();
		Text.setSize(1000, 800);
		Menu.add(SelectMethod);
		Menu.add(Parameters);
		Menu.add(Add);
		Menu.add(Delete);
		Menu.add(Complete);
		Text.add(Configuration);
		
		this.add(Menu, BorderLayout.NORTH);
		this.add(Text, BorderLayout.CENTER);
	    this.setSize(1000, 900);
	    this.setLocation(400, 200);
	    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    this.setVisible(true); 
	    this.setResizable(true); 
	}
	
	public String showConf() {
		String Conf = "";
		for (Vector<String> Operation : Operations) {
			String Method = Operation.elementAt(0);
			String Parameter = "(";
			for (int i = 1; i < Operation.size(); i++) {
				if (i != Operation.size() - 1) Parameter += Operation.elementAt(i) + ", ";
				else Parameter += Operation.elementAt(i) + ")";
			}
			Conf += Method + Parameter + "\n";
		}
		return Conf;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == "Complete") {
			this.setVisible(false);
			conf.writeConf(Operations, filePath);
			conf.checkConf();
			complete = true;
		}
		else {
			String Method = SelectMethod.getSelectedItem().toString();
			String[] Parameter = Parameters.getText().split(",");
			Vector<String> Operation = new Vector<String>();
			Operation.add(Method);
			for (String parameter : Parameter) Operation.add(parameter);
			if (e.getActionCommand() == "Add") Operations.add(Operation);
			else Operations.remove(Operation);
			Configuration.setText(showConf());
		}
	}
	
	public boolean isComplete() {
		return complete;
	}
	
	public static void main(String argv[]){
        ConfigurationUI UI = new ConfigurationUI("Configuration.txt");
    }

}
