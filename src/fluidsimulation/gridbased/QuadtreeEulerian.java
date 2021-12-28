package fluidsimulation.gridbased;

import java.util.Random;
import java.awt.Color;
import java.awt.Graphics;

import fluidsimulation.FluidSimulation;
import util.Quad;
import util.Quad.*;
import game.Main;

public class QuadtreeEulerian extends FluidSimulation {

    private static int N = 16; // N for size

    private static final int ITERATIONS = 10;
    private static final float DT = 0.5f;
    private static final float DIFF = 0.00001f;
    private static final float VISC = 0.00001f;

    private static Quad tree;

    private static int CELL_WIDTH = Main.WIN_WIDTH / (N + 2);
    private static int CELL_HEIGHT = Main.WIN_HEIGHT / (N + 2);
    private static int CELL_OFFSET_X = CELL_WIDTH;
    private static int CELL_OFFSET_Y = CELL_HEIGHT;

    private Random rand;

    private void fillRandom(Quad q) {
        q.refine();
        if (q.isLeaf) {
            q.props[QuadProps.d.value] = rand.nextFloat() * 100f;
            q.props[QuadProps.Vx.value] = (rand.nextFloat() - 0.5f) * 2f * 10f;
            q.props[QuadProps.Vy.value] = (rand.nextFloat() - 0.5f) * 2f * 10f;
        } else {
            for (Quad child : q.children) {
                fillRandom(child);
            }
        }
    }

    @Override
    protected void init(int sqrtParticlesAmount, int particlesAmount) {
        QuadtreeEulerian.N = sqrtParticlesAmount;
        QuadtreeEulerian.CELL_WIDTH = Main.WIN_WIDTH / (QuadtreeEulerian.N + 2);
        QuadtreeEulerian.CELL_HEIGHT = Main.WIN_HEIGHT / (QuadtreeEulerian.N + 2);
        QuadtreeEulerian.CELL_OFFSET_X = QuadtreeEulerian.CELL_WIDTH;
        QuadtreeEulerian.CELL_OFFSET_Y = QuadtreeEulerian.CELL_HEIGHT;

        tree = new Quad(null, 0, 0, N, N);
        Quad.N = N;
        Quad.border = new Quad[4 * N + 4];
        for (int i = 0; i < N; i++) { // N
            Quad.border[i] = new Quad(null, i, -1, i + 1, 0);
        }
        for (int i = 0; i < N; i++) { // S
            Quad.border[i + N] = new Quad(null, i, N, i + 1, N + 1);
        }
        for (int i = 0; i < N; i++) { // W
            Quad.border[i + 2 * N] = new Quad(null, -1, i, 0, i + 1);
        }
        for (int i = 0; i < N; i++) { // E
            Quad.border[i + 3 * N] = new Quad(null, N, i, N + 1, i + 1);
        }
        Quad.border[4 * N] = new Quad(null, -1, -1, 0, 0); // NW
        Quad.border[4 * N + 1] = new Quad(null, N, -1, N + 1, 0); // NE
        Quad.border[4 * N + 2] = new Quad(null, -1, N, 0, N + 1); // SW
        Quad.border[4 * N + 3] = new Quad(null, N, N, N + 1, N + 1); // SE

        rand = new Random();
        fillRandom(tree);
    }

    public QuadtreeEulerian(int sqrtParticlesAmount, int particlesAmount) {
        init(sqrtParticlesAmount, particlesAmount);
    }

    @Override
    public void update() {
        // coarsening TODO - uncomment
        coarsenTree(tree);

        // neighbor finding and refining
        forAll(tree, (Quad q) -> q.findNeighborsAndRefine(true));

        // velocity step
        diffuse(1, QuadProps.Vx0.value, QuadProps.Vx.value, VISC, DT);
        diffuse(2, QuadProps.Vy0.value, QuadProps.Vy.value, VISC, DT);

        project(QuadProps.Vx0.value, QuadProps.Vy0.value, QuadProps.Vx.value, QuadProps.Vy.value);

        advect(1, QuadProps.Vx.value, QuadProps.Vx0.value, QuadProps.Vx0.value, QuadProps.Vy0.value, DT);
        advect(2, QuadProps.Vy.value, QuadProps.Vy0.value, QuadProps.Vx0.value, QuadProps.Vy0.value, DT);

        project(QuadProps.Vx.value, QuadProps.Vy.value, QuadProps.Vx0.value, QuadProps.Vy0.value);

        // density step
        diffuse(0, QuadProps.d0.value, QuadProps.d.value, DIFF, DT);
        advect(0, QuadProps.d.value, QuadProps.d0.value, QuadProps.Vx.value, QuadProps.Vy.value, DT);
    }

    @Override
    public void addFluid(int mouseX, int mouseY) {
        int i = (mouseX - CELL_OFFSET_X) / QuadtreeEulerian.CELL_WIDTH;
        i = clamp(i, 0, QuadtreeEulerian.N);
        int j = (mouseY - CELL_OFFSET_Y) / QuadtreeEulerian.CELL_HEIGHT;
        j = clamp(j, 0, QuadtreeEulerian.N);

        Quad q = tree.search(i, j);
        q.props[QuadProps.d.value] = 250f; //rand.nextFloat() * 500f;
        q.props[QuadProps.Vx.value] = (rand.nextFloat() - 0.5f) * 2f * 10f;
        q.props[QuadProps.Vy.value] = (rand.nextFloat() - 0.5f) * 2f * 10f;
    }

    private void renderQuad(Graphics g, Quad q) {
        if (q.isLeaf) {
            float dens = q.props[QuadProps.d.value];
            if (dens > 0f) {
                Color color = new Color(Color.HSBtoRGB(((dens + 50f) % 255) / 255f, 0.8f, dens > 255f ? 1.0f : (dens / 255f)));
                g.setColor(color);
                g.fillRect(q.iStart * CELL_WIDTH + CELL_OFFSET_X, q.jStart * CELL_HEIGHT + CELL_OFFSET_Y,
                        (q.iEnd - q.iStart) * CELL_WIDTH, (q.jEnd - q.jStart) * CELL_HEIGHT);

                g.setColor(Color.RED);
                g.drawRect(q.iStart * CELL_WIDTH + CELL_OFFSET_X, q.jStart * CELL_HEIGHT + CELL_OFFSET_Y,
                        (q.iEnd - q.iStart) * CELL_WIDTH, (q.jEnd - q.jStart) * CELL_HEIGHT);
            }
        }
    }

    @Override
    public void render(Graphics g) {
        forAll(tree, (Quad q) -> renderQuad(g, q));
        for (int i = 0; i < Quad.border.length; i++) {
            renderQuad(g, Quad.border[i]);
        }
    }


    /*--------------------------------**  util  **--------------------------------*/


    private static int clamp(int x, int a, int b) {
        if (x < a) return a;
        if (x > b) return b;
        return x;
    }

    private static void coarsenTree(Quad q) {
        if (q.isLeaf) return;

        boolean canCoarsen = true;
        for (Quad child : q.children) {
            if (!child.isLeaf) {
                coarsenTree(child);
                canCoarsen = false;
            }
        }
        if (canCoarsen) q.checkAndCoarsen();
    }

    private interface CallableLeaf {
        void f(Quad q);
    }

    private static void forAll(Quad q, CallableLeaf leafInterface) {
        if (q.isLeaf) {
            leafInterface.f(q);
        } else {
            for(Quad child : q.children) {
                forAll(child, leafInterface);
            }
        }
    }

    private static void setBounds(int b, int xIdx) {
        // edge borders
        if (b == 2) {
            for (int i = 0; i < 2 * N; i++) {
                Quad.border[i].props[xIdx] = -Quad.border[i].parent.props[xIdx];
            }
            for (int i = 2 * N; i < 4 * N; i++) {
                Quad.border[i].props[xIdx] = Quad.border[i].parent.props[xIdx];
            }
        } else if (b == 1) {
            for (int i = 0; i < 2 * N; i++) {
                Quad.border[i].props[xIdx] = Quad.border[i].parent.props[xIdx];
            }
            for (int i = 2 * N; i < 4 * N; i++) {
                Quad.border[i].props[xIdx] = -Quad.border[i].parent.props[xIdx];
            }
        } else {
            for (int i = 0; i < 2 * N; i++) {
                Quad.border[i].props[xIdx] = Quad.border[i].parent.props[xIdx];
            }
            for (int i = 2 * N; i < 4 * N; i++) {
                Quad.border[i].props[xIdx] = Quad.border[i].parent.props[xIdx];
            }
        }

        // corner borders
        Quad.border[4 * N].props[xIdx] = 0.5f * (Quad.border[0].props[xIdx] + Quad.border[2 * N].props[xIdx]);
        Quad.border[4 * N + 1].props[xIdx] = 0.5f * (Quad.border[N - 1].props[xIdx] + Quad.border[3 * N].props[xIdx]);
        Quad.border[4 * N + 2].props[xIdx] = 0.5f * (Quad.border[N].props[xIdx] + Quad.border[3 * N - 1].props[xIdx]);
        Quad.border[4 * N + 3].props[xIdx] = 0.5f * (Quad.border[2 * N - 1].props[xIdx] + Quad.border[4 * N - 1].props[xIdx]);
    }

    private static void linearSolve(int b, int xIdx, int x0Idx, float a, float c) {
        float cRecip = 1.0f / c;
        for (int k = 0; k < ITERATIONS; k++) {
            forAll(tree, (Quad q) -> {
                float[][] neighborProps = q.findNeighborProps(new int[] {xIdx});
                float sum = 0f;
                for (float[] props : neighborProps) sum += props[0];
                q.props[xIdx] = (q.props[x0Idx] + a * sum) * cRecip;
            });

            setBounds(b, xIdx);
        }
    }

    private static void diffuse(int b, int xIdx, int x0Idx, float diff, float dt) {
        float a = dt * diff * N * N;
        linearSolve(b, xIdx, x0Idx, a, 1 + 4 * a);
    }

    private static void project(int vXIdx, int vYIdx, int pIdx, int divIdx) {
        forAll(tree, (Quad q) -> {
            float[][] neighborProps = q.findNeighborProps(new int[] {vXIdx, vYIdx});
            q.props[divIdx] = -0.5f * (
                    neighborProps[3][0] - // E
                    neighborProps[2][0] + // W
                    neighborProps[1][1] - // S
                    neighborProps[0][1] // N
                ) / N;
            q.props[pIdx] = 0;
        });
        setBounds(0, divIdx);
        setBounds(0, pIdx);

        linearSolve(0, pIdx, divIdx, 1, 4);

        forAll(tree, (Quad q) -> {
            float[][] neighborProps = q.findNeighborProps(new int[] {pIdx});
            q.props[vXIdx] -= 0.5f * (neighborProps[3][0] - neighborProps[2][0]) * N;
            q.props[vYIdx] -= 0.5f * (neighborProps[1][0] - neighborProps[0][0]) * N;
        });
        setBounds(1, vXIdx);
        setBounds(2, vYIdx);
    }

    private static void advect(int b, int dIdx, int d0Idx, int vXIdx, int vYIdx, float dt) {
        float dtX = dt * N;
        float dtY = dt * N;

        forAll(tree, (Quad q) -> {
            float x, y;
            float i0, i1, j0, j1;
            int i0i, i1i, j0i, j1i;
            float s1, s0, t1, t0;

            float avg = 0f;

            for (int i = q.iStart; i < q.iEnd; i++) {
                for (int j = q.jStart; j < q.jEnd; j++) {
                    x = i - dtX * q.props[vXIdx];
                    y = j - dtY * q.props[vYIdx];

                    if (x < 0.5f) x = 0.5f;
                    if (x > N + 0.5f) x = N + 0.5f;
                    i0 = (int) (x);
                    i1 = i0 + 1.0f;
                    if (y < 0.5f) y = 0.5f;
                    if (y > N + 0.5f) y = N + 0.5f;
                    j0 = (int) (y);
                    j1 = j0 + 1.0f;

                    s1 = x - i0;
                    s0 = 1.0f - s1;
                    t1 = y - j0;
                    t0 = 1.0f - t1;

                    i0i = (int) (i0);
                    i1i = (int) (i1);
                    j0i = (int) (j0);
                    j1i = (int) (j1);

                    avg +=
                            s0 * (t0 * tree.search(i0i, j0i).props[d0Idx] + t1 * tree.search(i0i, j1i).props[d0Idx]) +
                            s1 * (t0 * tree.search(i1i, j0i).props[d0Idx] + t1 * tree.search(i1i, j1i).props[d0Idx]);
                }
            }

            avg /= (q.iEnd - q.iStart) * (q.jEnd - q.jStart);
            q.props[dIdx] = avg;
        });

        setBounds(b, dIdx);
    }

}