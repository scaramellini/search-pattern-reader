package it.davide.xml;

import globalGraph.Edge;
import globalGraph.GraphNode;

import java.util.List;
import java.util.Set;

// class representing an instance of a pattern, it contains the edges that matched the pattern rules
// and that will be used to create the json report entry for that pattern instance
public class PatternInstance {

    private final List<Edge> edges;
    private final Set<GraphNode.FieldInfo> fields;
    private final GraphNode singleNode; // For patterns that match a single node, we can store it here

    public PatternInstance(List<Edge> edges, Set<GraphNode.FieldInfo> fields, GraphNode singleNode) {
        this.edges = edges;
        this.fields = fields;
        this.singleNode = singleNode;
    }

    /**
     * @return List<Edge>
     */
    public List<Edge> getEdges() {
        return edges;
    }

    /**
     * @return Set<GraphNode.FieldInfo>
     */
    public Set<GraphNode.FieldInfo> getFields() {
        return fields;
    }

    /**
     * @return GraphNode
     */
    public GraphNode getSingleNode() {
        return singleNode;
    }
}
