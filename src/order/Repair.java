import java.awt.*;

public class Repair extends Escort{
	
	RepairTarget repairTarget;
	
	public Repair(Unit unit){
		super(unit, 0, 0, Main.TPS/4);
	}
	
	public void setHost(Controllable host){
		super.setHost(host);
		
		repairTarget = new RepairTarget((Unit)target, ((Ship)host).type.repairRate, false);
		repairTarget.repair = true;
		repairTarget.rearm = false;
		repairTarget.inRange = false;
		((Ship)host).repairTargets.add(repairTarget);
		((Ship)host).repairQueue.clear();
	}
	
	public void finish(){
		if (host != null)
			((Ship)host).removeRepairTarget(repairTarget);
	}
	
	public void act(){
		super.act();
		
		if (time%50 == 0){
			repairTarget.inRange = ((Ship)host).distance(target) < Main.repairDistance;
			if (repairTarget.doneRearm && repairTarget.doneRecharge && repairTarget.doneRepair)
				((Unit)host).orders().finish(this);
		}
	}
	
	public Color getColor(){
		return new Color(0, 255, 0, OPAQUE);
	}
}
