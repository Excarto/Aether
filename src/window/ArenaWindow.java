import static java.lang.Math.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

public class ArenaWindow extends Window{
	
	private final SetupWindow parentWindow;
	private final JList<Arena> arenaList;
	private final JPanel rightPanel;
	private final JLabel budgetLabel, startBudgetLabel, scoreLabel;
	
	private Arena arena;
	
	public ArenaWindow (SetupWindow setupWindow){
		super(Size.NORMAL);
		arena = setupWindow.arena;
		parentWindow = setupWindow;
		
		final JComponent previewWindow = new JComponent(){
			public void paint(Graphics graphics){
				Graphics2D g = (Graphics2D)graphics;
				g.setRenderingHints(Main.menuHints);
				arena.drawPreview(g, min(getHeight(), getWidth()));
			}
		};
		previewWindow.setPreferredSize(new Dimension(400, 400));
		
		budgetLabel = new JLabel();
		budgetLabel.setPreferredSize(new Dimension(150, 20));
		startBudgetLabel = new JLabel();
		startBudgetLabel.setPreferredSize(new Dimension(150, 20));
		scoreLabel = new JLabel();
		scoreLabel.setPreferredSize(new Dimension(150, 20));
		rightPanel = new JPanel();
		rightPanel.setPreferredSize(new Dimension(160, 250));
		rightPanel.setBackground(Color.WHITE);
		rightPanel.setBorder(BorderFactory.createEtchedBorder());
		rightPanel.add(budgetLabel);
		rightPanel.add(startBudgetLabel);
		rightPanel.add(scoreLabel);
		
		Vector<Arena> availArenas = new Vector<Arena>(Main.arenas.length);
		for (Arena arena : Main.arenas){
			if (!arena.missionOnly)
				availArenas.add(arena);
		}
		arenaList = new JList<Arena>(availArenas);
		arenaList.setPreferredSize(new Dimension(160, 250));
		arenaList.setBorder(BorderFactory.createEtchedBorder());
		arenaList.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent arg0){
				arena = arenaList.getSelectedValue();
				budgetLabel.setText("Default budget: " + arena.defaultBudget);
				startBudgetLabel.setText("Starting budget: " + (int)round(arena.startBudget*100) + "%");
				scoreLabel.setText("Victory score: " + arena.maxScore);
				previewWindow.repaint();
		}});
		arenaList.setSelectedValue(arena, true);
		
		JPanel exitPanel = new JPanel();
		exitPanel.setPreferredSize(new Dimension(700, 50));
		exitPanel.setOpaque(false);
		JButton exitButton = new JButton("Select");
		exitButton.setPreferredSize(new Dimension(120, 40));
		exitButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				Main.removeWindow();
				parentWindow.returnValue(arena);
		}});
		exitPanel.add(exitButton);
		
		//JLabel titleLabel = new JLabel("Select Map", JLabel.CENTER);
		//titleLabel.setPreferredSize(new Dimension(900, 150));
		//titleLabel.setFont(new Font("Courier", Font.BOLD, 25));
		
		this.add(new Title("Select Map", 900, 150));
		this.add(arenaList);
		this.add(previewWindow);
		this.add(rightPanel);
		this.add(exitPanel);
	}
}
