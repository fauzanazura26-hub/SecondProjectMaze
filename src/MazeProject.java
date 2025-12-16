/*
 * MazeVisualizer.java
 * * A single-file Java Swing application for Maze Generation and Pathfinding Visualization.
 * * Features:
 * - Generation: Randomized Prim's Algorithm.
 * - Terrain: Grass (Cost 1), Mud (Cost 5), Water (Cost 10).
 * - Pathfinding: BFS, DFS, Dijkstra.
 * - Visualization: Step-by-step animation of exploration and final path.
 * * References/Citations:
 * - Prim, R. C. (1957). "Shortest connection networks and some generalizations".
 * Bell System Technical Journal. (Used for Maze Generation).
 * - Dijkstra, E. W. (1959). "A note on two problems in connexion with graphs".
 * Numerische Mathematik. (Used for Weighted Pathfinding).
 */

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MazeProject extends JFrame {

    // --- Configuration Constants ---
    private static final int ROWS = 41; // Must be odd for Prim's
    private static final int COLS = 41; // Must be odd for Prim's
    private static final int CELL_SIZE = 15;
    private static final int DELAY = 10; // Animation delay in ms

    // --- Terrain Costs ---
    private static final int COST_GRASS = 1;
    private static final int COST_MUD = 5;
    private static final int COST_WATER = 10;

    // --- Colors ---
    private static final Color COL_WALL = new Color(40, 40, 40);
    private static final Color COL_GRASS = new Color(144, 238, 144);
    private static final Color COL_MUD = new Color(205, 133, 63);
    private static final Color COL_WATER = new Color(135, 206, 235);
    private static final Color COL_START = Color.GREEN;
    private static final Color COL_GOAL = Color.RED;
    private static final Color COL_VISITED = new Color(255, 255, 0, 150); // Translucent Yellow
    private static final Color COL_PATH = new Color(138, 43, 226); // Blue Violet

    // --- Global State ---
    private Node[][] grid;
    private Node startNode;
    private Node endNode;
    private MazePanel mazePanel;
    private JTextArea statsArea;
    private boolean isRunning = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new MazeProject().setVisible(true);
        });
    }

    public MazeProject() {
        setTitle("Maze Generation & Pathfinding Visualizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

        // Initialize Grid
        grid = new Node[ROWS][COLS];
        initializeGrid();

        // UI Components
        mazePanel = new MazePanel();
        add(mazePanel, BorderLayout.CENTER);

        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);

        // Generate initial maze
        generateMaze();
    }

    // --- Inner Classes ---

    /**
     * Represents a single cell in the grid.
     */
    private static class Node implements Comparable<Node> {
        int r, c;
        boolean isWall;
        int weight; // 1, 5, or 10
        boolean visited;
        boolean isPath; // For final path
        Node parent;
        double distance; // For Dijkstra

        Node(int r, int c) {
            this.r = r;
            this.c = c;
            this.isWall = true; // Start as wall
            this.weight = COST_GRASS;
            this.visited = false;
            this.isPath = false;
            this.distance = Double.MAX_VALUE;
        }

        void resetForPathfinding() {
            this.visited = false;
            this.isPath = false;
            this.parent = null;
            this.distance = Double.MAX_VALUE;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.distance, other.distance);
        }
    }

    /**
     * Custom JPanel for rendering the grid.
     */
    private class MazePanel extends JPanel {
        public MazePanel() {
            setPreferredSize(new Dimension(COLS * CELL_SIZE, ROWS * CELL_SIZE));
            setBackground(Color.BLACK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    Node node = grid[r][c];

                    // Determine Background Color
                    if (node.isWall) {
                        g.setColor(COL_WALL);
                    } else if (node == startNode) {
                        g.setColor(COL_START);
                    } else if (node == endNode) {
                        g.setColor(COL_GOAL);
                    } else if (node.isPath) {
                        g.setColor(COL_PATH);
                    } else if (node.visited && node != startNode && node != endNode) {
                        // Blend terrain color with visited color
                        g.setColor(blend(getTerrainColor(node.weight), COL_VISITED));
                    } else {
                        g.setColor(getTerrainColor(node.weight));
                    }

                    g.fillRect(c * CELL_SIZE, r * CELL_SIZE, CELL_SIZE, CELL_SIZE);

                    // Optional: Draw grid lines (subtle)
                    g.setColor(new Color(0, 0, 0, 50));
                    g.drawRect(c * CELL_SIZE, r * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }

        private Color getTerrainColor(int weight) {
            switch (weight) {
                case COST_MUD: return COL_MUD;
                case COST_WATER: return COL_WATER;
                default: return COL_GRASS;
            }
        }

        private Color blend(Color c1, Color c2) {
            double ratio = 0.5;
            int r = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
            int g = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
            int b = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
            return new Color(r, g, b);
        }
    }

    // --- UI Setup ---

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setPreferredSize(new Dimension(220, 0));

        JLabel title = new JLabel("Controls");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(15));

        JButton btnReset = new JButton("Generate New Maze");
        styleButton(btnReset);
        btnReset.addActionListener(e -> {
            if (!isRunning) generateMaze();
        });
        panel.add(btnReset);
        panel.add(Box.createVerticalStrut(20));

        JButton btnBFS = new JButton("Run BFS");
        styleButton(btnBFS);
        btnBFS.addActionListener(e -> runAlgorithm("BFS"));
        panel.add(btnBFS);
        panel.add(Box.createVerticalStrut(10));

        JButton btnDFS = new JButton("Run DFS");
        styleButton(btnDFS);
        btnDFS.addActionListener(e -> runAlgorithm("DFS"));
        panel.add(btnDFS);
        panel.add(Box.createVerticalStrut(10));

        JButton btnDijkstra = new JButton("Run Dijkstra");
        styleButton(btnDijkstra);
        btnDijkstra.addActionListener(e -> runAlgorithm("Dijkstra"));
        panel.add(btnDijkstra);

        panel.add(Box.createVerticalStrut(20));

        // Legend
        panel.add(new JLabel("Legend:"));
        panel.add(createLegendLabel("Grass (Cost 1)", COL_GRASS));
        panel.add(createLegendLabel("Mud (Cost 5)", COL_MUD));
        panel.add(createLegendLabel("Water (Cost 10)", COL_WATER));
        panel.add(createLegendLabel("Wall", COL_WALL));
        panel.add(createLegendLabel("Start / Goal", Color.ORANGE));

        panel.add(Box.createVerticalStrut(20));
        statsArea = new JTextArea(10, 15);
        statsArea.setEditable(false);
        statsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        statsArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JScrollPane scroll = new JScrollPane(statsArea);
        panel.add(scroll);

        return panel;
    }

    private void styleButton(JButton btn) {
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(180, 35));
    }

    private JPanel createLegendLabel(String text, Color c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JPanel colorBox = new JPanel();
        colorBox.setPreferredSize(new Dimension(12, 12));
        colorBox.setBackground(c);
        colorBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        p.add(colorBox);
        p.add(new JLabel(text));
        return p;
    }

    // --- Core Logic ---

    private void initializeGrid() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c] = new Node(r, c);
            }
        }
    }

    /**
     * Generates a maze using Randomized Prim's Algorithm.
     */
    private void generateMaze() {
        // Reset Grid
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c] = new Node(r, c); // Re-instantiate to clear walls/weights
            }
        }

        // Prim's Logic
        // 1. Start with a grid full of walls (already done in Node constructor)

        // 2. Pick a random start cell (ensure odd coordinates)
        Random rand = new Random();
        int startR = 1 + rand.nextInt((ROWS - 1) / 2) * 2;
        int startC = 1 + rand.nextInt((COLS - 1) / 2) * 2;

        Node firstCell = grid[startR][startC];
        firstCell.isWall = false;

        ArrayList<Node> walls = new ArrayList<>();
        addWalls(firstCell, walls);

        while (!walls.isEmpty()) {
            // Pick random wall
            int index = rand.nextInt(walls.size());
            Node wall = walls.remove(index);

            // Check neighbors
            Node[] result = getDividedCells(wall);
            if (result != null) {
                Node visited = result[0];
                Node unvisited = result[1];

                if (unvisited.isWall) {
                    // Make the wall a path
                    wall.isWall = false;
                    // Make the unvisited cell a path
                    unvisited.isWall = false;

                    addWalls(unvisited, walls);
                }
            }
        }

        // 3. Assign Terrain Types and Pick Start/End
        assignTerrainAndEndpoints(rand);

        statsArea.setText("Maze Generated.\nSelect an algorithm.");
        mazePanel.repaint();
    }

    private void addWalls(Node cell, ArrayList<Node> wallList) {
        int r = cell.r;
        int c = cell.c;
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // N, S, W, E

        for (int[] d : dirs) {
            int nr = r + d[0];
            int nc = c + d[1];
            if (isValid(nr, nc) && grid[nr][nc].isWall) {
                // Check if this wall isn't already added (simple check, duplicates ok for Prim's but distinct is better)
                wallList.add(grid[nr][nc]);
            }
        }
    }

    private Node[] getDividedCells(Node wall) {
        // A wall connects two cells if it is between them.
        // We look for neighbors at distance 1.
        int r = wall.r;
        int c = wall.c;

        // Vertical check
        if (isValid(r - 1, c) && isValid(r + 1, c)) {
            Node top = grid[r - 1][c];
            Node bottom = grid[r + 1][c];
            if (!top.isWall && bottom.isWall) return new Node[]{top, bottom};
            if (top.isWall && !bottom.isWall) return new Node[]{bottom, top};
        }

        // Horizontal check
        if (isValid(r, c - 1) && isValid(r, c + 1)) {
            Node left = grid[r][c - 1];
            Node right = grid[r][c + 1];
            if (!left.isWall && right.isWall) return new Node[]{left, right};
            if (left.isWall && !right.isWall) return new Node[]{right, left};
        }

        return null;
    }

    private void assignTerrainAndEndpoints(Random rand) {
        List<Node> walkable = new ArrayList<>();
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (!grid[r][c].isWall) {
                    walkable.add(grid[r][c]);
                    // Randomly assign terrain
                    int chance = rand.nextInt(100);
                    if (chance < 15) grid[r][c].weight = COST_WATER;
                    else if (chance < 35) grid[r][c].weight = COST_MUD;
                    else grid[r][c].weight = COST_GRASS;
                }
            }
        }

        // Set Start (Top Left-ish) and End (Bottom Right-ish)
        // Simple search for first and last valid block
        startNode = walkable.get(0);
        endNode = walkable.get(walkable.size() - 1);

        // Ensure start/end are Grass for clarity
        startNode.weight = COST_GRASS;
        endNode.weight = COST_GRASS;
    }

    // --- Pathfinding Execution ---

    private void runAlgorithm(String type) {
        if (isRunning) return;
        isRunning = true;

        // Reset grid stats
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c].resetForPathfinding();
            }
        }
        mazePanel.repaint();

        Thread algoThread = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            int nodesVisited = 0;
            boolean found = false;

            if (type.equals("BFS")) {
                found = runBFS();
            } else if (type.equals("DFS")) {
                found = runDFS();
            } else if (type.equals("Dijkstra")) {
                found = runDijkstra();
            }

            long duration = System.currentTimeMillis() - startTime;

            // Reconstruct Path
            int pathCost = 0;
            int pathLength = 0;

            if (found) {
                Node current = endNode;
                while (current != null) {
                    current.isPath = true;
                    pathCost += current.weight;
                    pathLength++;
                    current = current.parent;
                    mazePanel.repaint();
                    try { Thread.sleep(5); } catch (Exception e) {}
                }
                // Subtract start node cost usually, but we'll include traversal total
            }

            // Count visited
            for(int r=0; r<ROWS; r++) {
                for(int c=0; c<COLS; c++) {
                    if(grid[r][c].visited) nodesVisited++;
                }
            }

            final int fCost = pathCost;
            final int fNodes = nodesVisited;
            final int fLen = pathLength;
            final boolean fFound = found;

            SwingUtilities.invokeLater(() -> {
                String result = String.format(
                        "Algorithm: %s\n" +
                                "Status: %s\n" +
                                "Total Cost: %d\n" +
                                "Visited: %d\n" +
                                "Path Len: %d\n" +
                                "Time: %d ms",
                        type, (fFound ? "Found" : "No Path"), fCost, fNodes, fLen, duration
                );
                statsArea.setText(result);
                isRunning = false;
            });
        });
        algoThread.start();
    }

    // --- Algorithms ---

    private boolean runBFS() {
        Queue<Node> queue = new LinkedList<>();
        startNode.visited = true;
        queue.add(startNode);

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            if (current == endNode) return true;

            for (Node neighbor : getNeighbors(current)) {
                if (!neighbor.visited && !neighbor.isWall) {
                    neighbor.visited = true;
                    neighbor.parent = current;
                    queue.add(neighbor);
                }
            }

            visualizeStep();
        }
        return false;
    }

    private boolean runDFS() {
        Stack<Node> stack = new Stack<>();
        stack.push(startNode);

        // DFS usually marks visited upon popping or pushing.
        // To visualize exploration well, we mark on push but handle duplicates.
        Set<Node> visitedSet = new HashSet<>();

        while (!stack.isEmpty()) {
            Node current = stack.pop();

            if (current == endNode) return true;

            if (!visitedSet.contains(current)) {
                visitedSet.add(current);
                current.visited = true;
                visualizeStep();

                for (Node neighbor : getNeighbors(current)) {
                    if (!visitedSet.contains(neighbor) && !neighbor.isWall) {
                        neighbor.parent = current;
                        stack.push(neighbor);
                    }
                }
            }
        }
        return false;
    }

    private boolean runDijkstra() {
        PriorityQueue<Node> pq = new PriorityQueue<>();
        startNode.distance = 0;
        pq.add(startNode);

        while (!pq.isEmpty()) {
            Node current = pq.poll();

            // If we found a shorter path to this node already processed, skip
            if (current.visited) continue;

            current.visited = true;
            visualizeStep();

            if (current == endNode) return true;

            for (Node neighbor : getNeighbors(current)) {
                if (!neighbor.visited && !neighbor.isWall) {
                    double newDist = current.distance + neighbor.weight;
                    if (newDist < neighbor.distance) {
                        neighbor.distance = newDist;
                        neighbor.parent = current;
                        pq.add(neighbor);
                    }
                }
            }
        }
        return false;
    }

    // --- Helpers ---

    private List<Node> getNeighbors(Node n) {
        List<Node> list = new ArrayList<>();
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] d : dirs) {
            int nr = n.r + d[0];
            int nc = n.c + d[1];
            if (isValid(nr, nc)) {
                list.add(grid[nr][nc]);
            }
        }
        return list;
    }

    private boolean isValid(int r, int c) {
        return r >= 0 && r < ROWS && c >= 0 && c < COLS;
    }

    private void visualizeStep() {
        try {
            SwingUtilities.invokeAndWait(() -> mazePanel.repaint());
            Thread.sleep(DELAY);
        } catch (Exception e) {
            // Ignored
        }
    }
}