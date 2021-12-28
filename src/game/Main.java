package game;

import fluidsimulation.FluidSimulation;
import fluidsimulation.gridbased.*;
import fluidsimulation.particlebased.*;

/*import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JFrame;*/

public class Main
{

	//private static GameState state = new GameState();
	private static FluidSimulation simulation;

	public static final int SQRT_PARTICLES_AMOUNT = 112;
	public static final int PARTICLES_AMOUNT = SQRT_PARTICLES_AMOUNT * SQRT_PARTICLES_AMOUNT;
	private static final int TRIAL_FRAMES = 20;

	public static final int WIN_WIDTH = 512;
	public static final int WIN_HEIGHT = 512;

	/*public static final Point MOUSE_OFFSET = new Point(0, -28);
	private static final Point WIN_SIZE_OFFSET = new Point(0, 28);*/
	
	public static void main(String args[]) {// throws InterruptedException {
		// Eulerian, QuadtreeEulerian, Lagrangian, GridLagrangian
		simulation = new Lagrangian(Main.SQRT_PARTICLES_AMOUNT, Main.PARTICLES_AMOUNT);

		double average = 0;
		for (int i = 0; i < TRIAL_FRAMES; i++) {
			long startTime = System.nanoTime();
			simulation.update();
			long endTime = System.nanoTime();

			long elapsedTime = endTime - startTime;
			average += elapsedTime;
			System.out.println("Frame " + (i + 1) + ": " + elapsedTime);
		}
		average /= TRIAL_FRAMES;
		System.out.println("Average: " + average);

		/*JFrame frame = new JFrame("Fluid Simulation");
		frame.addMouseListener(state);
		frame.addMouseMotionListener(state);

		GamePanel game = new GamePanel();
		frame.add(game);

		frame.setPreferredSize(new Dimension(WIN_WIDTH + WIN_SIZE_OFFSET.x, WIN_HEIGHT + WIN_SIZE_OFFSET.y));
		frame.pack();

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);

		frame.setVisible(true);

		while (true)
		{
			long startTime = System.currentTimeMillis();

			state.update();
			frame.repaint();

			long elapsedTime = System.currentTimeMillis() - startTime;

			long dt = Math.max(0, 1000 / 30 - elapsedTime);
			Thread.sleep(dt);
			// Thread.sleep(500);
		}*/
	}
	
}