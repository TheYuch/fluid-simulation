package fluidsimulation.particlebased;

import fluidsimulation.FluidSimulation;
import game.Main;
import util.Pair;
import util.Vector2;

import java.lang.Math;
import java.util.ArrayList;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics;

public class GridLagrangian extends FluidSimulation
{
	
	//solver parameters
	//private static Vector2 G = new Vector2(0f, 12000 * 9.8f);	//external forces (gravity)
	private static float REST_DENS = 1000f;
	private static float GAS_CONST = 2000f;
	private static float H = 8f;
	private static float HSQ = H * H;
	private static float MASS = 65f;
	private static float VISC = 250f;
	private static float DT = 0.0008f;

	//smoothing kernels defined in Müller and their gradients
	private static float POLY6 = (float)(315f / (65f * Math.PI * Math.pow(H, 9f)));
	private static float SPIKY_GRAD = (float)(-45f / (Math.PI * Math.pow(H, 6f)));
	private static float VISC_LAP = (float)(45f / (Math.PI * Math.pow(H, 6f)));

	//simulation parameters
	private static float EPS = H;
	private static final float BOUND_DAMPING = -0.5f;
	
	//TODO: implement surface tension
	
	//rendering fields
	private static int PARTICLE_DIAMETER = (int)(H);
	private static int PARTICLE_RADIUS = PARTICLE_DIAMETER / 2;
	
	private static class Particle
	{
		
		private Vector2 x;
		private Vector2 v;
		private Vector2 f;
		private float rho;
		private float p;

		public Particle(float posX, float posY, float vX, float vY)
		{
			this.x = new Vector2(posX, posY);
			this.v = new Vector2(vX, vY);
			this.f = new Vector2(0f, 0f);
			this.rho = 0f;
			this.p = 0f;
		}
		
	}

	private Random rand;
	
	private static ArrayList<Integer>[][] grid;
	private static ArrayList<Pair<Particle, Pair<Integer, Integer>>> particles;

	private static void addParticle(float posX, float posY, float vX, float vY)
	{
		Particle particle = new Particle(posX, posY, vX, vY);
		int i = (int)(posY / H) + 1; //+ 1 to account for empty boundary grid cells
		int j = (int)(posX / H) + 1;
		Pair<Integer, Integer> idx = new Pair<Integer, Integer>(i, j);
		particles.add(new Pair<Particle, Pair<Integer, Integer>>(particle, idx));
		grid[i][j].add(particles.size() - 1);
	}

	private static int clamp(int x, int a, int b) {
		if (x < a) return a;
		if (x > b) return b;
		return x;
	}
	
	private static void moveParticle(int idx, Vector2 newPos)
	{
		Pair<Particle, Pair<Integer, Integer>> pair = particles.get(idx);

		grid[pair.S.F][pair.S.S].remove((Integer)(idx));

		int i = (int) (newPos.y / H) + 1;
		i = clamp(i, 0, grid.length);
		int j = (int) (newPos.x / H) + 1;
		j = clamp(j, 0, grid[i].length);
		grid[i][j].add(idx);

		pair.F.x.set(newPos);
		pair.S.F = i;
		pair.S.S = j;
	}
	
	@Override
	protected void init(int sqrtParticlesAmount, int particlesAmount)
	{
		GridLagrangian.H = (float)(Main.WIN_HEIGHT / sqrtParticlesAmount / 2f); //÷ 2f so that particles have space to move in window
		GridLagrangian.HSQ = H * H;
		
		GridLagrangian.POLY6 = (float)(315f / (65f * Math.PI * Math.pow(H, 9f)));
		GridLagrangian.SPIKY_GRAD = (float)(-45f / (Math.PI * Math.pow(H, 6f)));
		GridLagrangian.VISC_LAP = (float)(45f / (Math.PI * Math.pow(H, 6f)));
		
		GridLagrangian.EPS = H;
		
		GridLagrangian.PARTICLE_DIAMETER = (int)(H);
		GridLagrangian.PARTICLE_RADIUS = PARTICLE_DIAMETER / 2;

		int gridHeight = (int)(Main.WIN_HEIGHT / H) + 3; //empty boundary grid cells for efficiency (+2) with +1 = 3 for ceiling
		int gridWidth = (int)(Main.WIN_WIDTH / H) + 3;
		grid = new ArrayList[gridHeight][gridWidth];
		for (int i = 0; i < gridHeight; i++)
		{
			for (int j = 0; j < gridWidth; j++)
			{
				grid[i][j] = new ArrayList<Integer>();
			}
		}
		
		particles = new ArrayList<Pair<Particle, Pair<Integer, Integer>>>();
		
		//filling screen with random fluid
		rand = new Random();
		for (int i = 0; i < particlesAmount; i++)
		{
			addParticle(rand.nextInt(Main.WIN_WIDTH), rand.nextInt(Main.WIN_HEIGHT), (rand.nextFloat() - 0.5f) * 2f * 10f, (rand.nextFloat() - 0.5f) * 2f * 10f);
		}
	}
	
	public GridLagrangian(int sqrtParticlesAmount, int particles)
	{
		init(sqrtParticlesAmount, particles);
	}
	
	@Override
	public void update()
	{
		computeDensityPressure(grid, particles);
		computeForces(grid, particles);
		integrate(particles);
	}
	
	@Override
	public void addFluid(int mouseX, int mouseY)
	{
		addParticle(mouseX, mouseY, (rand.nextFloat() - 0.5f) * 2f * 10f, (rand.nextFloat() - 0.5f) * 2f * 10f);
	}

	@Override
	public void render(Graphics g)
	{
		g.setColor(Color.ORANGE);
		for (Pair<Particle, Pair<Integer, Integer>> pair : particles)
		{
			int x = (int)(pair.F.x.x) - PARTICLE_RADIUS;
			int y = (int)(pair.F.x.y) - PARTICLE_RADIUS;
			g.fillOval(x, y, PARTICLE_DIAMETER, PARTICLE_DIAMETER);
		}
	}

	/*--------------------------------**  util  **--------------------------------*/

	private static void computeDensityPressure(ArrayList<Integer>[][] grid, ArrayList<Pair<Particle, Pair<Integer, Integer>>> particles) 
	{
		for (Pair<Particle, Pair<Integer, Integer>> pair : particles)
		{
			Particle pI = pair.F;
			pI.rho = 0f;
			for (int i = pair.S.F - 1; i < pair.S.F + 2; i++)
			{
				for (int j = pair.S.S - 1; j < pair.S.S + 2; j++)
				{
					for (int idx : grid[i][j])
					{
						Particle pJ = particles.get(idx).F;
						Vector2 rIJ = pJ.x.minus(pI.x);
						float r2SQ = rIJ.dot(rIJ);
						
						if (r2SQ < HSQ)
						{
							pI.rho += MASS * POLY6 * Math.pow(HSQ - r2SQ, 3f);
						}
					}
				}
			}
			pI.p = GAS_CONST * (pI.rho - REST_DENS);
		}
	}
	
	private static void computeForces(ArrayList<Integer>[][] grid, ArrayList<Pair<Particle, Pair<Integer, Integer>>> particles)
	{
		for (int k = 0; k < particles.size(); k++)
		{
			Pair<Particle, Pair<Integer, Integer>> pair = particles.get(k);
			Particle pI = pair.F;
			Vector2 fPress = new Vector2();	//force from pressure
			Vector2 fVisc = new Vector2();	//force from viscosity
			for (int i = pair.S.F - 1; i < pair.S.F + 2; i++)
			{
				for (int j = pair.S.S - 1; j < pair.S.S + 2; j++)
				{
					for (int idx : grid[i][j])
					{
						if (idx == k)
							continue;
						
						Particle pJ = particles.get(idx).F;
						Vector2 rIJ = pJ.x.minus(pI.x);
						float r = rIJ.magnitude();
						
						if (r < H)
						{
							//pressure force contribution
							fPress = fPress.plus(rIJ.normalized().scale(-1f).scale(
									MASS * (pI.p + pJ.p) / (2f * pJ.rho) * SPIKY_GRAD * 
									(float)Math.pow(H - r, 2f)));
							
							//pressure viscosity contribution
							fVisc = fVisc.plus(pJ.v.minus(pI.v).scale(
									VISC * MASS / pJ.rho * VISC_LAP * (H - r)));
						}
					}
				}
			}
			//Vector2 fGrav = G.scale(pI.rho);
			pI.f.set(fPress.plus(fVisc));//.plus(fGrav));
		}			
	}

	private static void integrate(ArrayList<Pair<Particle, Pair<Integer, Integer>>> particles)
	{
		for (int i = 0; i < particles.size(); i++)
		{
			Pair<Particle, Pair<Integer, Integer>> pair = particles.get(i);
			Particle p = pair.F;
			
			//forward Euler integration
			p.v = p.v.plus(p.f.scale(DT / p.rho));
			moveParticle(i, p.x.plus(p.v.scale(DT)));
			
			//enforce boundary conditions
			Vector2 newPos = new Vector2(p.x.x, p.x.y);
			if (p.x.x - EPS < 0f)
			{
				p.v.x *= BOUND_DAMPING;
				newPos.x = EPS;
			}
			else if (p.x.x + EPS > Main.WIN_WIDTH)
			{
				p.v.x *= BOUND_DAMPING;
				newPos.x = Main.WIN_WIDTH - EPS;
			}
			if (p.x.y - EPS < 0f)
			{
				p.v.y *= BOUND_DAMPING;
				newPos.y = EPS;
			}
			else if (p.x.y + EPS > Main.WIN_HEIGHT)
			{
				p.v.y *= BOUND_DAMPING;
				newPos.y = Main.WIN_HEIGHT - EPS;
			}
			moveParticle(i, newPos);
		}
	}
	
}
