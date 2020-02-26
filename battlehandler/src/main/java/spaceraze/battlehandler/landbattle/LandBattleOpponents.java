package spaceraze.battlehandler.landbattle;


public class LandBattleOpponents {
	private TaskForceTroop troop1, troop2;
	
	public LandBattleOpponents(TaskForceTroop aTroop, TaskForceTroop bTroop){
		troop1 = aTroop;
		troop2 = bTroop;
	}
	
	public TaskForceTroop getOpponent(TaskForceTroop aTroop){
		TaskForceTroop otherTroop = troop1;
		if (aTroop == troop1){
			otherTroop = troop2;
		}
		return otherTroop;
	}
	
	public boolean getContain(TaskForceTroop aTroop){
		boolean isTroop = (aTroop == troop1);
		if (!isTroop){
			isTroop = (aTroop == troop2);
		}
		return isTroop;
	}
	
}
