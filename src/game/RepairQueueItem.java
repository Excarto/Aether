
public class RepairQueueItem {
	public final Repairable repairable;
	public final RepairTarget target;
	public final double repairTo;
	public final double repairRate;
	public final boolean scrap;
	public final boolean isAuto;
	
	public RepairQueueItem(Repairable repairable, RepairTarget target,
			double repairTo, double repairRate, boolean scrap, boolean isAuto){
		this.repairable = repairable;
		this.target = target;
		this.repairTo = repairTo;
		this.repairRate = repairRate;
		this.scrap = scrap;
		this.isAuto = isAuto;
	}
}
