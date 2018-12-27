import static java.lang.Math.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import java.awt.image.*;

import org.imgscalr.*;

public abstract class UnitPanel extends JPanel{
	
	Unit unit;
	int circleRadius;
	final int size;
	boolean isWeapons;
	boolean knowStatus;
	BufferedImage img;
	
	JPanel optionsPanel;
	JComponent window;
	JRadioButton weaponButton, systemButton;
	JCheckBox showArcs;
	
	public UnitPanel(int size, boolean incSelector, boolean isWeapons){
		this.setPreferredSize(new Dimension(size, size+(incSelector ? 38 : 20)));
		this.setLayout(Window.DEFAULT_LAYOUT);
		this.size = size;
		circleRadius = 15;
		this.isWeapons = isWeapons;
		knowStatus = true;
		
		window = new JComponent(){
			public void paint(Graphics g){
				paintWindowBackground(g);
				paintWindow(g);
		}};
		window.setPreferredSize(new Dimension(size, size));
		window.setDoubleBuffered(true);
		window.setOpaque(true);
		window.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		window.addMouseListener(new SelectListener());
		
		if (incSelector){
			weaponButton = new JRadioButton("Weapons");
			weaponButton.setPreferredSize(new Dimension(100, 14));
			weaponButton.addActionListener(new CategoryListener());
			systemButton = new JRadioButton("Systems");
			systemButton.setPreferredSize(new Dimension(100, 14));
			systemButton.addActionListener(new CategoryListener());
			ButtonGroup group = new ButtonGroup();
			group.add(weaponButton);
			group.add(systemButton);
			(isWeapons ? weaponButton : systemButton).setSelected(true);
		}
		
		showArcs = new JCheckBox("Show Arcs");
		showArcs.setPreferredSize(new Dimension(100, 14));
		showArcs.setSelected(true);
		showArcs.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e){
				window.repaint();
			}
		});
		
		optionsPanel = new JPanel();
		optionsPanel.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH, 38));
		optionsPanel.setLayout(Window.DEFAULT_LAYOUT);
		if (incSelector){
			JPanel typePanel = new JPanel(Window.DEFAULT_LAYOUT);
			typePanel.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH/2-5, 38));
			typePanel.setOpaque(false);
			typePanel.add(weaponButton);
			typePanel.add(systemButton);
			optionsPanel.add(typePanel);
		}
		if (incSelector || isWeapons)
			optionsPanel.add(showArcs);
		this.add(window);
		this.add(optionsPanel);
	}
	
	public void setUnit(Unit newUnit){
		if (newUnit != null){
			if (img != null)
				img.flush();
			img = Scalr.resize(newUnit.type.topImg, Scalr.Method.QUALITY, size, size);
			/*img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = (Graphics2D)img.getGraphics();
			g2d.scale((double)size/unit.type.topImg.getWidth(), (double)size/unit.type.topImg.getHeight());
			g2d.drawImage(unit.type.topImg, 0, 0, null);*/
		}
		unit = newUnit;
		repaint();
	}
	
	public Unit getUnit(){
		return unit;
	}
	
	protected abstract void select(Component component);
	protected abstract void deselect(Component component);
	
	private void paintWindowBackground(Graphics g){
		if (unit != null){
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, img.getWidth(), img.getHeight());
			g.drawImage(img, 0, 0, null);
		}
	}
	
	static final Color HEALTHY_COLOR = new Color(50, 200, 50, 110);
	static final Color DAMAGED_COLOR = new Color(125, 125, 50, 110);
	static final Color DEAD_COLOR = new Color(200, 50, 50, 110);
	static final Color UNKNOWN_COLOR = new Color(100, 100, 100, 110);
	static final Color ARC_COLOR = new Color(100, 100, 100, 200);
	protected void paintWindow(Graphics g){
		if (unit != null){
			((Graphics2D)g).setRenderingHints(Main.inGameHints);
			
			//g.setColor(Color.BLACK);
			//g.fillRect(0, 0, img.getWidth(), img.getHeight());
			//g.drawImage(img, 0, 0, null);
			
			for (Component component : (isWeapons ? unit.weapons : unit.systems)){
				int posX = (int)(component.hardpoint.posX*size);
				int posY = (int)(component.hardpoint.posY*size);
				if (knowStatus){
					if (component.getHull() > component.type.hull/4){
						g.setColor(HEALTHY_COLOR);
					}else if (component.getHull() > 0){
						g.setColor(DAMAGED_COLOR);
					}else
						g.setColor(DEAD_COLOR);
				}else
					g.setColor(UNKNOWN_COLOR);
				g.fillOval(posX-circleRadius, posY-circleRadius, circleRadius*2, circleRadius*2);
				
				g.drawImage(component.type.icon,
						posX-component.type.icon.getWidth()/2,
						posY-component.type.icon.getHeight()/2, null);
				
				if (isWeapons && showArcs.isSelected()){
					g.setColor(ARC_COLOR);
					g.fillArc(posX-circleRadius, posY-circleRadius,
							circleRadius*2, circleRadius*2,
							90-((Weapon)component).getMountAngle()-((Weapon)component).getArc(),
							((Weapon)component).getArc()*2);
				}
			}
		}
	}
	
	private class SelectListener extends MouseAdapter{
		public void mousePressed(MouseEvent e){
			if (unit != null){
				for (Component component : (isWeapons ? unit.weapons : unit.systems)){
					if (abs(component.hardpoint.posX*size-e.getX()) < circleRadius &&
							abs(component.hardpoint.posY*size-e.getY()) < circleRadius){
						if (e.getButton() == MouseEvent.BUTTON1){
							UnitPanel.this.select(component);
						}else
							UnitPanel.this.deselect(component);
						repaint();
					}
				}
			}
		}
	}
	
	private class CategoryListener implements ActionListener{
		public void actionPerformed (ActionEvent e){
			showArcs.setEnabled(weaponButton.isSelected());
			isWeapons = weaponButton.isSelected();
			select(null);
		}
	}
}
