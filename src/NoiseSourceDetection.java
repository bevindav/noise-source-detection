/**
 * Noise Source Detection System
 * Implementation of BFS, DFS, and A* algorithms for noise source detection
 * 
 * Author: Bevinda Vivian (13523120)
 */

import java.util.*;

// Node class representing a point in the grid
class Node {
    public int id;
    public int x, y;
    public double noiseLevel;
    public boolean isNoiseSource;
    public List<Integer> neighbors;
    
    public Node(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.noiseLevel = Math.random(); // Random noise level between 0.0 and 1.0
        this.isNoiseSource = this.noiseLevel > 0.7; // Noise source if level > 0.7
        this.neighbors = new ArrayList<>();
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
    
    public void printGrid() {
        System.out.println("\nGrid Visualization (5x5):");
        System.out.println("Legend: [N] = Noise Source, [*] = High Noise, [.] = Low Noise, [ ] = No Noise\n");
        
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                int nodeId = i * gridSize + j;
                Node node = nodes.get(nodeId);
                
                if (node.isNoiseSource) {
                    System.out.print("[N] ");
                } else if (node.noiseLevel > 0.5) {
                    System.out.print("[*] ");
                } else if (node.noiseLevel > 0.2) {
                    System.out.print("[.] ");
                } else {
                    System.out.print("[ ] ");
                }
            }
            System.out.println();
        }
        System.out.println();
    }
}

// BFS Algorithm Implementation
class BFS {
    public static SearchResult search(Graph graph, int startNodeId) {
        System.out.println("1. BREADTH-FIRST SEARCH (BFS)");
        
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
                System.out.printf("BFS: Found noise source at node %d with noise level %.4f%n", 
                                currentId, currentNode.noiseLevel);
                
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
        System.out.println("\n2. DEPTH-FIRST SEARCH (DFS)");
        
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
            System.out.printf("DFS: Found noise source at node %d with noise level %.4f%n", 
                            currentId, currentNode.noiseLevel);
            
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
        System.out.println("\n3. A* ALGORITHM");
        
        SearchResult result = new SearchResult("A*");
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>();
        Set<Integer> closedSet = new HashSet<>();
        Map<Integer, Integer> parent = new HashMap<>();
        Map<Integer, Double> gCosts = new HashMap<>();
        
        // Find nearest noise source as goal for heuristic
        Node startNode = graph.getNode(startNodeId);
        List<Node> noiseSources = graph.getNoiseSourceNodes();
        
        if (noiseSources.isEmpty()) {
            System.out.println("A*: No noise sources found in the environment");
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
                System.out.printf("A*: Found noise source at node %d with noise level %.4f (cost: %.2f)%n", 
                                currentId, currentNode.noiseLevel, current.gCost);
                
                // Build path to this noise source
                if (result.pathToFirstSource.isEmpty()) {
                    List<Integer> path = buildPath(parent, startNodeId, currentId);
                    result.pathToFirstSource = path;
                    result.pathLength = path.size();
                }
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
    
    public void printResults() {
        System.out.printf("\n%s Algorithm Results:%n", algorithmName);
        System.out.printf("Found %d noise sources: %s%n", noiseSourcesFound.size(), noiseSourcesFound);
        
        if (!pathToFirstSource.isEmpty()) {
            System.out.print("Path to nearest noise source: ");
            for (int i = 0; i < pathToFirstSource.size(); i++) {
                System.out.print(pathToFirstSource.get(i));
                if (i < pathToFirstSource.size() - 1) System.out.print(" -> ");
            }
            System.out.println();
        }
        
        System.out.printf("%s Stats: Visited=%d, PathLength=%d, SourcesFound=%d", 
                         algorithmName, visitedNodes, pathLength, noiseSourcesFound.size());
        
        if (algorithmName.equals("A*")) {
            System.out.printf(", TotalCost=%.2f", totalCost);
        }
        System.out.println();
    }
}

// Main class - Noise Source Detection System
public class NoiseSourceDetection {
    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("NOISE SOURCE DETECTION SYSTEM");
        System.out.println("Using BFS, DFS, and A* Graph Traversal Algorithms");
        System.out.println("Author: Bevinda Vivian (13523120)");
        System.out.println("======================================================================\n");
        
        // Initialize 5x5 grid environment
        System.out.println("Initializing 5x5 grid environment...");
        Graph graph = new Graph(5);
        
        System.out.printf("Environment created with %d nodes%n", graph.getAllNodes().size());
        
        List<Node> noiseSources = graph.getNoiseSourceNodes();
        List<Integer> sourceIds = new ArrayList<>();
        for (Node source : noiseSources) {
            sourceIds.add(source.id);
        }
        System.out.printf("Noise sources at nodes: %s%n", sourceIds);
        
        // Display grid visualization
        graph.printGrid();
        
        // Starting detection from node 0 (top-left corner)
        int startNode = 0;
        System.out.printf("Starting detection from node %d%n", startNode);
        System.out.println("======================================================================\n");
        
        // Run BFS Algorithm
        SearchResult bfsResult = BFS.search(graph, startNode);
        bfsResult.printResults();
        
        System.out.println("\n======================================================================");
        
        // Run DFS Algorithm
        SearchResult dfsResult = DFS.search(graph, startNode);
        dfsResult.printResults();
        
        System.out.println("\n======================================================================");
        
        // Run A* Algorithm
        SearchResult astarResult = AStar.search(graph, startNode);
        astarResult.printResults();
        
        System.out.println("\n======================================================================");
        
        // Algorithm Comparison
        System.out.println("\nALGORITHM COMPARISON SUMMARY");
        System.out.println("======================================================================");
        System.out.printf("%-12s | %-8s | %-10s | %-12s | %-8s%n", 
                         "Algorithm", "Visited", "Path Len", "Sources Found", "Cost");
        System.out.println("----------------------------------------------------------------------");
        System.out.printf("%-12s | %-8d | %-10d | %-12d | %-8s%n", 
                         "BFS", bfsResult.visitedNodes, bfsResult.pathLength, 
                         bfsResult.noiseSourcesFound.size(), "N/A");
        System.out.printf("%-12s | %-8d | %-10d | %-12d | %-8s%n", 
                         "DFS", dfsResult.visitedNodes, dfsResult.pathLength, 
                         dfsResult.noiseSourcesFound.size(), "N/A");
        System.out.printf("%-12s | %-8d | %-10d | %-12d | %-8.2f%n", 
                         "A*", astarResult.visitedNodes, astarResult.pathLength, 
                         astarResult.noiseSourcesFound.size(), astarResult.totalCost);
        
        System.out.println("\n======================================================================");
        System.out.println("NOISE SOURCE DETECTION COMPLETED");
        System.out.println("© 2025 Bevinda Vivian (13523120) - Teknik Informatika ITB");
        System.out.println("======================================================================");
    }
}
