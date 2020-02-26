package spaceraze.battlehandler.landbattle.simulation;

/**
 * This class is used to send information about an ongoing simulation to a listener of that simulation
 * @author WMPABOD
 *
 */
public class BattleSimLandResult {
	private double tf1wins;
	private double tf2wins;
	private int iterations;
	private double averageNrRounds;
	
	public int getIterations() {
		return iterations;
	}

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	public BattleSimLandResult(double tf1wins,double tf2wins, double averageNrRounds){
		this.tf1wins = tf1wins;
		this.tf2wins = tf2wins;
		this.averageNrRounds = averageNrRounds;
	}
	
	public double getTf1wins() {
		return tf1wins;
	}
	
	public void setTf1wins(double tf1wins) {
		this.tf1wins = tf1wins;
	}
	
	public double getTf2wins() {
		return tf2wins;
	}
	
	public void setTf2wins(double tf2wins) {
		this.tf2wins = tf2wins;
	}

	public double getAverageNrRounds() {
		return averageNrRounds;
	}

	public void setAverageNrRounds(double averageNrRounds) {
		this.averageNrRounds = averageNrRounds;
	}
}
