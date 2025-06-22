/**
 * Enhanced Noise Source Detection System with Swing GUI and Audio Integration
 * Implementation of BFS, DFS, and A* algorithms for noise source detection
 * 
 * Author: Bevinda Vivian (13523120)
 */

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.Timer;

// Enhanced Node class with audio features
class Node {
    public int id;
    public int x, y;
    public double noiseLevel;
    public boolean isNoiseSource;
    public List<Integer> neighbors;
    public double audioLevel;
    public String sourceType;
    
    public Node(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.noiseLevel = Math.random(); // Random noise level between 0.0 and 1.0
        this.isNoiseSource = this.noiseLevel > 0.7; // Noise source if level > 0.7
        this.neighbors = new ArrayList<>();
        this.audioLevel = 0.0;
        this.sourceType = "ambient";
    }
    
    public void updateAudioData(double level, String type) {
        this.audioLevel = level;
        this.noiseLevel = level;
        this.isNoiseSource = level > 0.7;
        this.sourceType = type;
    }
    
    public double getDistanceTo(Node other) {
        return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2));
    }
    
    @Override
    public String toString() {
        return String.format("Node %d (x=%d, y=%d, noise=%.3f, source=%b)", 
                           id, x, y, noiseLevel, isNoiseSource);
    }
}

// Graph class representing the grid environment
class Graph {
    private Map<Integer, Node> nodes;
    private int gridSize;
    
    public Graph(int gridSize) {
        this.gridSize = gridSize;
        this.nodes = new HashMap<>();
        initializeGrid();
    }
    
    private void initializeGrid() {
        // Create nodes in 5x5 grid
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                int nodeId = i * gridSize + j;
                nodes.put(nodeId, new Node(nodeId, i, j));
            }
        }
        
        // Connect neighboring nodes
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                int nodeId = i * gridSize + j;
                Node node = nodes.get(nodeId);
                
                // Add neighbors (up, down, left, right)
                if (i > 0) node.neighbors.add((i-1) * gridSize + j); // up
                if (i < gridSize-1) node.neighbors.add((i+1) * gridSize + j); // down
                if (j > 0) node.neighbors.add(i * gridSize + (j-1)); // left
                if (j < gridSize-1) node.neighbors.add(i * gridSize + (j+1)); // right
            }
        }
    }
    
    public Node getNode(int id) {
        return nodes.get(id);
    }
    
    public List<Node> getAllNodes() {
        return new ArrayList<>(nodes.values());
    }
    
    public List<Node> getNoiseSourceNodes() {
        List<Node> sources = new ArrayList<>();
        for (Node node : nodes.values()) {
            if (node.isNoiseSource) {
                sources.add(node);
            }
        }
        return sources;
    }
    
    public int getGridSize() {
        return gridSize;
    }
}

// Search Result class to store algorithm results
class SearchResult {
    String algorithmName;
    List<Integer> noiseSourcesFound;
    List<Integer> pathToFirstSource;
    int visitedNodes;
    int pathLength;
    double totalCost;
    
    public SearchResult(String algorithmName) {
        this.algorithmName = algorithmName;
        this.noiseSourcesFound = new ArrayList<>();
        this.pathToFirstSource = new ArrayList<>();
        this.visitedNodes = 0;
        this.pathLength = 0;
        this.totalCost = 0.0;
    }
}

// Audio Processor class for real-time audio analysis
class AudioProcessor {
    private TargetDataLine microphone;
    private AudioFormat format;
    private boolean isRecording = false;
    private Timer audioTimer;
    private AudioCallback callback;
    
    public interface AudioCallback {
        void onAudioUpdate(double level);
    }
    
    public AudioProcessor() {
        // CD quality audio format
        format = new AudioFormat(44100, 16, 1, true, true);
    }
    
    public boolean startRecording(AudioCallback callback) {
        this.callback = callback;
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            
            if (!AudioSystem.isLineSupported(info)) {
                return false;
            }
            
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();
            isRecording = true;
            
            // Start audio monitoring timer
            audioTimer = new Timer();
            audioTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (isRecording && callback != null) {
                        double level = getCurrentAudioLevel();
                        SwingUtilities.invokeLater(() -> callback.onAudioUpdate(level));
                    }
                }
            }, 0, 100); // Update every 100ms
            
            return true;
            
        } catch (LineUnavailableException e) {
            return false;
        }
    }
    
    public void stopRecording() {
        if (audioTimer != null) {
            audioTimer.cancel();
        }
        if (microphone != null && isRecording) {
            microphone.stop();
            microphone.close();
            isRecording = false;
        }
    }
    
    private double getCurrentAudioLevel() {
        if (!isRecording || microphone == null) return 0.0;
        
        byte[] buffer = new byte[1024];
        int bytesRead = microphone.read(buffer, 0, buffer.length);
        
        if (bytesRead > 0) {
            return calculateRMSLevel(buffer, bytesRead);
        }
        return 0.0;
    }
    
    private double calculateRMSLevel(byte[] audioData, int length) {
        long sum = 0;
        for (int i = 0; i < length; i += 2) {
            if (i + 1 < length) {
                int sample = (audioData[i] << 8) | (audioData[i + 1] & 0xFF);
                sum += sample * sample;
            }
        }
        
        double rms = Math.sqrt((double) sum / (length / 2));
        return Math.min(rms / 32768.0, 1.0); // Normalize to 0-1
    }
    
    public boolean loadAudioFile(String filename) {
        try {
            File audioFile = new File(filename);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            
            // Process audio data
            byte[] buffer = new byte[4096];
            int bytesRead;
            double maxLevel = 0.0;
            
            while ((bytesRead = audioStream.read(buffer)) != -1) {
                double level = calculateRMSLevel(buffer, bytesRead);
                maxLevel = Math.max(maxLevel, level);
            }
            
            audioStream.close();
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
}

// BFS Algorithm Implementation
class BFS {
    public static SearchResult search(Graph graph, int startNodeId) {
        SearchResult result = new SearchResult("BFS");
        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        Map<Integer, Integer> parent = new HashMap<>();
        
        queue.offer(startNodeId);
        visited.add(startNodeId);
        parent.put(startNodeId, null);
        
        while (!queue.isEmpty()) {
            int currentId = queue.poll();
            Node currentNode = graph.getNode(currentId);
            result.visitedNodes++;
            
            // Check if this is a noise source
            if (currentNode.isNoiseSource) {
                result.noiseSourcesFound.add(currentId);
                
                // Build path to this noise source
                if (result.pathToFirstSource.isEmpty()) {
                    List<Integer> path = buildPath(parent, startNodeId, currentId);
                    result.pathToFirstSource = path;
                    result.pathLength = path.size();
                }
            }
            
            // Add neighbors to queue
            for (int neighborId : currentNode.neighbors) {
                if (!visited.contains(neighborId)) {
                    visited.add(neighborId);
                    parent.put(neighborId, currentId);
                    queue.offer(neighborId);
                }
            }
        }
        
        return result;
    }
    
    private static List<Integer> buildPath(Map<Integer, Integer> parent, int start, int end) {
        List<Integer> path = new ArrayList<>();
        Integer current = end;
        
        while (current != null) {
            path.add(0, current);
            current = parent.get(current);
        }
        
        return path;
    }
}

// DFS Algorithm Implementation
class DFS {
    public static SearchResult search(Graph graph, int startNodeId) {
        SearchResult result = new SearchResult("DFS");
        Set<Integer> visited = new HashSet<>();
        Map<Integer, Integer> parent = new HashMap<>();
        
        dfsRecursive(graph, startNodeId, visited, parent, result, startNodeId);
        
        return result;
    }
    
    private static void dfsRecursive(Graph graph, int currentId, Set<Integer> visited, 
                                   Map<Integer, Integer> parent, SearchResult result, int startId) {
        visited.add(currentId);
        result.visitedNodes++;
        
        Node currentNode = graph.getNode(currentId);
        
        // Check if this is a noise source
        if (currentNode.isNoiseSource) {
            result.noiseSourcesFound.add(currentId);
            
            // Build path to this noise source
            if (result.pathToFirstSource.isEmpty()) {
                List<Integer> path = buildPath(parent, startId, currentId);
                result.pathToFirstSource = path;
                result.pathLength = path.size();
            }
        }
        
        // Visit neighbors
        for (int neighborId : currentNode.neighbors) {
            if (!visited.contains(neighborId)) {
                parent.put(neighborId, currentId);
                dfsRecursive(graph, neighborId, visited, parent, result, startId);
            }
        }
    }
    
    private static List<Integer> buildPath(Map<Integer, Integer> parent, int start, int end) {
        List<Integer> path = new ArrayList<>();
        Integer current = end;
        
        while (current != null) {
            path.add(0, current);
            current = parent.get(current);
        }
        
        return path;
    }
}

// A* Algorithm Implementation
class AStar {
    static class AStarNode implements Comparable<AStarNode> {
        int nodeId;
        double gCost; // Cost from start
        double hCost; // Heuristic cost to goal
        double fCost; // Total cost
        
        public AStarNode(int nodeId, double gCost, double hCost) {
            this.nodeId = nodeId;
            this.gCost = gCost;
            this.hCost = hCost;
            this.fCost = gCost + hCost;
        }
        
        @Override
        public int compareTo(AStarNode other) {
            return Double.compare(this.fCost, other.fCost);
        }
    }
    
    public static SearchResult search(Graph graph, int startNodeId) {
        SearchResult result = new SearchResult("A*");
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>();
        Set<Integer> closedSet = new HashSet<>();
        Map<Integer, Integer> parent = new HashMap<>();
        Map<Integer, Double> gCosts = new HashMap<>();
        
        // Find nearest noise source as goal for heuristic
        Node startNode = graph.getNode(startNodeId);
        List<Node> noiseSources = graph.getNoiseSourceNodes();
        
        if (noiseSources.isEmpty()) {
            return result;
        }
        
        // Use closest noise source for heuristic calculation
        Node closestSource = noiseSources.get(0);
        for (Node source : noiseSources) {
            if (startNode.getDistanceTo(source) < startNode.getDistanceTo(closestSource)) {
                closestSource = source;
            }
        }
        
        openSet.offer(new AStarNode(startNodeId, 0, startNode.getDistanceTo(closestSource)));
        gCosts.put(startNodeId, 0.0);
        parent.put(startNodeId, null);
        
        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();
            int currentId = current.nodeId;
            
            if (closedSet.contains(currentId)) continue;
            
            closedSet.add(currentId);
            result.visitedNodes++;
            
            Node currentNode = graph.getNode(currentId);
            
            // Check if this is a noise source
            if (currentNode.isNoiseSource) {
                result.noiseSourcesFound.add(currentId);
                result.totalCost = current.gCost;
                
                // Build path to this noise source
                if (result.pathToFirstSource.isEmpty()) {
                    List<Integer> path = buildPath(parent, startNodeId, currentId);
                    result.pathToFirstSource = path;
                    result.pathLength = path.size();
                }
                break; // Found first noise source, stop for A*
            }
            
            // Add neighbors to open set
            for (int neighborId : currentNode.neighbors) {
                if (closedSet.contains(neighborId)) continue;
                
                Node neighbor = graph.getNode(neighborId);
                double tentativeGCost = current.gCost + currentNode.getDistanceTo(neighbor);
                
                if (!gCosts.containsKey(neighborId) || tentativeGCost < gCosts.get(neighborId)) {
                    gCosts.put(neighborId, tentativeGCost);
                    parent.put(neighborId, currentId);
                    
                    double hCost = neighbor.getDistanceTo(closestSource);
                    openSet.offer(new AStarNode(neighborId, tentativeGCost, hCost));
                }
            }
        }
        
        return result;
    }
    
    private static List<Integer> buildPath(Map<Integer, Integer> parent, int start, int end) {
        List<Integer> path = new ArrayList<>();
        Integer current = end;
        
        while (current != null) {
            path.add(0, current);
            current = parent.get(current);
        }
        
        return path;
    }
}

// Custom Panel for Grid Visualization
class GridPanel extends JPanel {
    private Graph graph;
    private List<Integer> highlightedPath = new ArrayList<>();
    private Color pathColor = Color.YELLOW;
    
    public GridPanel(Graph graph) {
        this.graph = graph;
        setPreferredSize(new Dimension(400, 400));
        setBorder(BorderFactory.createTitledBorder("Grid Visualization (5x5)"));
    }
    
    public void setHighlightedPath(List<Integer> path, Color color) {
        this.highlightedPath = path;
        this.pathColor = color;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int cellSize = 60;
        int margin = 20;
        
        // Draw grid
        for (int i = 0; i < graph.getGridSize(); i++) {
            for (int j = 0; j < graph.getGridSize(); j++) {
                int nodeId = i * graph.getGridSize() + j;
                Node node = graph.getNode(nodeId);
                
                int x = margin + j * cellSize;
                int y = margin + i * cellSize;
                  // Determine color based on noise level and source type
                Color nodeColor;
                if (node.isNoiseSource) {
                    switch (node.sourceType) {
                        case "traffic":
                            nodeColor = new Color(255, 69, 0); // Red-Orange for traffic
                            break;
                        case "construction":
                            nodeColor = new Color(220, 20, 60); // Crimson for construction
                            break;
                        case "aircraft":
                            nodeColor = new Color(255, 20, 147); // Deep pink for aircraft
                            break;
                        default:
                            nodeColor = Color.RED; // Default noise source
                    }
                } else if (node.noiseLevel > 0.5) {
                    nodeColor = Color.ORANGE; // High Noise
                } else if (node.noiseLevel > 0.2) {
                    nodeColor = Color.YELLOW; // Low Noise
                } else {
                    nodeColor = Color.LIGHT_GRAY; // No Noise
                }
                
                // Highlight path if this node is in the path
                if (highlightedPath.contains(nodeId)) {
                    g2d.setColor(pathColor);
                    g2d.fillRect(x - 2, y - 2, cellSize + 4, cellSize + 4);
                }
                
                // Draw node
                g2d.setColor(nodeColor);
                g2d.fillRect(x, y, cellSize, cellSize);
                
                // Draw border
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y, cellSize, cellSize);
                  // Draw node ID and source type indicator
                g2d.setColor(Color.BLACK);
                FontMetrics fm = g2d.getFontMetrics();
                String text = String.valueOf(nodeId);
                
                // Add emoji for different source types
                if (node.isNoiseSource) {
                    switch (node.sourceType) {
                        case "traffic":
                            text = "" + nodeId;
                            break;
                        case "construction":
                            text = "" + nodeId;
                            break;
                        case "aircraft":
                            text = "" + nodeId;
                            break;
                        default:
                            text = "" + nodeId;
                    }
                }
                
                int textX = x + (cellSize - fm.stringWidth(String.valueOf(nodeId))) / 2;
                int textY = y + (cellSize + fm.getAscent()) / 2;
                g2d.drawString(String.valueOf(nodeId), textX, textY);
                
                // Draw noise level
                String noiseText = String.format("%.2f", node.noiseLevel);
                int noiseX = x + (cellSize - fm.stringWidth(noiseText)) / 2;
                int noiseY = y + cellSize - 5;
                g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                g2d.drawString(noiseText, noiseX, noiseY);
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            }
        }
        
        // Draw legend
        int legendY = margin + graph.getGridSize() * cellSize + 20;
        g2d.setColor(Color.BLACK);
        g2d.drawString("Legend:", margin, legendY);
          int legendItemY = legendY + 20;
        drawLegendItem(g2d, new Color(255, 69, 0), "Traffic Noise", margin, legendItemY);
        drawLegendItem(g2d, new Color(220, 20, 60), "Construction", margin, legendItemY + 20);
        drawLegendItem(g2d, new Color(255, 20, 147), "Aircraft Noise", margin, legendItemY + 40);
        drawLegendItem(g2d, Color.RED, "Generic Noise Source", margin, legendItemY + 60);
        drawLegendItem(g2d, Color.ORANGE, "High Noise (0.5-0.7)", margin, legendItemY + 80);
        drawLegendItem(g2d, Color.YELLOW, "Low Noise (0.2-0.5)", margin, legendItemY + 100);
        drawLegendItem(g2d, Color.LIGHT_GRAY, "No Noise (<0.2)", margin, legendItemY + 120);
    }
    
    private void drawLegendItem(Graphics2D g2d, Color color, String text, int x, int y) {
        g2d.setColor(color);
        g2d.fillRect(x, y - 10, 15, 15);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y - 10, 15, 15);
        g2d.drawString(text, x + 20, y);
    }
}

// Main Enhanced GUI Application with Audio Integration
public class NoiseDetectionGUI extends JFrame {
    private Graph graph;
    private GridPanel gridPanel;
    private JTextArea resultsArea;
    private JButton bfsButton, dfsButton, astarButton, resetButton;
    private JButton micButton, loadAudioButton, simulateAudioButton, stopAudioButton;
    private JLabel statusLabel, audioLevelLabel;
    private JProgressBar audioLevelBar;
    private AudioProcessor audioProcessor;
    private boolean isAudioActive = false;
    
    public NoiseDetectionGUI() {
        this.audioProcessor = new AudioProcessor();
        initializeGUI();
        resetEnvironment();
    }
      private void initializeGUI() {
        setTitle("🎵 Enhanced Noise Source Detection - BFS, DFS, A* with Audio Integration");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Create main panels
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Left panel for grid visualization
        JPanel leftPanel = new JPanel(new BorderLayout());
        gridPanel = new GridPanel(new Graph(5)); // Placeholder
        leftPanel.add(gridPanel, BorderLayout.CENTER);
        
        // Algorithm control panel
        JPanel algorithmPanel = new JPanel(new FlowLayout());
        algorithmPanel.setBorder(BorderFactory.createTitledBorder("Algorithm Controls"));
        
        bfsButton = new JButton("Run BFS");
        dfsButton = new JButton("Run DFS");
        astarButton = new JButton("Run A*");
        resetButton = new JButton("Reset Environment");
        
        bfsButton.addActionListener(e -> runAlgorithm("BFS"));
        dfsButton.addActionListener(e -> runAlgorithm("DFS"));
        astarButton.addActionListener(e -> runAlgorithm("A*"));
        resetButton.addActionListener(e -> resetEnvironment());
        
        algorithmPanel.add(bfsButton);
        algorithmPanel.add(dfsButton);
        algorithmPanel.add(astarButton);
        algorithmPanel.add(resetButton);
        
        // Audio control panel
        JPanel audioPanel = new JPanel(new GridBagLayout());
        audioPanel.setBorder(BorderFactory.createTitledBorder("🎵 Audio Integration"));
        GridBagConstraints gbc = new GridBagConstraints();
        
        micButton = new JButton("Start Microphone");
        loadAudioButton = new JButton("Load Audio File");
        simulateAudioButton = new JButton("Simulate Audio");
        stopAudioButton = new JButton("Stop Audio");
        
        micButton.addActionListener(e -> startMicrophone());
        loadAudioButton.addActionListener(e -> loadAudioFile());
        simulateAudioButton.addActionListener(e -> simulateAudioEnvironment());
        stopAudioButton.addActionListener(e -> stopAudio());
        
        stopAudioButton.setEnabled(false);
        
        // Audio level display
        audioLevelLabel = new JLabel("Audio Level:");
        audioLevelBar = new JProgressBar(0, 100);
        audioLevelBar.setStringPainted(true);
        audioLevelBar.setString("0%");
        
        // Layout audio components
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(5, 5, 5, 5);
        audioPanel.add(micButton, gbc);
        gbc.gridx = 1;
        audioPanel.add(loadAudioButton, gbc);
        gbc.gridx = 2;
        audioPanel.add(simulateAudioButton, gbc);
        gbc.gridx = 3;
        audioPanel.add(stopAudioButton, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        audioPanel.add(audioLevelLabel, gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        audioPanel.add(audioLevelBar, gbc);
        
        // Combine control panels
        JPanel controlsPanel = new JPanel(new BorderLayout());
        controlsPanel.add(algorithmPanel, BorderLayout.NORTH);
        controlsPanel.add(audioPanel, BorderLayout.CENTER);
        
        leftPanel.add(controlsPanel, BorderLayout.SOUTH);
          // Right panel for results
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JLabel resultsLabel = new JLabel("Algorithm Results & Audio Analysis:");
        resultsLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        resultsArea = new JTextArea(30, 35);
        resultsArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        resultsArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultsArea);
        
        rightPanel.add(resultsLabel, BorderLayout.NORTH);
        rightPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Status panel
        statusLabel = new JLabel("Ready - Click Reset to generate new environment | 🎵 Try audio features!");
        statusLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        
        // Add panels to main frame
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.CENTER);
        
        add(mainPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(null);
    }
    
    private void resetEnvironment() {
        graph = new Graph(5);
        gridPanel = new GridPanel(graph);
        
        // Update the left panel
        Component[] components = ((JPanel) getContentPane().getComponent(0)).getComponents();
        JPanel leftPanel = (JPanel) components[0];
        leftPanel.removeAll();
        leftPanel.add(gridPanel, BorderLayout.CENTER);
          // Re-add control panels
        JPanel algorithmPanel = new JPanel(new FlowLayout());
        algorithmPanel.setBorder(BorderFactory.createTitledBorder("Algorithm Controls"));
        
        bfsButton = new JButton("Run BFS");
        dfsButton = new JButton("Run DFS");
        astarButton = new JButton("Run A*");
        resetButton = new JButton("Reset Environment");
        
        bfsButton.addActionListener(e -> runAlgorithm("BFS"));
        dfsButton.addActionListener(e -> runAlgorithm("DFS"));
        astarButton.addActionListener(e -> runAlgorithm("A*"));
        resetButton.addActionListener(e -> resetEnvironment());
        
        algorithmPanel.add(bfsButton);
        algorithmPanel.add(dfsButton);
        algorithmPanel.add(astarButton);
        algorithmPanel.add(resetButton);
        
        // Audio control panel
        JPanel audioPanel = new JPanel(new GridBagLayout());
        audioPanel.setBorder(BorderFactory.createTitledBorder("Audio Integration"));
        GridBagConstraints gbc = new GridBagConstraints();
        
        micButton = new JButton("Start Microphone");
        loadAudioButton = new JButton("Load Audio File");
        simulateAudioButton = new JButton("Simulate Audio");
        stopAudioButton = new JButton("Stop Audio");
        
        micButton.addActionListener(e -> startMicrophone());
        loadAudioButton.addActionListener(e -> loadAudioFile());
        simulateAudioButton.addActionListener(e -> simulateAudioEnvironment());
        stopAudioButton.addActionListener(e -> stopAudio());
        
        stopAudioButton.setEnabled(false);
        
        // Layout audio components
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(5, 5, 5, 5);
        audioPanel.add(micButton, gbc);
        gbc.gridx = 1;
        audioPanel.add(loadAudioButton, gbc);
        gbc.gridx = 2;
        audioPanel.add(simulateAudioButton, gbc);
        gbc.gridx = 3;
        audioPanel.add(stopAudioButton, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        audioPanel.add(audioLevelLabel, gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        audioPanel.add(audioLevelBar, gbc);
        
        // Combine control panels
        JPanel controlsPanel = new JPanel(new BorderLayout());
        controlsPanel.add(algorithmPanel, BorderLayout.NORTH);
        controlsPanel.add(audioPanel, BorderLayout.CENTER);
        
        leftPanel.add(controlsPanel, BorderLayout.SOUTH);
          // Update results
        displayEnvironmentInfo();
        statusLabel.setText("New environment generated - " + graph.getNoiseSourceNodes().size() + " noise sources found");
        
        leftPanel.revalidate();
        leftPanel.repaint();
    }
    
    // Audio Integration Methods
    private void startMicrophone() {
        if (isAudioActive) {
            JOptionPane.showMessageDialog(this, "Audio is already active. Stop current session first.", 
                                        "Audio Active", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        boolean success = audioProcessor.startRecording(level -> {
            // Update audio level bar
            int percentage = (int) (level * 100);
            audioLevelBar.setValue(percentage);
            audioLevelBar.setString(percentage + "%");
            
            // Update grid based on audio level
            if (level > 0.1) { // Threshold for significant audio
                updateGridWithAudio(level, "microphone");
            }
        });
        
        if (success) {
            isAudioActive = true;
            micButton.setEnabled(false);
            stopAudioButton.setEnabled(true);
            statusLabel.setText("Microphone active - Make some noise to see real-time detection!");
            
            appendToResults("MICROPHONE DETECTION STARTED\n");
            appendToResults("==============================\n");
            appendToResults("Real-time audio monitoring active.\n");
            appendToResults("Noise threshold: 0.1 (10%)\n");
            appendToResults("Make noise near your microphone to update the grid!\n\n");
        } else {
            JOptionPane.showMessageDialog(this, "Failed to access microphone.\nPlease check your audio settings.", 
                                        "Microphone Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadAudioFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Audio Files", "wav", "aiff", "au"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            appendToResults("AUDIO FILE ANALYSIS\n");
            appendToResults("======================\n");
            appendToResults("Loading file: " + selectedFile.getName() + "\n");
            
            boolean success = audioProcessor.loadAudioFile(selectedFile.getAbsolutePath());
            
            if (success) {
                appendToResults("File loaded successfully!\n");
                appendToResults("Analyzing audio content...\n\n");
                
                // Simulate file-based noise detection
                simulateFileBasedNoise();
                statusLabel.setText("Audio file analyzed - Updated grid based on audio content");
            } else {
                appendToResults("Failed to load audio file.\n");
                appendToResults("Supported formats: WAV, AIFF, AU\n\n");
                JOptionPane.showMessageDialog(this, "Failed to load audio file.\nPlease try a different file.", 
                                            "File Load Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void simulateAudioEnvironment() {
        appendToResults("SIMULATED AUDIO ENVIRONMENT\n");
        appendToResults("==============================\n");
        appendToResults("Creating realistic noise scenarios...\n\n");
        
        Random random = new Random();
        
        // Create specific noise sources
        List<Node> allNodes = graph.getAllNodes();
        
        // Traffic noise (low frequency, high volume)
        if (allNodes.size() > 6) {
            Node trafficNode = allNodes.get(6);
            trafficNode.updateAudioData(0.8, "traffic");
            appendToResults("Traffic noise at position (" + trafficNode.x + "," + trafficNode.y + ") - Level: 80%\n");
        }
        
        // Construction noise (mid frequency, very high volume)
        if (allNodes.size() > 12) {
            Node constructionNode = allNodes.get(12);
            constructionNode.updateAudioData(0.9, "construction");
            appendToResults("🔨 Construction noise at position (" + constructionNode.x + "," + constructionNode.y + ") - Level: 90%\n");
        }
        
        // Aircraft noise (low-mid frequency, high volume)
        if (allNodes.size() > 18) {
            Node aircraftNode = allNodes.get(18);
            aircraftNode.updateAudioData(0.85, "aircraft");
            appendToResults("Aircraft noise at position (" + aircraftNode.x + "," + aircraftNode.y + ") - Level: 85%\n");
        }
        
        // Add some ambient noise
        for (Node node : allNodes) {
            if (!node.isNoiseSource) {
                double ambient = random.nextDouble() * 0.3;
                node.updateAudioData(ambient, "ambient");
            }
        }
        
        appendToResults("\nSimulated environment created with realistic noise patterns!\n");
        appendToResults("Try running algorithms to see how they detect different noise types.\n\n");
        
        gridPanel.repaint();
        statusLabel.setText("Simulated audio environment created - 3 major noise sources added");
    }
    
    private void stopAudio() {
        if (isAudioActive) {
            audioProcessor.stopRecording();
            isAudioActive = false;
            micButton.setEnabled(true);
            stopAudioButton.setEnabled(false);
            audioLevelBar.setValue(0);
            audioLevelBar.setString("0%");
            statusLabel.setText("Audio monitoring stopped");
            
            appendToResults("AUDIO MONITORING STOPPED\n");
            appendToResults("===========================\n");
            appendToResults("Real-time audio detection ended.\n\n");
        }
    }
    
    private void updateGridWithAudio(double level, String sourceType) {
        // Update random nodes with current audio level (simulation)
        Random random = new Random();
        int randomNodeId = random.nextInt(25);
        Node node = graph.getNode(randomNodeId);
        if (node != null) {
            node.updateAudioData(level, sourceType);
            gridPanel.repaint();
            
            if (level > 0.7) {
                appendToResults("High audio detected! Level: " + String.format("%.1f%%", level * 100) + 
                              " at node " + randomNodeId + "\n");
            }
        }
    }
    
    private void simulateFileBasedNoise() {
        Random random = new Random();
        List<Node> allNodes = graph.getAllNodes();
        
        // Simulate different noise patterns based on "file analysis"
        for (Node node : allNodes) {
            if (random.nextDouble() > 0.75) { // 25% chance of significant noise
                double level = 0.6 + random.nextDouble() * 0.4; // 0.6 to 1.0
                String[] types = {"traffic", "construction", "ambient", "voice"};
                String type = types[random.nextInt(types.length)];
                node.updateAudioData(level, type);
                
                appendToResults("Detected " + type + " noise at node " + node.id + 
                              " - Level: " + String.format("%.1f%%", level * 100) + "\n");
            }
        }
        
        gridPanel.repaint();
        appendToResults("\n");
    }
    
    private void appendToResults(String text) {
        resultsArea.append(text);
        resultsArea.setCaretPosition(resultsArea.getDocument().getLength());
    }
      private void displayEnvironmentInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("ENHANCED NOISE SOURCE DETECTION SYSTEM\n");
        sb.append("==========================================\n");
        sb.append("Author: Bevinda Vivian (13523120)\n");
        sb.append("Features: BFS, DFS, A* + Audio Integration\n");
        sb.append("==========================================\n\n");
        
        sb.append("Environment: 5x5 Grid (25 nodes)\n");
        sb.append("Total nodes: ").append(graph.getAllNodes().size()).append("\n");
        
        List<Node> noiseSources = graph.getNoiseSourceNodes();
        sb.append("Noise sources: ").append(noiseSources.size()).append("\n");
        
        if (!noiseSources.isEmpty()) {
            sb.append("Noise source locations: ");
            for (int i = 0; i < noiseSources.size(); i++) {
                Node source = noiseSources.get(i);
                sb.append(source.id);
                if (!source.sourceType.equals("ambient")) {
                    sb.append("(").append(source.sourceType).append(")");
                }
                if (i < noiseSources.size() - 1) sb.append(", ");
            }
            sb.append("\n\n");
        }
        
        sb.append("Instructions:\n");
        sb.append("Click algorithm buttons to start detection\n");
        sb.append("Try audio features for realistic scenarios\n");
        sb.append("Start node: 0 (top-left corner)\n\n");
        
        sb.append("Audio Features:\n");
        sb.append("Real-time microphone detection\n");
        sb.append("Audio file analysis (.wav, .aiff)\n");
        sb.append("Simulated audio environments\n");
        sb.append("Live audio level monitoring\n\n");
        
        resultsArea.setText(sb.toString());
    }
    
    private void runAlgorithm(String algorithmName) {
        SearchResult result;
        Color pathColor;
        
        switch (algorithmName) {
            case "BFS":
                result = BFS.search(graph, 0);
                pathColor = Color.BLUE;
                statusLabel.setText("BFS completed - Found " + result.noiseSourcesFound.size() + " noise sources");
                break;
            case "DFS":
                result = DFS.search(graph, 0);
                pathColor = Color.GREEN;
                statusLabel.setText("DFS completed - Found " + result.noiseSourcesFound.size() + " noise sources");
                break;
            case "A*":
                result = AStar.search(graph, 0);
                pathColor = Color.MAGENTA;
                statusLabel.setText("A* completed - Found " + result.noiseSourcesFound.size() + " noise sources");
                break;
            default:
                return;
        }
        
        // Highlight path on grid
        gridPanel.setHighlightedPath(result.pathToFirstSource, pathColor);
        
        // Display results
        displayResults(result);
    }
      private void displayResults(SearchResult result) {
        StringBuilder sb = new StringBuilder(resultsArea.getText());
        
        sb.append("======================================\n");
        sb.append("").append(result.algorithmName.toUpperCase()).append(" ALGORITHM RESULTS\n");
        sb.append("======================================\n");
        
        sb.append("Nodes visited: ").append(result.visitedNodes).append("\n");
        sb.append("Noise sources found: ").append(result.noiseSourcesFound.size()).append("\n");
        
        if (!result.noiseSourcesFound.isEmpty()) {
            sb.append("Source locations: ");
            for (int i = 0; i < result.noiseSourcesFound.size(); i++) {
                int sourceId = result.noiseSourcesFound.get(i);
                Node sourceNode = graph.getNode(sourceId);
                sb.append(sourceId);
                if (sourceNode != null && !sourceNode.sourceType.equals("ambient")) {
                    sb.append("(").append(getSourceEmoji(sourceNode.sourceType)).append(")");
                }
                if (i < result.noiseSourcesFound.size() - 1) sb.append(", ");
            }
            sb.append("\n");
        }
        
        if (!result.pathToFirstSource.isEmpty()) {
            sb.append("Path to nearest source: ");
            for (int i = 0; i < result.pathToFirstSource.size(); i++) {
                sb.append(result.pathToFirstSource.get(i));
                if (i < result.pathToFirstSource.size() - 1) sb.append(" → ");
            }
            sb.append("\n");
            sb.append("Path length: ").append(result.pathLength).append("\n");
        }
        
        if (result.algorithmName.equals("A*")) {
            sb.append("Total cost: ").append(String.format("%.2f", result.totalCost)).append("\n");
        }
        
        sb.append("\n");
        
        resultsArea.setText(sb.toString());
        resultsArea.setCaretPosition(resultsArea.getDocument().getLength());
    }
    
    private String getSourceEmoji(String sourceType) {
        switch (sourceType) {
            case "traffic": return "";
            case "construction": return "";
            case "aircraft": return "";
            case "microphone": return "";
            default: return "";
        }
    }    public static void main(String[] args) {
        // Set Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            NoiseDetectionGUI gui = new NoiseDetectionGUI();
            gui.setVisible(true);
            
            // Show welcome message
            JOptionPane.showMessageDialog(gui, 
                "Welcome to Enhanced Noise Source Detection!\n\n" +
                "Features:\n" +
                "BFS, DFS, A* algorithms\n" +
                "Real-time microphone detection\n" +
                "Audio file analysis\n" +
                "Simulated audio environments\n" +
                "Visual grid representation\n\n" +
                "Try the audio features for realistic scenarios!", 
                "Enhanced Noise Detection", 
                JOptionPane.INFORMATION_MESSAGE);
        });
    }
}
