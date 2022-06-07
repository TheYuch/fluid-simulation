package fluidsimulation.gridbased;

import java.util.Random;
import java.awt.Color;
import java.awt.Graphics;

import fluidsimulation.FluidSimulation;
import game.Main;

// Eulerian MAC with Semi-Lagrangian advection scheme
public class Eulerian extends FluidSimulation {

    public static int N = 16; // N for size
    public static int CELL_WIDTH = Main.WIN_WIDTH / N;
    public static int CELL_HEIGHT = Main.WIN_HEIGHT / N;

    private static final int ITERATIONS = 10;
    private static final float DT = 0.5f; // timestep
    private static final float DIFF = 0.00001f;
    private static final float VISC = 0.00001f;

    private float[] s;
    private float[] density; // density of fluid surface (or fluid "dye")

    private float[] Vx;
    private float[] Vy;

    private float[] Vx0; // prev Vx
    private float[] Vy0; // prev Vy

    private Random rand;

    public void addDensity(int x, int y, float amount) {
        this.density[IX(x, y)] += amount;
    }

    public void addVelocity(int x, int y, float amountX, float amountY) {
        int idx = IX(x, y);
        this.Vx[idx] += amountX;
        this.Vy[idx] += amountY;
    }

    private void fillRectangle(int posX, int posY, int width, int height) // TODO: make abstract, add as GUI feature
    {
        for (int y = posY; y < posY + height; y++)
        {
            for (int x = posX; y < posX + width; x++)
            {
                addDensity(x, y, rand.nextFloat() * 10f + 100f);
                addVelocity(x, y, (rand.nextFloat() - 0.5f) * 2f * 10f, (rand.nextFloat() - 0.5f) * 2f * 10f);
            }
        }
    }

    private void fillRandom() // TODO: make abstract, add as GUI feature
    {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                addDensity(j, i, rand.nextFloat() * 200f);
                addVelocity(j, i, (rand.nextFloat() - 0.5f) * 2f * 10f, (rand.nextFloat() - 0.5f) * 2f * 10f);
            }
        }
    }

    @Override
    protected void init(int sqrtParticlesAmount, int particlesAmount) {
        Eulerian.N = sqrtParticlesAmount;
        Eulerian.CELL_WIDTH = Main.WIN_WIDTH / Eulerian.N;
        Eulerian.CELL_HEIGHT = Main.WIN_HEIGHT / Eulerian.N;

        this.s = new float[N * N];
        this.density = new float[N * N];

        this.Vx = new float[N * N];
        this.Vy = new float[N * N];

        this.Vx0 = new float[N * N];
        this.Vy0 = new float[N * N];

        rand = new Random();
        // two fluid bodies colliding
        /*for (int i = 0; i < N / 5; i++) {
            for (int j = 0; j < N / 5; j++) {
                addDensity(j, i, (rand.nextFloat() * 100f));
                addVelocity(j, i, rand.nextFloat() * 3f + 2f, rand.nextFloat() * 3f + 2f);
            }
        }
        for (int i = 3 * N / 4; i < N; i++) {
            for (int j = 3 * N / 4; j < N; j++) {
                addDensity(j, i, rand.nextFloat() * 50f);
                addVelocity(j, i, rand.nextFloat() * -2f - 1f, rand.nextFloat() * -2f - 1f);
            }
        }*/
    }

    public Eulerian(int sqrtParticlesAmount, int particlesAmount) {
        init(sqrtParticlesAmount, particlesAmount);
    }

    @Override
    public void update() {
        /*// TMP CONSTANT FLUID, TODO: make abstract, add as GUI feature
        int x1 = (int)(N * 0.4f);
        int y1 = (int)(N * 0.4f);
        int x2 = (int)(N * 0.6f);
        int y2 = (int)(N * 0.6f);
        addDensity(x1, y1, rand.nextFloat() * 200f + 800f);
        addVelocity(x1, y1, rand.nextFloat() * 0.5f + 1f, rand.nextFloat() * 0.5f + 1f);
        addDensity(x2, y2, rand.nextFloat() * 200f + 650f);
        addVelocity(x2, y2, rand.nextFloat() * -2f - 1f, rand.nextFloat() * -2f - 1f);*/

        // velocity step
        diffuse(1, Vx0, Vx, VISC, DT); // diffuse velocities
        diffuse(2, Vy0, Vy, VISC, DT);

        project(Vx0, Vy0, Vx, Vy); // maintain incompressibility

        advect(1, Vx, Vx0, Vx0, Vy0, DT); // advect velocities
        advect(2, Vy, Vy0, Vx0, Vy0, DT);

        project(Vx, Vy, Vx0, Vy0); // maintain incompressibility

        // density step
        diffuse(0, s, density, DIFF, DT * 10); // diffuse fluid surface "dye"
        advect(0, density, s, Vx, Vy, DT); // advect fluid surface "dye"
    }

    @Override
    public void addFluid(int mouseX, int mouseY) {
        int j = (mouseX) / Eulerian.CELL_WIDTH;
        j = clamp(j, 1, Eulerian.N - 2);
        int i = (mouseY) / Eulerian.CELL_HEIGHT;
        i = clamp(i, 1, Eulerian.N - 2);

        addDensity(i, j, 250f); //rand.nextFloat() * 500f);
        addVelocity(i, j, (rand.nextFloat() - 0.5f) * 2f * 10f, (rand.nextFloat() - 0.5f) * 2f * 10f);
    }

    @Override
    public void render(Graphics g) {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                float dens = density[IX(i, j)];
                if (dens > 0f) {
                    Color color = new Color(Color.HSBtoRGB((dens % 255) / 255f, 0.8f, 1f));
                    g.setColor(color);

                    int x = j * CELL_WIDTH;
                    int y = i * CELL_HEIGHT;
                    g.fillRect(x, y, CELL_WIDTH, CELL_HEIGHT);
                }
            }
        }
    }

    /*--------------------------------**  util  **--------------------------------*/

    private static int clamp(int x, int a, int b) {
        if (x < a) return a;
        if (x > b) return b;
        return x;
    }

    private static int IX(int i, int j) { // 2D indexes -> 1D index (IX)
        i = clamp(i, 0, N - 1);
        j = clamp(j, 0, N - 1);
        return i + (j * N);
    }

    // b = which array x is -> how to set bounding cells
    private static void setBounds(int b, float[] x) { // bound fluid into box, fluid bounces off walls with opposite velocities
        // setting 1 through N - 2, to only set edges and avoid corners
        for (int i = 1; i < N - 1; i++) {
            x[IX(i, 0)] = b == 2 ? -x[IX(i, 1)] : x[IX(i, 1)];
            x[IX(i, N - 1)] = b == 2 ? -x[IX(i, N - 2)] : x[IX(i, N - 2)];
        }
        for (int j = 1; j < N - 1; j++) {
            x[IX(0, j)] = b == 1 ? -x[IX(1, j)] : x[IX(1, j)];
            x[IX(N - 1, j)] = b == 1 ? -x[IX(N - 2, j)] : x[IX(N - 2, j)];
        }

        // corner cells set to average of two edge cell neighbors
        x[IX(0, 0)] = 0.5f * (x[IX(1, 0)] + x[IX(0, 1)]);
        x[IX(0, N - 1)] = 0.5f * (x[IX(1, N - 1)] + x[IX(0, N - 2)]);
        x[IX(N - 1, 0)] = 0.5f * (x[IX(N - 2, 0)] + x[IX(N - 1, 1)]);
        x[IX(N - 1, N - 1)] = 0.5f * (x[IX(N - 2, N - 1)] + x[IX(N - 1, N - 2)]);
    }

    // could use conjugate gradient solver
    private static void linearSolve(int b, float[] x, float[] x0, float a, float c) { // Uses Gauss-Seidel method to solve linear differential equation
        float cRecip = 1.0f / c;
        for (int k = 0; k < ITERATIONS; k++) {
            for (int j = 1; j < N - 1; j++) {
                for (int i = 1; i < N - 1; i++) {
                    x[IX(i, j)] = (x0[IX(i, j)] + a * (
                            x[IX(i + 1, j)] + x[IX(i - 1, j)] + x[IX(i, j + 1)] + x[IX(i, j - 1)]
                        )) * cRecip;
                }
            }

            setBounds(b, x);
        }
    }

    private static void diffuse(int b, float[] x, float[] x0, float diff, float dt) { // spread fluid surface density and fluid velocity
        float a = dt * diff * (N - 2) * (N - 2); // (N - 2)^2 = area of all non-boundary cells
        linearSolve(b, x, x0, a, 1 + 4 * a);
    }

    // could use conjugate gradient solver
    private static void project(float[] vX, float[] vY, float[] p, float[] div) { // maintain fluid incompressibility, i.e. stop fluid box net outflow/inflow
        for (int j = 1; j < N - 1; j++) {
            for (int i = 1; i < N - 1; i++) {
                div[IX(i, j)] = -0.5f *
                    (
                        vX[IX(i + 1, j)] -
                        vX[IX(i - 1, j)] +
                        vY[IX(i, j + 1)] -
                        vY[IX(i, j - 1)]
                    ) / N;
                p[IX(i, j)] = 0;
            }
        }
        setBounds(0, div);
        setBounds(0, p);

        linearSolve(0, p, div, 1, 4);

        for (int j = 1; j < N - 1; j++) {
            for (int i = 1; i < N - 1; i++) {
                vX[IX(i, j)] -= 0.5f * (p[IX(i + 1, j)] - p[IX(i - 1, j)]) * N;
                vY[IX(i, j)] -= 0.5f * (p[IX(i, j + 1)] - p[IX(i, j - 1)]) * N;
            }
        }
        setBounds(1, vX);
        setBounds(2, vY);
    }

    // Semi-Lagrangian advection
    private static void advect(int b, float[] d, float[] d0, float[] vX, float[] vY, float dt) { // use linear backtrace with linear interpolation to apply velocity field to fluid surface density and fluid velocity
        float i0, i1, j0, j1;

        float dtX = dt * (N - 2);
        float dtY = dt * (N - 2);

        float s0, s1, t0, t1;
        float x, y;

        for (int j = 1; j < N - 1; j++) {
            for (int i = 1; i < N - 1; i++) {
                x = i - dtX * vX[IX(i, j)];
                y = j - dtY * vY[IX(i, j)];

                if (x < 0.5f) x = 0.5f;
                if (x > N + 0.5f) x = N + 0.5f;
                i0 = (int) (x); // equivalent to floor(x)
                i1 = i0 + 1.0f;
                if (y < 0.5f) y = 0.5f;
                if (y > N + 0.5f) y = N + 0.5f;
                j0 = (int) (y); // equivalent to floor(y)
                j1 = j0 + 1.0f;

                s1 = x - i0;
                s0 = 1.0f - s1;
                t1 = y - j0;
                t0 = 1.0f - t1;

                // convert floats to ints ('i' last character means int)
                int i0i = (int) (i0);
                int i1i = (int) (i1);
                int j0i = (int) (j0);
                int j1i = (int) (j1);

                d[IX(i, j)] =
                    s0 * (t0 * d0[IX(i0i, j0i)] + t1 * d0[IX(i0i, j1i)]) +
                    s1 * (t0 * d0[IX(i1i, j0i)] + t1 * d0[IX(i1i, j1i)]);
            }
        }

        setBounds(b, d);
    }

}
