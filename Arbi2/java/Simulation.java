public class Simulation {

	public static void main(String[] args) {
		float money = 20000;

		for (int x = 1; x <= 36; x++) {
			money *= 1.05;
			System.out.println("Month: " + x + ": " + money);
		}
	}
}
