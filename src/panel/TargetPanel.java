import java.awt.*;

public class TargetPanel extends UnitPanel{
	static final int SIZE = GameWindow.MENU_WIDTH/2+2;
	
	Unit owner;
	
	public TargetPanel(){
		super(SIZE, true, true);
		circleRadius = 12;
		this.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH, SIZE));
		optionsPanel.setPreferredSize(new Dimension(GameWindow.MENU_WIDTH-SIZE-2, GameWindow.MENU_WIDTH/3));
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
			g.drawString("[No Target]", 23, SIZE/2);
		}
	}
}
