package ru.litun.unitingtwist;

import android.opengl.Matrix;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by Litun on 07.04.2016.
 */
public class FieldGraph implements Drawable {
    int gn;
    private GraphGameHexagon[][] points;
    private GraphGameHexagon center;
    private List<GraphGameHexagon> endpoints = new ArrayList<>();
    private List<GraphGameHexagon> opened = new ArrayList<>();

    public static final float SCALE = 0.1f;

    public FieldGraph(int n) {
        gn = n * 2 - 1;
        points = new GraphGameHexagon[gn][gn];
        for (int i = 0; i < gn; i++)
            for (int j = 0; j < gn; j++) {
                Point point = new Point(((i - n + 1) - (j - n + 1) * 0.5f) * SCALE,
                        (j - n + 1) * 0.866f * SCALE, 0f);
                points[i][j] = new GraphGameHexagon(i, j, point);
            }
        center = points[n - 1][n - 1];
    }

    public List<GraphGameHexagon> getEndpoints() {
        return endpoints;
    }

    public List<GraphGameHexagon> getOpened() {
        return opened;
    }

    public GraphGameHexagon getCenter() {
        return center;
    }

    public void put(GraphGameHexagon hexagon) {
        resetVisits();
        //find 3
        List<GraphGameHexagon> remove = new LinkedList<>();
        final int color = hexagon.getHexagon().getColor();
        final Queue<GraphGameHexagon> queue = new LinkedList<>();
        queue.add(hexagon);
        while (queue.size() > 0) {
            GraphGameHexagon hex = queue.poll();
            hex.setVisited(true);
            remove.add(hex);
            iterateNear(hex, new Action() {
                @Override
                public void act(GraphGameHexagon h) {
                    if (h.hasHexagon() && !h.isVisited() && h.getHexagon().getColor() == color)
                        queue.add(h);
                }
            });
        }
        Log.v("3", String.valueOf(remove.size()));

        //TODO: cut
        boolean cut = true;

        //recount opened and endpoints
        if (cut) totalRecount();
        //TODO: recount opened and endpoint
    }

    public void totalRecount() {
        resetVisits();

        opened.clear();
        endpoints.clear();

        Set<GraphGameHexagon> newOpened = new TreeSet<>();
        final Set<GraphGameHexagon> newEndpoints = new TreeSet<>();

        final Queue<GraphGameHexagon> queue = new LinkedList<>();
        queue.add(center);
        while (queue.size() > 0) {
            GraphGameHexagon point = queue.poll();
            point.setVisited(true);
            if (!point.hasHexagon()) {
                opened.add(point);
                continue;
            }
            iterateNearNotVisited(point, new Action() {
                @Override
                public void act(GraphGameHexagon h) {
                    queue.add(h);
                }
            });
        }

        for (GraphGameHexagon hexagon : opened)
            iterateNear(hexagon, new Action() {
                @Override
                public void act(GraphGameHexagon h) {
                    if (h.hasHexagon()) newEndpoints.add(h);
                }
            });
        endpoints.addAll(newEndpoints);
        resetVisits();
    }

    void resetVisits() {
        iteratePoints(new Action() {
            @Override
            public void act(GraphGameHexagon h) {
                h.setVisited(false);
            }
        });
    }

    private final float[] rotationMatrix = new float[16];

    public void rotate(float angle) {
        Matrix.setRotateM(rotationMatrix, 0, /*mAngle*/ angle, 0, 0, 1.0f);
        for (int i = 0; i < gn; i++)
            for (int j = 0; j < gn; j++) {
                GraphGameHexagon po = points[i][j];
                Point p = po.getPoint();
                float[] point = new float[4];
                float[] previousPoint = new float[]{p.getDefaultX(), p.getDefaultY(), p.getDefaultZ(), 1};
                Matrix.multiplyMV(point, 0, rotationMatrix, 0, previousPoint, 0);
                //Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0);
                p.setX(point[0]);
                p.setY(point[1]);

                if (po.hasHexagon()) {
                    GameHexagon hexagon = po.getHexagon();
                    hexagon.setAngle(angle);
                }
            }
    }

    private ThreeListener listener;

    int[][] edges = new int[][]{
            new int[]{1, 0},
            new int[]{-1, 0},
            new int[]{0, 1},
            new int[]{0, -1},
            new int[]{1, 1},
            new int[]{-1, -1}
    };

    public void set3Listener(ThreeListener listener) {
        this.listener = listener;
    }

    @Override
    public void update(long deltaTime) {

    }

    @Override
    public void draw(final float[] mvpMatrix) {
        iteratePoints(new Action() {
            @Override
            public void act(GraphGameHexagon hexagon) {
                hexagon.draw(mvpMatrix);
            }
        });
    }

    private void iteratePoints(Action action) {
        for (int i = 0; i < gn; i++)
            for (int j = 0; j < gn; j++)
                action.act(points[i][j]);
    }

    private void iterateNear(GraphGameHexagon hexagon, Action action) {
        int i = hexagon.getI();
        int j = hexagon.getJ();
        for (int[] edge : edges) {
            int newI = i + edge[0];
            int newJ = j + edge[1];
            if (newI >= 0 && newJ >= 0 && newI < gn && newJ < gn)
                action.act(points[newI][newJ]);
        }
    }

    private void iterateNearNotVisited(GraphGameHexagon hexagon, final Action action) {
        iterateNear(hexagon, new Action() {
            @Override
            public void act(GraphGameHexagon h) {
                if (!h.isVisited()) action.act(h);
            }
        });
    }

    interface Action {
        void act(GraphGameHexagon h);
    }

    public interface ThreeListener {
        void on3found(List<GraphGameHexagon> points);
    }
}
