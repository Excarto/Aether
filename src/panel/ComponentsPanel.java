import static java.lang.Math.*;
import java.awt.*;
import javax.swing.*;

public class ComponentsPanel extends SidePanel{
	
	public final SelectPanel unitPanel;
	private Component selectedComponent;
	
	public ComponentsPanel(boolean isWeapons){
		super();
		
		this.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH, Unit.CONTROLS_HEIGHT));
		this.setLayout(Window.DEFAULT_LAYOUT);
		
		unitPanel = new SelectPanel(isWeapons);
		unitPanel.setOpaque(false);
		unitPanel.optionsPanel.setOpaque(false);
		unitPanel.showArcs.setOpaque(false);
		this.add(unitPanel);
	}
	
	public void paintComponent(Graphics g){
		g.drawImage(background, 0, 0, null);
	}
	
	public void refresh(){
		unitPanel.refresh();
	}
	
	public void setUnit(Unit unit){
		selectedComponent = null;
		unitPanel.setUnit(unit);
	}
	
	public Component getSelectedComponent(){
		return selectedComponent;
	}
	
	static final Color SELECTION_COLOR = new Color(20, 180, 20);
	private class SelectPanel extends UnitPanel{
		private JPanel detailPanel;
		
		public SelectPanel(boolean isWeapons){
			super(GameWindow.MENU_WIDTH, false, isWeapons);
			circleRadius = 18;
		}
		
		public void setUnit(Unit newUnit){
			super.setUnit(newUnit);
			if (detailPanel != null)
				ComponentsPanel.this.remove(detailPanel);
			ComponentsPanel.this.repaint();
			
			IdList<? extends Component> components = isWeapons ? unit.weapons : unit.systems;
			for (Component component : components){
				if (component.selected){
					select(component);
					break;
				}
			}
		}
		
		protected void select(Component component){
			selectedComponent = component;
			if (selectedComponent != null){
				component.selected = true;
				
				if (detailPanel != null)
					ComponentsPanel.this.remove(detailPanel);
				detailPanel = component.getMenu();
				ComponentsPanel.this.add(detailPanel);
				ComponentsPanel.this.revalidate();
				//ComponentsPanel.this.repaint();
			}else{
				for (Component comp : (isWeapons ? unit.weapons : unit.systems))
					comp.selected = false;
			}
			
			ComponentsPanel.this.repaint();
		}
		
		protected void deselect(Component component){
			if (component == selectedComponent){
				if (detailPanel != null)
					ComponentsPanel.this.remove(detailPanel);
				selectedComponent = null;
			}
			component.selected = false;
			
			ComponentsPanel.this.repaint();
		}
		
		public void paintWindow(Graphics g){
			((Graphics2D)g).setRenderingHints(Main.inGameHints);
			
			if (selectedComponent != null){
				g.setColor(SELECTION_COLOR);
				g.drawLine((int)(selectedComponent.hardpoint.posX*size), 0, (int)(selectedComponent.hardpoint.posX*size), getWidth());
				g.drawLine(0, (int)(selectedComponent.hardpoint.posY*size), getHeight(), (int)(selectedComponent.hardpoint.posY*size));
			}
			
			//super.paintWindow(g);
			
			g.setColor(SELECTION_COLOR);
			for (Component component : (isWeapons ? unit.weapons : unit.systems)){
				if (component.selected)
					g.drawOval((int)(component.hardpoint.posX*size-circleRadius)-1,
							(int)(component.hardpoint.posY*size-circleRadius)-1,
							circleRadius*2+1, circleRadius*2+1);
			}
			
			super.paintWindow(g);
			
			if (isWeapons){
				for (Weapon weapon : unit.weapons){
					int posX = (int)(weapon.hardpoint.posX*size);
					int posY = (int)(weapon.hardpoint.posY*size);
					g.setColor(weapon.isReady() ? Color.GREEN : Color.RED);
					g.drawLine(posX, posY,
							(int)(posX+circleRadius*sin(toRadians(weapon.getAngle()))),
							(int)(posY-circleRadius*cos(toRadians(weapon.getAngle()))));
					if (!Double.isNaN(weapon.getTargetAngle())){
						g.setColor(weapon.isTriggerPulled() ? Color.GREEN : Color.RED);
						int circlePosX = (int)(circleRadius*sin(toRadians(weapon.getTargetAngle())));
						int circlePosY = (int)(circleRadius*cos(toRadians(weapon.getTargetAngle())));
						g.drawLine(posX+circlePosX*3/4, posY-circlePosY*3/4, posX+circlePosX, posY-circlePosY);
					}
				}
			}
		}
		
		public void refresh(){
			//lineLength = min(lineLength+3*GameWindow.MENU_REFRESH_PER_SEC, 400);
			//super.refresh();
			if (detailPanel instanceof WeaponPanel)
				((WeaponPanel)detailPanel).refresh();
			else if (detailPanel instanceof SystemPanel)
				((SystemPanel)detailPanel).refresh();
			repaint();
		}
	}
	
	public class DamageLabel extends JLabel{
		private int damage;
		public void refresh(){
			if (selectedComponent != null && selectedComponent.getHull() != damage){
				damage = (int)selectedComponent.getHull();
				this.setText(100-100*damage/selectedComponent.type.hull + "% damage (" +
						ceil((selectedComponent.type.hull-damage)/(double)selectedComponent.type.hullPerMaterial) + " rm)");
				this.repaint();
			}
		}
	}
	
}
