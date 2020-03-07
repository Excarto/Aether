
public class RepairTarget{
	public final Unit unit;
	public final double repairRate;
	public final boolean isInCraftBay;
	public boolean inRange;
	public boolean repair, recharge, rearm;
	public boolean doneRepair, doneRecharge, doneRearm;
	
	public RepairTarget(Unit unit, double repairRate, boolean isInCraftBay){
		this.unit = unit;
		this.repairRate = repairRate;
		this.isInCraftBay = isInCraftBay;
		inRange = true;
		repair = true;
		recharge = true;
		rearm = true;
		doneRepair = false;
		doneRecharge = false;
		doneRearm = false;
	}
}
