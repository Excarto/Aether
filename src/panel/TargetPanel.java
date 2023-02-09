import java.awt.*;

// Panel displaying the targeted enemy unit at the bottom of the right-hand side panel.

public class TargetPanel extends UnitPanel{
	static final int SIZE_SMALL = GameWindow.MENU_WIDTH/2+2;
	static final int SIZE_BIG = GameWindow.MENU_WIDTH-30;
	
	Unit owner;
	
	public TargetPanel(boolean big){
		super(big ? SIZE_BIG : SIZE_SMALL, true, true);
		circleRadius = 12;
		if (big){
			this.add(Window.createSpacer(GameWindow.MENU_WIDTH-20, 5), 0);
		}else{
			this.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH, size));
			optionsPanel.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-size-15, size-35));
		}
	}
	
	protected void select(Component component){
		GameWindow gameWindow = ((HumanPlayer)owner.player).getWindow();
		if (gameWindow.getInputHandler().getWeaponToSetTarget() != null)
			gameWindow.getInputHandler().getWeaponToSetTarget().setSubTarget(component);
		gameWindow.getInputHandler().setWeaponToSetTarget(null);
	}
	
	protected void deselect(Component component){}
	
	public void setOwner(Unit owner){
		this.owner = owner;
		updateTarget(owner);
	}
	
	public void updateTarget(Unit owner){
		if (this.owner == owner){
			if (owner.getTarget() != null && owner.getTarget().target instanceof Unit){
				setUnit((Unit)owner.getTarget().target);
			}else
				setUnit(null);
			repaint();
		}
	}
	
	protected void paintWindow(Graphics g){
		if (unit != null && owner.getTarget() != null){
			//if (owner.getTarget().isVisible()){
			knowStatus = owner.player.knowHealth(unit);
			super.paintWindow(g);
			
			g.setColor(Color.RED);
			int length = size/10;
			g.drawLine(0, length, length, 0);
			g.drawLine(size-length, 0, size, length);
			g.drawLine(size-length, size, size, size-length);
			g.drawLine(0, size-length, length, size);
			
			for (Weapon weapon : owner.weapons){
				Component subTarget = weapon.getSubTarget();
				if (subTarget != null && subTarget.unit == unit){
					Component selectedComponent = Unit.weaponsPanel.getSelectedComponent();
					if (selectedComponent instanceof Weapon && ((Weapon)selectedComponent).getSubTarget() == subTarget){
						g.setColor(Color.RED);
					}else
						g.setColor(Color.ORANGE);
					
					int targetPosX = (int)(subTarget.hardpoint.posX*size);
					int targetPosY = (int)(subTarget.hardpoint.posY*size);
					
					g.drawLine(targetPosX-circleRadius, targetPosY-circleRadius,
							targetPosX-circleRadius*4/5, targetPosY-circleRadius*4/5);
					g.drawLine(targetPosX+circleRadius, targetPosY+circleRadius,
							targetPosX+circleRadius*4/5, targetPosY+circleRadius*4/5);
					g.drawLine(targetPosX+circleRadius, targetPosY-circleRadius,
							targetPosX+circleRadius*4/5, targetPosY-circleRadius*4/5);
					g.drawLine(targetPosX-circleRadius, targetPosY+circleRadius,
							targetPosX-circleRadius*4/5, targetPosY+circleRadius*4/5);
				}
			}
			//}
		}else{
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			g.setColor(Color.LIGHT_GRAY);
			g.setFont(new Font("Courier", Font.BOLD, 14));
			g.drawString("[No Target]", size/2-42, size/2);
		}
	}
}
