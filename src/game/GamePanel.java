package game;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

public class GamePanel extends JPanel {
	
	@Override
	public void paintComponent(Graphics g) {
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, Main.WIN_WIDTH, Main.WIN_HEIGHT);

		GameState.simulation.render(g);
	}

}