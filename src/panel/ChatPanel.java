import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// Pre-game server lobby chat interface

public class ChatPanel extends JPanel{
	static final int WIDTH = 500, HEIGHT = 85;
	
	private TextMsg textMsg;
	private JTextArea chatArea;
	private JScrollPane scrollPane;
	private JTextField textField;
	
	public ChatPanel(final Connection connection){
		this.setLayout(Window.DEFAULT_LAYOUT);
		this.setPreferredSize(new Dimension(WIDTH+4, HEIGHT+26));
		this.setBorder(BorderFactory.createEtchedBorder());
		
		textMsg = new TextMsg();
		
		chatArea = new JTextArea();
		//chatArea.setPreferredSize(new Dimension(WIDTH-10, 20));
		chatArea.setEditable(false);
		chatArea.setLineWrap(true);
		scrollPane = new JScrollPane();
		scrollPane.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setViewportView(chatArea);
		
		textField = new JTextField();
		textField.setPreferredSize(new Dimension(WIDTH, 20));
		textField.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if (!textField.getText().isEmpty()){
					textMsg.message = textField.getText();
					connection.send(textMsg);
					//print(" >> " + textField.getText());
					textField.setText(null);
				}
		}});
		
		this.add(scrollPane);
		this.add(textField);
	}
	
	public void print(String text){
		chatArea.setText(chatArea.getText() + text + "\n");
		//chatArea.setPreferredSize(new Dimension(WIDTH-10, 14*chatArea.getLineCount()-10));
		//scrollPane.revalidate();
		scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
	}
}
