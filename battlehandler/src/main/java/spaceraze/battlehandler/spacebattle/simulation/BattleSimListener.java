package spaceraze.battlehandler.spacebattle.simulation;

/**
 * All classes that use a clientside battle sim must implement this 
 * interface to get information about the simulation.
 * @author WMPABOD
 *
 */
public interface BattleSimListener {
	
	public void battleSimPerformed(BattleSimResult bsr);

	public void battleSimFinished();

}
