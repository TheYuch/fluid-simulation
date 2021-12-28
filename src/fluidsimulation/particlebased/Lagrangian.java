package fluidsimulation.particlebased;

import fluidsimulation.FluidSimulation;
import game.Main;
import util.Vector2;

import java.lang.Math;
import java.util.ArrayList;
import java.util.Random;
import java.awt.Color;
import java.awt.Graphics;

public class Lagrangian extends FluidSimulation
{
	
	//solver parameters
	//private static Vector2 G = new Vector2(0f, 12000 * 9.8f);	//external forces (gravity)
	private static float REST_DENS = 1000f;						//rest density
	//TODO: (below) ideal gas law only poorly enforces incompressibility
	private static float GAS_CONST = 2000f;						//constant for equation of state
	private static float H = 8f;								//kernel radius, particle diameter, max collision detection distance between particles
	private static float HSQ = H * H;							//H^2 for optimization
	private static float MASS = 65f;							//same mass of all particles
	private static float VISC = 250f;							//viscosity constant
	private static float DT = 0.0008f;							//integration timestep

	//smoothing kernels defined in Müller and their gradients
	private static float POLY6 = (float)(315f / (65f * Math.PI * Math.pow(H, 9f)));
	private static float SPIKY_GRAD = (float)(-45f / (Math.PI * Math.pow(H, 6f)));
	private static float VISC_LAP = (float)(45f / (Math.PI * Math.pow(H, 6f)));

	//simulation parameters
	private static float EPS = H;						//boundary epsilon
	private static final float BOUND_DAMPING = -0.5f;	//must be negative for particles to reflect away from boundaries
	
	//TODO: implement surface tension
	
	//rendering fields
	private static int PARTICLE_DIAMETER = (int)(H);
	private static int PARTICLE_RADIUS = PARTICLE_DIAMETER / 2;
	
	private static class Particle
	{
		
		private Vector2 x; //position
		private Vector2 v;	//velocity
		private Vector2 f;	//force
		private float rho; //density
		private float p;   //pressure

		public Particle(float posX, float posY, float vX, float vY)
		{
			this.x = new Vector2(posX, posY);
			this.v = new Vector2(vX, vY);
			this.f = new Vector2(0f, 0f);
			this.rho = 0f;
			this.p = 0f;
		}
		
	}
	
	private static ArrayList<Particle> particles;

	private Random rand;

	private static void addParticle(float posX, float posY, float vX, float vY)
	{
		particles.add(new Particle(posX, posY, vX, vY));
	}
	
	@Override
	protected void init(int sqrtParticlesAmount, int particlesAmount)
	{
		Lagrangian.H = (float)(Main.WIN_HEIGHT / sqrtParticlesAmount / 2f); //÷ 2f so that particles have space to move in window
		Lagrangian.HSQ = H * H;
		
		Lagrangian.POLY6 = (float)(315f / (65f * Math.PI * Math.pow(H, 9f)));
		Lagrangian.SPIKY_GRAD = (float)(-45f / (Math.PI * Math.pow(H, 6f)));
		Lagrangian.VISC_LAP = (float)(45f / (Math.PI * Math.pow(H, 6f)));
		
		Lagrangian.EPS = H;
		
		Lagrangian.PARTICLE_DIAMETER = (int)(H);
		Lagrangian.PARTICLE_RADIUS = PARTICLE_DIAMETER / 2;
		
		particles = new ArrayList<Particle>();
		
		//filling screen with random fluid
		rand = new Random();
		for (int i = 0; i < particlesAmount; i++)
		{
			addParticle(rand.nextInt(Main.WIN_WIDTH), rand.nextInt(Main.WIN_HEIGHT), (rand.nextFloat() - 0.5f) * 2f * 10f, (rand.nextFloat() - 0.5f) * 2f * 10f);
		}
	}
	
	public Lagrangian(int sqrtParticlesAmount, int particles)
	{
		init(sqrtParticlesAmount, particles);
	}
	
	@Override
	public void update()
	{
		computeDensityPressure(particles);
		computeForces(particles);
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
		for (Particle p : particles)
		{
			int x = (int)(p.x.x) - PARTICLE_RADIUS;
			int y = (int)(p.x.y) - PARTICLE_RADIUS;
			g.fillOval(x, y, PARTICLE_DIAMETER, PARTICLE_DIAMETER);
		}
	}

	/*--------------------------------**  util  **--------------------------------*/

	//Computes densities and uses that to compute pressure
	//O(n^2), sum up densities within H radius of current particle, use ideal gas law for equation of state for pressure -> summed density	
	private static void computeDensityPressure(ArrayList<Particle> particles) 
	{
		for (Particle pI : particles)
		{
			pI.rho = 0f;
			for (Particle pJ : particles)
			{
				Vector2 rIJ = pJ.x.minus(pI.x);
				float r2SQ = rIJ.dot(rIJ);
				
				if (r2SQ < HSQ)
				{
					pI.rho += MASS * POLY6 * Math.pow(HSQ - r2SQ, 3f);
				}
			}
			pI.p = GAS_CONST * (pI.rho - REST_DENS);
		}
	}
	
	//Computes forces on each particle through density and pressure
	private static void computeForces(ArrayList<Particle> particles)
	{
		for (Particle pI : particles)
		{
			Vector2 fPress = new Vector2();	//force from pressure
			Vector2 fVisc = new Vector2();	//force from viscosity
			for (Particle pJ : particles)
			{
				if (pI == pJ)
					continue;
				
				Vector2 rIJ = pJ.x.minus(pI.x);
				float r = rIJ.magnitude();
				
				if (r < H)
				{
					//pressure force contribution
					fPress = fPress.plus(rIJ.normalized().scale(-1f).scale(
							MASS * (pI.p + pJ.p) / (2f * pJ.rho) * SPIKY_GRAD * 
							(float)Math.pow(H - r, 2f)));
					//above equivalent to: fPress += -rIJ.normalized() * MASS * (pI.p + pJ.p) /
					//(2f * pJ.rho) * SPIKY_GRAD * Math.pow(H - r, 2f);
					
					//pressure viscosity contribution
					fVisc = fVisc.plus(pJ.v.minus(pI.v).scale(
							VISC * MASS / pJ.rho * VISC_LAP * (H - r)));
					//above equivalent to: fVisc += VISC * MASS * (pJ.v - pI.v) / 
					//pJ.rho * VISC_LAP * (H - r);
				}
			}
			//Vector2 fGrav = G.scale(pI.rho);
			pI.f.set(fPress.plus(fVisc)); //.plus(fGrav));
		}			
	}

	//Calculates acceleration and updates particle positions
	//TODO: could use leapfrog method implicit integrator (more stable for oscillatory systems)
	private static void integrate(ArrayList<Particle> particles)
	{
		for (Particle p : particles)
		{
			//forward Euler integration
			p.v = p.v.plus(p.f.scale(DT / p.rho));
			p.x = p.x.plus(p.v.scale(DT));
			
			//enforce boundary conditions
			if (p.x.x - EPS < 0f)
			{
				p.v.x *= BOUND_DAMPING;
				p.x.x = EPS;
			}
			else if (p.x.x + EPS > Main.WIN_WIDTH)
			{
				p.v.x *= BOUND_DAMPING;
				p.x.x = Main.WIN_WIDTH - EPS;
			}
			if (p.x.y - EPS < 0f)
			{
				p.v.y *= BOUND_DAMPING;
				p.x.y = EPS;
			}
			else if (p.x.y + EPS > Main.WIN_HEIGHT)
			{
				p.v.y *= BOUND_DAMPING;
				p.x.y = Main.WIN_HEIGHT - EPS;
			}
		}
	}
	
}
