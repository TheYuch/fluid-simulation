package fluidsimulation;

import java.awt.Graphics;

public abstract class FluidSimulation {

    protected abstract void init(int sqrtParticlesAmount, int particlesAmount);

    public abstract void update();

    public abstract void addFluid(int mouseX, int mouseY);

    public abstract void render(Graphics g);
}
