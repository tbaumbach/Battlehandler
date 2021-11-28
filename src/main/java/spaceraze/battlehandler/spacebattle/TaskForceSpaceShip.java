package spaceraze.battlehandler.spacebattle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import spaceraze.world.Spaceship;
import spaceraze.world.VIP;

public class TaskForceSpaceShip implements Serializable, Cloneable {
	static final long serialVersionUID = 1L;
	
	final private Spaceship spaceship;
	final private List<VIP> vipOnShip;
	
	public TaskForceSpaceShip(Spaceship spaceship, List<VIP> vipOnShip) {
		this.spaceship = spaceship;
		this.vipOnShip = vipOnShip;
	}

	public Spaceship getSpaceship() {
		return spaceship;
	}

	public List<VIP> getVipOnShip() {
		return vipOnShip != null ? vipOnShip : new ArrayList<>();
	}
}
