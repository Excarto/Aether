import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

// Side panel used when nothing is selected. Displays a list of all the player's ships

public class DefaultPanel extends SidePanel{
	static final int SHIP_PANEL_HEIGHT = 40;
	
	private final HumanPlayer player;
	private final JPanel shipsPanel;
	
	public DefaultPanel(HumanPlayer player){
		super();
		this.player = player;
		
		shipsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2)){
			public void paintComponent(Graphics g){
				g.drawImage(background, 0, 0, null);
			}
		};
		for (Ship ship : player.ships)
			shipsPanel.add(new ShipPanel(ship), 0);
		shipsPanel.setPreferredSize(new Dimension(
				GameWindow.MENU_WIDTH-24, 2+shipsPanel.getComponentCount()*(SHIP_PANEL_HEIGHT+2)));
		
		JScrollPane scrollPane = new JScrollPane(shipsPanel);
		scrollPane.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-4, Main.resY-3));
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		this.add(scrollPane);
	}
	
	public void refresh(){
		for (int x = 0; x < shipsPanel.getComponentCount(); x++)
			((ShipPanel)shipsPanel.getComponent(x)).refresh();
	}
	
	static final Color AWAY_COLOR = new Color(170, 170, 170);
	static final Color ALIVE_COLOR = new Color(230, 230, 230);
	static final Color DEAD_COLOR = new Color(170, 100, 100);
	private class ShipPanel extends JPanel{
		
		final Ship ship;
		final JButton warpButton;
		
		public ShipPanel(Ship unit){
			super(new FlowLayout(FlowLayout.CENTER, 2, 1));
			ship = unit;
			this.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-29, SHIP_PANEL_HEIGHT));
			
			this.addMouseListener(new MouseAdapter(){
				public void mousePressed(MouseEvent e) {
					((HumanPlayer)ship.player).getWindow().select(ship);
				}
			});
			
			JComponent icon = new JComponent(){
				public void paint(Graphics g){
					g.drawImage(ship.getIcon(), 0, 0, null);
			}};
			icon.setPreferredSize(new Dimension(UnitType.ICON_SIZE, UnitType.ICON_SIZE));
			
			JLabel nameLabel = new JLabel(ship.getName());
			nameLabel.setPreferredSize(new Dimension(145, 15));
			nameLabel.setHorizontalAlignment(JLabel.CENTER);
			JLabel costLabel = new JLabel("Cost:"+ship.getCost());
			costLabel.setPreferredSize(new Dimension(145, 15));
			costLabel.setHorizontalAlignment(JLabel.CENTER);
			
			JPanel labelPanel = new JPanel(Window.NO_BORDER);
			labelPanel.setPreferredSize(new Dimension(145, SHIP_PANEL_HEIGHT-8));
			labelPanel.add(nameLabel);
			labelPanel.add(costLabel);
			labelPanel.setOpaque(false);
			
			warpButton = new JButton("Warp");
			warpButton.setPreferredSize(new Dimension(40, 22));
			warpButton.setMargin(new Insets(1,1,1,1));
			warpButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					if (ship.player.getBudget() >= ship.getCost())
						ship.player.warpIn(ship);
			}});
			
			this.add(icon);
			this.add(labelPanel);
			this.add(warpButton);
		}
		
		public void refresh(){
			warpButton.setEnabled(ship.outOfArena);
			if (ship.outOfArena){
				setBackground(AWAY_COLOR);
				setBorder(Window.UNSELECTED_BORDER);
			}else{
				if (ship.getHull() < 0){
					setBackground(DEAD_COLOR);
					setBorder(Window.UNSELECTED_BORDER);
				}else{
					setBackground(ALIVE_COLOR);
					setBorder(player.getWindow().isSelected(ship) ? Window.SELECTED_BORDER : Window.UNSELECTED_BORDER);
				}
			}
		}
	}
	
}
