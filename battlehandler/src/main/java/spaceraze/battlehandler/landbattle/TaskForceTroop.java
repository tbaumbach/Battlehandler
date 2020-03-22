package spaceraze.battlehandler.landbattle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import spaceraze.world.Troop;
import spaceraze.world.VIP;

public class TaskForceTroop implements Serializable, Cloneable {
	static final long serialVersionUID = 1L;

	private final  Troop troop;
	private final  List<VIP> vipOnTroop;
	
	public TaskForceTroop(Troop troop, List<VIP> vipOnTroop) {
		this.troop = troop;
		this.vipOnTroop = vipOnTroop;
	}

	public Troop getTroop() {
		return troop;
	}

	public List<VIP> getVipOnTroop() {
		return vipOnTroop != null ? vipOnTroop : new ArrayList<>();
	}
}
