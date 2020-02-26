package spaceraze.battlehandler.landbattle;

import java.util.LinkedList;
import java.util.List;

import spaceraze.util.general.Functions;
import spaceraze.util.general.Logger;
import spaceraze.world.enums.LandBattleAttackType;
import spaceraze.world.landbattle.report.LandBattleReport;

public abstract class LandBattleAttack {
	protected LandBattleAttackType attackType;
	private List<TaskForceTroop> targetOpponents;
	protected int resistance; // resistance on the planet where the battle is fought
	
	public LandBattleAttack(LandBattleAttackType anAttackType, List<TaskForceTroop> aTargetOpponents, int aResistance){
		attackType = anAttackType;
		targetOpponents = aTargetOpponents;
		resistance = aResistance;
	}

	public LandBattleAttackType getAttackType(){
		return attackType;
	}
	
	public abstract void performAttack(LandBattleGroup attBG, LandBattleGroup defBG, int attVipBonus, int defVipBonus);
	
	//public abstract void performAttack(LandBattleReport attReport, LandBattleReport defReport, int attVipBonus, int defVipBonus);
	
	protected TaskForceTroop getRandomOpponent(){
		List<TaskForceTroop> okOpponents = getNonDestroyedOpponents();
		TaskForceTroop foundTroop = null;
		if (okOpponents.size() > 0){
			int randomIndex = Functions.getRandomInt(0, okOpponents.size()-1);
			foundTroop = okOpponents.get(randomIndex);
		}
		return foundTroop;
	}
	
	private List<TaskForceTroop> getNonDestroyedOpponents(){
		List<TaskForceTroop> okTroops = new LinkedList<>();
		for (TaskForceTroop aTroop : targetOpponents) {
			if (!aTroop.getTroop().isDestroyed()){
				Logger.finer(aTroop.getTroop().getUniqueShortName() + " not destroyed: " + aTroop.getTroop().getCurrentDC());
				okTroops.add(aTroop);
			}else{
				Logger.finer(aTroop.getTroop().getUniqueShortName() + " destroyed: " + aTroop.getTroop().getCurrentDC());
			}
		}
		Logger.finer("okTroops size: " + okTroops.size());
		return okTroops;
	}
	
	protected String getAsString(){
		String retStr = "";
		retStr = retStr + "attType=" + attackType.toString() + " ";
		retStr = retStr + "res=" + resistance + " ";
		retStr = retStr + "opp=" + resistance + " ";
		for (TaskForceTroop aTroop : targetOpponents) {
			retStr = retStr + aTroop.getTroop().getUniqueShortName() + ";";
		}
		return retStr;
	}
}
