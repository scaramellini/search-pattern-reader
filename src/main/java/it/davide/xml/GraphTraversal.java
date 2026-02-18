package it.davide.xml;

import globalGraph.*;

import java.util.*;

public class GraphTraversal {

    private final IFMLGraph graph;

    public GraphTraversal(IFMLGraph graph) {
        this.graph = graph;
    }

    /**
     * Trova tutti i percorsi partendo da startNode.
     * Nessun limite di profondità se maxDepth = 0.
     */
    public List<List<GraphNode>> dfsPaths(GraphNode startNode, int maxDepth) {

        List<List<GraphNode>> results = new ArrayList<>();
        LinkedList<GraphNode> currentPath = new LinkedList<>();

        dfsRecursive(startNode, maxDepth, 0, currentPath, results);

        return results;
    }

    private void dfsRecursive(GraphNode current,
                              int maxDepth,
                              int depth,
                              LinkedList<GraphNode> currentPath,
                              List<List<GraphNode>> results) {

        currentPath.add(current);

        results.add(new ArrayList<>(currentPath));

        if (maxDepth == 0 || depth < maxDepth) {
            for (Edge edge : graph.getOutgoing(current.getId())) {

                GraphNode next = graph.getNode(edge.getTargetId());

                if (next != null && !currentPath.contains(next)) {
                    dfsRecursive(next, maxDepth, depth + 1, currentPath, results);
                }
            }
        }

        currentPath.removeLast();
    }
}
