import static java.lang.Math.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class MissionArenaWindow extends Window{
	
	public MissionArenaWindow(Mission mission){
		super(Size.SMALL);
		
		final JComponent previewWindow = new JComponent(){
			public void paint(Graphics graphics){
				Graphics2D g = (Graphics2D)graphics;
				g.setRenderingHints(Main.menuHints);
				mission.arena.drawPreview(g, min(getHeight(), getWidth()));
			}
		};
		previewWindow.setPreferredSize(new Dimension(320, 320));
		
		JPanel exitPanel = new JPanel();
		exitPanel.setPreferredSize(new Dimension(900, 100));
		exitPanel.setOpaque(false);
		JButton backButton = new JButton("Back");
		backButton.setPreferredSize(new Dimension(90, 25));
		backButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				Main.removeWindow();
		}});
		exitPanel.add(backButton);
		
		this.add(previewWindow);
		this.add(exitPanel);
	}
	
}
