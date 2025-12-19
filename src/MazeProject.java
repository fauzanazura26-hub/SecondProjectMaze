/*
 * MazeVisualizer.java (v4.0 - Auto Fit / Responsive)
 * * Features:
 * - Dynamic Scaling: Cells resize automatically to fit the window.
 * (20x20 = Big Cells, 100x100 = Small Cells).
 * - Algorithms: BFS, DFS, Dijkstra, A*.
 * - Logic: Randomized Prim's Maze Generation.
 */

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.*;
import java.util.List;

public class MazeProject extends JFrame {

    // --- Configuration ---
    private int rows = 21; // Default start size
    private int cols = 21;
    private static final int DELAY = 15; // Animation delay

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
    private static final Color COL_VISITED = new Color(255, 255, 0, 150);
    private static final Color COL_PATH = new Color(138, 43, 226);

    // --- Global State ---
    private Node[][] grid;
    private Node startNode;
    private Node endNode;
    private MazePanel mazePanel;
    private JTextArea statsArea;
    private JSlider sizeSlider;
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
        setTitle("Mini Project 2 (MAZE) ");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- Center Panel (The Maze) ---
        // No ScrollPane needed anymore, it will auto-fit
        mazePanel = new MazePanel();
        add(mazePanel, BorderLayout.CENTER);

        // --- Right Panel (Controls) ---
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.EAST);

        // --- Window Setup ---
        setPreferredSize(new Dimension(1000, 700)); // Default Window Size
        pack();
        setLocationRelativeTo(null);

        // Initial Generation
        resizeGrid(rows);

        // Re-render when window is resized by user
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                mazePanel.repaint();
            }
        });
    }

    // --- Inner Classes ---

    private static class Node implements Comparable<Node> {
        int r, c;
        boolean isWall;
        int weight;
        boolean visited;
        boolean isPath;
        Node parent;

        double gCost;
        double hCost;
        double fCost;

        Node(int r, int c) {
            this.r = r;
            this.c = c;
            this.isWall = true;
            this.weight = COST_GRASS;
            this.visited = false;
            this.isPath = false;
            this.gCost = Double.MAX_VALUE;
            this.fCost = Double.MAX_VALUE;
        }

        void resetForPathfinding() {
            this.visited = false;
            this.isPath = false;
            this.parent = null;
            this.gCost = Double.MAX_VALUE;
            this.hCost = 0;
            this.fCost = Double.MAX_VALUE;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.fCost, other.fCost);
        }
    }

    private class MazePanel extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (grid == null) return;

            // --- Auto-Fit Logic ---
            int panelW = getWidth();
            int panelH = getHeight();

            // Calculate largest possible square cell size that fits
            int cellW = panelW / cols;
            int cellH = panelH / rows;
            int cellSize = Math.max(1, Math.min(cellW, cellH)); // Ensure at least 1px

            // Calculate offsets to center the maze
            int totalGridW = cellSize * cols;
            int totalGridH = cellSize * rows;
            int startX = (panelW - totalGridW) / 2;
            int startY = (panelH - totalGridH) / 2;

            // Draw Background
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, panelW, panelH);

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    Node node = grid[r][c];

                    if (node.isWall) g.setColor(COL_WALL);
                    else if (node == startNode) g.setColor(COL_START);
                    else if (node == endNode) g.setColor(COL_GOAL);
                    else if (node.isPath) g.setColor(COL_PATH);
                    else if (node.visited && node != startNode && node != endNode)
                        g.setColor(blend(getTerrainColor(node.weight), COL_VISITED));
                    else g.setColor(getTerrainColor(node.weight));

                    // Draw the cell
                    g.fillRect(startX + c * cellSize, startY + r * cellSize, cellSize, cellSize);

                    // Only draw grid lines if cells are big enough (> 5px)
                    if (cellSize > 5) {
                        g.setColor(new Color(0, 0, 0, 40));
                        g.drawRect(startX + c * cellSize, startY + r * cellSize, cellSize, cellSize);
                    }
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
        panel.setPreferredSize(new Dimension(240, 0));

        JLabel title = new JLabel("Controls");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(15));

        // --- Maze Size Slider ---
        JLabel sizeLabel = new JLabel("Maze Size: " + rows + "x" + cols);
        sizeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(sizeLabel);

        sizeSlider = new JSlider(JSlider.HORIZONTAL, 21, 101, 21);
        sizeSlider.setMajorTickSpacing(20);
        sizeSlider.setMinorTickSpacing(10);
        sizeSlider.setPaintTicks(true);
        sizeSlider.setAlignmentX(Component.CENTER_ALIGNMENT);

        sizeSlider.addChangeListener(e -> {
            if (!sizeSlider.getValueIsAdjusting()) {
                int val = sizeSlider.getValue();
                if (val % 2 == 0) val++; // Ensure odd

                if (val != rows && !isRunning) {
                    resizeGrid(val);
                    sizeLabel.setText("Maze Size: " + rows + "x" + cols);
                } else if (isRunning) {
                    sizeSlider.setValue(rows); // Lock while running
                }
            }
        });
        panel.add(sizeSlider);
        panel.add(Box.createVerticalStrut(15));

        // --- Buttons ---
        JButton btnReset = new JButton("Generate New Maze");
        styleButton(btnReset);
        btnReset.addActionListener(e -> {
            if (!isRunning) generateMaze();
        });
        panel.add(btnReset);
        panel.add(Box.createVerticalStrut(20));

        panel.add(createAlgoButton("Run BFS"));
        panel.add(Box.createVerticalStrut(5));
        panel.add(createAlgoButton("Run DFS"));
        panel.add(Box.createVerticalStrut(5));
        panel.add(createAlgoButton("Run Dijkstra"));
        panel.add(Box.createVerticalStrut(5));
        panel.add(createAlgoButton("Run A* (A-Star)"));

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

    private JButton createAlgoButton(String text) {
        JButton btn = new JButton(text);
        styleButton(btn);
        btn.addActionListener(e -> {
            String algo = text.replace("Run ", "").replace(" (A-Star)", "");
            runAlgorithm(algo);
        });
        return btn;
    }

    private void styleButton(JButton btn) {
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(200, 35));
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

    private void resizeGrid(int size) {
        this.rows = size;
        this.cols = size;
        grid = new Node[rows][cols];
        generateMaze();
    }

    private void generateMaze() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = new Node(r, c);
            }
        }

        Random rand = new Random();
        int startR = 1 + rand.nextInt((rows - 1) / 2) * 2;
        int startC = 1 + rand.nextInt((cols - 1) / 2) * 2;

        Node firstCell = grid[startR][startC];
        firstCell.isWall = false;

        ArrayList<Node> walls = new ArrayList<>();
        addWalls(firstCell, walls);

        while (!walls.isEmpty()) {
            int index = rand.nextInt(walls.size());
            Node wall = walls.remove(index);

            Node[] result = getDividedCells(wall);
            if (result != null) {
                Node visited = result[0];
                Node unvisited = result[1];

                if (unvisited.isWall) {
                    wall.isWall = false;
                    unvisited.isWall = false;
                    addWalls(unvisited, walls);
                }
            }
        }

        assignTerrainAndEndpoints(rand);

        statsArea.setText("Maze Generated.\nSize: " + rows + "x" + cols + "\nSelect an algorithm.");
        mazePanel.repaint();
    }

    private void addWalls(Node cell, ArrayList<Node> wallList) {
        int r = cell.r;
        int c = cell.c;
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int[] d : dirs) {
            int nr = r + d[0];
            int nc = c + d[1];
            if (isValid(nr, nc) && grid[nr][nc].isWall) {
                wallList.add(grid[nr][nc]);
            }
        }
    }

    private Node[] getDividedCells(Node wall) {
        int r = wall.r;
        int c = wall.c;
        if (isValid(r - 1, c) && isValid(r + 1, c)) {
            Node top = grid[r - 1][c];
            Node bottom = grid[r + 1][c];
            if (!top.isWall && bottom.isWall) return new Node[]{top, bottom};
            if (top.isWall && !bottom.isWall) return new Node[]{bottom, top};
        }
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
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!grid[r][c].isWall) {
                    walkable.add(grid[r][c]);
                    int chance = rand.nextInt(100);
                    if (chance < 15) grid[r][c].weight = COST_WATER;
                    else if (chance < 35) grid[r][c].weight = COST_MUD;
                    else grid[r][c].weight = COST_GRASS;
                }
            }
        }

        if (!walkable.isEmpty()) {
            startNode = walkable.get(0);
            endNode = walkable.get(walkable.size() - 1);
            startNode.weight = COST_GRASS;
            endNode.weight = COST_GRASS;
        }
    }

    // --- Pathfinding Execution ---

    private void runAlgorithm(String type) {
        if (isRunning) return;
        isRunning = true;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c].resetForPathfinding();
            }
        }
        mazePanel.repaint();

        Thread algoThread = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            int nodesVisited = 0;
            boolean found = false;

            if (type.equals("BFS")) found = runBFS();
            else if (type.equals("DFS")) found = runDFS();
            else if (type.equals("Dijkstra")) found = runDijkstra();
            else if (type.equals("A*")) found = runAStar();

            long duration = System.currentTimeMillis() - startTime;

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
            }

            for(int r=0; r<rows; r++) {
                for(int c=0; c<cols; c++) {
                    if(grid[r][c].visited) nodesVisited++;
                }
            }

            final int fCost = pathCost;
            final int fNodes = nodesVisited;
            final int fLen = pathLength;
            final boolean fFound = found;

            SwingUtilities.invokeLater(() -> {
                String result = String.format(
                        "Algo: %s\n" +
                                "Size: %dx%d\n" +
                                "Status: %s\n" +
                                "Cost: %d\n" +
                                "Visited: %d\n" +
                                "Len: %d\n" +
                                "Time: %d ms",
                        type, rows, cols, (fFound ? "Found" : "No Path"), fCost, fNodes, fLen, duration
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
        startNode.gCost = 0;
        startNode.fCost = 0;
        pq.add(startNode);

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            if (current.visited) continue;
            current.visited = true;
            visualizeStep();

            if (current == endNode) return true;

            for (Node neighbor : getNeighbors(current)) {
                if (!neighbor.visited && !neighbor.isWall) {
                    double newGCost = current.gCost + neighbor.weight;
                    if (newGCost < neighbor.gCost) {
                        neighbor.gCost = newGCost;
                        neighbor.fCost = newGCost;
                        neighbor.parent = current;
                        pq.add(neighbor);
                    }
                }
            }
        }
        return false;
    }

    private boolean runAStar() {
        PriorityQueue<Node> pq = new PriorityQueue<>();

        startNode.gCost = 0;
        startNode.hCost = heuristic(startNode, endNode);
        startNode.fCost = startNode.gCost + startNode.hCost;

        pq.add(startNode);

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            if (current.visited) continue;
            current.visited = true;
            visualizeStep();

            if (current == endNode) return true;

            for (Node neighbor : getNeighbors(current)) {
                if (!neighbor.visited && !neighbor.isWall) {
                    double newGCost = current.gCost + neighbor.weight;

                    if (newGCost < neighbor.gCost) {
                        neighbor.gCost = newGCost;
                        neighbor.hCost = heuristic(neighbor, endNode);
                        neighbor.fCost = neighbor.gCost + neighbor.hCost;
                        neighbor.parent = current;
                        pq.add(neighbor);
                    }
                }
            }
        }
        return false;
    }

    private double heuristic(Node a, Node b) {
        return Math.abs(a.r - b.r) + Math.abs(a.c - b.c);
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
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    private void visualizeStep() {
        // Faster animation on big grids so it doesn't take forever
        int dynamicDelay = (rows > 50) ? 1 : DELAY;
        try {
            SwingUtilities.invokeAndWait(() -> mazePanel.repaint());
            // Small sleep only if grid is not huge
            if (rows <= 60) {
                Thread.sleep(dynamicDelay);
            }
        } catch (Exception e) {}
    }
}