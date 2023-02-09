
// Craft class adds very little to the base unit class other than keeping track of and docking with the mothership

public class Craft extends Unit{
	
	public final CraftType type;
	
	private Ship mothership;
	private boolean returnToMothership;
	private int lastDockTime;
	
	public Craft(CraftType type){
		super(type);
		this.type = type;
		setName(type.className);
	}
	
	public void act(){
		super.act();
		
		if (returnToMothership && Main.game.turn%100 == 0){
			boolean noAmmo = !weapons.isEmpty();
			for (Weapon weapon : weapons){
				if (weapon.type.ammoType == -1 || ammo[weapon.type.ammoType] > 0)
					noAmmo = false;
			}
			if (noAmmo){// || getHull() < type.hull/4){
				returnToMothership();
				returnToMothership = false;
			}
		}
	}
	
	public void returnToMothership(){
		if (mothership != null && orders != null){
			orders.clear();
			orders.queueOrder(new Dock(mothership));
		}
	}
	
	public void setMothership(Ship ship){
		this.mothership = ship;
		returnToMothership = ship.craftDefaultAmmo || ship.craftDefaultRepair;
	}
	
	public Ship getMothership(){
		return mothership;
	}
	
	public void removeControllable(Controllable controllable){
		super.removeControllable(controllable);
		
		if (controllable == mothership)
			mothership = null;
	}
	
	public boolean canDock(){
		return Main.game.turn-lastDockTime > Main.TPS;
	}
	
	public void setDockTime(){
		lastDockTime = Main.game.turn;
	}
	
	public void dock(){
		if (orders != null)
			orders.clear();
		Main.game.playSound(type.dockSound, this, true);
	}
	
	public void explode(){
		super.explode();
		Main.game.createDeathExplosion(type, getPosX(), getPosY(), getVelX(), getVelY());
	}
	
	public SidePanel getMenu(){
		return super.getMenu();
	}
}
