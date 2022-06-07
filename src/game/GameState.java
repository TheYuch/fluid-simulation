package game;

import fluidsimulation.FluidSimulation;
import fluidsimulation.gridbased.*;
import fluidsimulation.particlebased.*;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;

import java.util.ArrayList;

public class GameState extends MouseAdapter implements MouseMotionListener
{

	public static FluidSimulation simulation;
	
	//interactivity fields
	private static ArrayList<Integer> mouseX;
	private static ArrayList<Integer> mouseY;
	
	public GameState()
	{
		// Eulerian, QuadtreeEulerian, Lagrangian, GridLagrangian TODO: control type with GUI
		simulation = new QuadtreeEulerian(Main.SQRT_PARTICLES_AMOUNT, Main.PARTICLES_AMOUNT);
		
		mouseX = new ArrayList<>();
		mouseY = new ArrayList<>();
	}
	
	public void update()
	{
		for (int i = 0; i < mouseX.size(); i++)
		{
			int x = mouseX.get(i);
			int y = mouseY.get(i);
			simulation.addFluid(x, y);
		}
		mouseX.clear();
		mouseY.clear();
		simulation.update();
	}
	
	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseDragged(MouseEvent e)
	{
		int x = e.getX() + Main.MOUSE_OFFSET.x;
		int y = e.getY() + Main.MOUSE_OFFSET.y;
		mouseX.add(x);
		mouseY.add(y);
	}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseMoved(MouseEvent e) {}
	
}
