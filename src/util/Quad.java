package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class Quad {

    public enum QuadProps {
        d(0), d0(1), Vx(2), Vy(3), Vx0(4), Vy0(5);

        public final int value;

        private QuadProps(int value) {
            this.value = value;
        }
    }

    public static Quad[] border; // must be initialized when quad head is created by external class
    public static int N;
    /*
	Border:
	- [0, N - 1] = N
	- [N, 2N - 1] = s
	- [2N, 3N - 1] = W
	- [3N, 4N - 1] = E
	- 4N = NW
	- 4N + 1 = NE
	- 4N + 2 = SW
	- 4N + 3 = SE
	 */

    // order: north, south, west, east
    private static final int[] A = {2, 0, 1, 0};
    private static final int[] B = {3, 1, 3, 2};
    private static final int[] C = {0, 2, 0, 1};
    private static final int[] D = {1, 3, 2, 3};

    private static final float COARSEN_MAX_DENSITY_DIF = 0.4f;
    private static final float REFINE_MIN_DENSITY_DIF = 40f;

    // quadtree information
    public Quad parent = null;
    public Quad[] children; // NW, NE, SW, SE
    public ArrayList<Quad>[] allNeighbors;
    public boolean isLeaf = true;

    // NOTE: i is index of x position, j is index of y position
    // actual range is [jStart, jEnd - 1] and [iStart, iEnd - 1]
    public int iStart;
    public int jStart;
    public int iEnd;
    public int jEnd;

    // node information
    public float[] props = {0, 0, 0, 0, 0, 0}; // d, d0, Vx, Vy, Vx0, Vy0

    public Quad(Quad parent, int iStart, int jStart, int iEnd, int jEnd) {
        this.parent = parent;
        if (iStart < iEnd - 1) this.children = new Quad[4];
        this.allNeighbors = new ArrayList[A.length];
        this.iStart = iStart;
        this.jStart = jStart;
        this.iEnd = iEnd;
        this.jEnd = jEnd;
    }

    public Quad(Quad parent, int iStart, int jStart, int iEnd, int jEnd, float d, float Vx, float Vy) {
        this.parent = parent;
        if (iStart < iEnd - 1) this.children = new Quad[4];
        this.allNeighbors = new ArrayList[A.length];
        this.iStart = iStart;
        this.jStart = jStart;
        this.iEnd = iEnd;
        this.jEnd = jEnd;

        this.props[QuadProps.d.value] = d;
        this.props[QuadProps.Vx.value] = Vx;
        this.props[QuadProps.Vy.value] = Vy;
    }

    public boolean refine() { // returns false if failed to refine (already at unit area)
        if (iStart == iEnd - 1) return false;
        int midI = (iStart + iEnd) / 2;
        int midJ = (jStart + jEnd) / 2;
        float d = props[QuadProps.d.value];
        float Vx = props[QuadProps.Vx.value];
        float Vy = props[QuadProps.Vy.value];
        children[0] = new Quad(this, iStart, jStart, midI, midJ, d, Vx, Vy);
        children[1] = new Quad(this, midI, jStart, iEnd, midJ, d, Vx, Vy);
        children[2] = new Quad(this, iStart, midJ, midI, jEnd, d, Vx, Vy);
        children[3] = new Quad(this, midI, midJ, iEnd, jEnd, d, Vx, Vy);

        Arrays.fill(props, 0);
        isLeaf = false;
        return true;
    }

    private void coarsen() {
        for (Quad q : children) {
            for (int i = 0; i < props.length; i++) {
                props[i] += q.props[i];
            }
        }
        for (int i = 0; i < props.length; i++) {
            props[i] /= children.length;
        }

        Arrays.fill(children, null);
        isLeaf = true;
    }

    public void checkAndCoarsen() {
        float minD = 1000000000;
        float maxD = -1000000000;
        for (Quad q : children) {
            float d = q.props[QuadProps.d.value];
            minD = Math.min(d, minD);
            maxD = Math.max(d, maxD);
        }
        if (maxD - minD <= COARSEN_MAX_DENSITY_DIF) {
            coarsen();
        }
    }

    private ArrayList<Quad> getAdjacentBorderQuads(Quad q, int a, int b) {
        ArrayList<Quad> quads = new ArrayList<>();
        int start = 0, end = 0, offset = 0;
        if (a == 2 && b == 3) { // N
            start = q.iStart;
            end = q.iEnd;
            offset = 0;
        } else if (a == 0 && b == 1) { // S
            start = q.iStart;
            end = q.iEnd;
            offset = N;
        } else if (a == 1 && b == 3) { // W
            start = q.jStart;
            end = q.jEnd;
            offset = 2 * N;
        } else if (a == 0 && b == 2) { // E
            start = q.jStart;
            end = q.jEnd;
            offset = 3 * N;
        }
        for (int i = start; i < end; i++) {
            int idx = i + offset;
            quads.add(border[idx]);
            border[idx].parent = q;
        }

        return quads;
    }

    private Quad findGENeighbor(int a, int b, int c, int d) // c and d of direction side, a and b of opposite side
    {
        if (parent == null) return null;
        if (this == parent.children[a]) return parent.children[c];
        if (this == parent.children[b]) return parent.children[d];

        Quad node = parent.findGENeighbor(a, b, c, d);
        if (node == null || node.isLeaf) return node;

        if (this == parent.children[c]) return node.children[a];
        else return node.children[b];
    }

    private ArrayList<Quad> findLNeighbors(Quad neighbor, int a, int b) {
        Queue<Quad> candidates = new LinkedList<>();
        ArrayList<Quad> neighbors = new ArrayList<>();
        if (neighbor == null) {
            neighbors.addAll(getAdjacentBorderQuads(this, a, b));
            return neighbors;
        }
        candidates.add(neighbor);

        while (!candidates.isEmpty()) {
            Quad candidate = candidates.remove();
            if (candidate.isLeaf) neighbors.add(candidate);
            else {
                candidates.add(candidate.children[a]);
                candidates.add(candidate.children[b]);
            }
        }

        return neighbors;
    }

    public void findNeighborsAndRefine(boolean canRefine) {
        if (canRefine) {
            float minD = 1000000000;
            float maxD = -1000000000;
            for (int i = 0; i < A.length; i++) {
                Quad neighbor = findGENeighbor(A[i], B[i], C[i], D[i]);
                allNeighbors[i] = findLNeighbors(neighbor, A[i], B[i]);

                for (Quad q : allNeighbors[i]) {
                    float d = q.props[QuadProps.d.value];
                    minD = Math.min(minD, d);
                    maxD = Math.max(maxD, d);
                }
            }
            if (maxD - minD >= REFINE_MIN_DENSITY_DIF) {
                if (refine()) {
                    for (Quad q : children) {
                        q.findNeighborsAndRefine(false);
                    }
                }
            }
        } else {
            for (int i = 0; i < A.length; i++) {
                Quad neighbor = findGENeighbor(A[i], B[i], C[i], D[i]);
                allNeighbors[i] = findLNeighbors(neighbor, A[i], B[i]);
            }
        }
    }

    public float[][] findNeighborProps(int[] propsIdx) {
        float[][] props = new float[4][propsIdx.length];
        for (int i = 0; i < A.length; i++) {
            for (Quad q : allNeighbors[i]) {
                for (int j = 0; j < propsIdx.length; j++) {
                    props[i][j] += q.props[propsIdx[j]];
                }
            }
            int count = allNeighbors[i].size();
            for (int j = 0; j < propsIdx.length; j++) {
                props[i][j] /= count;
            }
        }
        return props;
    }

    public Quad search(int i, int j) {
        if (isLeaf) return this;

        int midI = (iStart + iEnd) / 2;
        int midJ = (jStart + jEnd) / 2;
        if (j < midJ) {
            if (i < midI) {
                return children[0].search(i, j);
            } else {
                return children[1].search(i, j);
            }
        } else {
            if (i < midI) {
                return children[2].search(i, j);
            } else {
                return children[3].search(i, j);
            }
        }
    }

}