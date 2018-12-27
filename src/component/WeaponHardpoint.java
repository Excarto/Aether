import static java.lang.Math.*;

public class WeaponHardpoint extends Hardpoint{
	public final int arc;
	public final int angle;
	
	public WeaponHardpoint(double posX, double posY, double posZ, int mass, int angle, int arc){
		super(posX, posY, posZ, mass);
		this.angle = angle;
		this.arc = max(Weapon.MIN_ARC, arc/2);
	}
	
	public int getMaxArc(WeaponType type){
		int maxArc = Weapon.MIN_ARC;
		int nextMax = maxArc;
		while(nextMax <= arc && type.getMass(nextMax) <= mass){
			maxArc = nextMax;
			nextMax = nextMax < Weapon.ARC_INCREMENT ? Weapon.ARC_INCREMENT : nextMax + Weapon.ARC_INCREMENT;
		}
		return maxArc;
	}
	
	public WeaponType autoLoadout(Unit unit){
		double highestVal = 0;
		WeaponType highestType = null;
		for (WeaponType type : Main.weaponTypes){
			if (type.mass <= mass){
				int existingCount = 0;
				for (Weapon weapon : unit.weapons){
					if (weapon.type == type)
						existingCount++;
				}
				int maxAmmo = 5;
				if (type.ammoType != -1)
					maxAmmo = min(maxAmmo, (int)(unit.type.storageSpace/Main.ammoMass[type.ammoType]));
				double val = pow(type.getMass(getMaxArc(type)), 0.6)
						*(3+getMaxArc(type)/180)
						*(1+random()*AUTO_RANDOMNESS)
						*pow(1+existingCount, 0.2)
						*1.0/log1p(maxAmmo);
				if (val > highestVal){
					highestVal = val;
					highestType = type;
				}
			}
		}
		return highestType;
	}
}
