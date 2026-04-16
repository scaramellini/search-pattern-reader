package it.davide.xml;

import globalGraph.*;

import java.util.*;


//class responsible for traversing the global graph, it is used by the patterns rules to explore the graph and find the nodes and edges that match the patterns
//it is used for multipage patterns
public class GraphTraversal {

    private final IFMLGraph graph;

    public GraphTraversal(IFMLGraph graph) {
        this.graph = graph;
    }

    /** 
     * @param startNode node from which the traversal starts
     * @param maxDepth maximum depth of the traversal, if 0 it will traverse the entire graph
     * @return List<List<GraphNode>>
     */
    public List<List<GraphNode>> dfsPaths(GraphNode startNode, int maxDepth) {

        List<List<GraphNode>> results = new ArrayList<>();
        LinkedList<GraphNode> currentPath = new LinkedList<>();

        dfsRecursive(startNode, maxDepth, 0, currentPath, results);

        return results;
    }

    /** 
     * @param current current node being visited
     * @param maxDepth maximum depth of the traversal
     * @param depth current depth of the traversal
     * @param currentPath list of nodes representing the current path being explored
     * @param results list of paths found during the traversal
     */
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
