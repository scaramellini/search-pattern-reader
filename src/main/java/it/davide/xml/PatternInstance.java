package it.davide.xml;

import globalGraph.Edge;
import java.util.List;

// class representing an instance of a pattern, it contains the edges that matched the pattern rules and that will be used to create the json report entry for that pattern instance
public class PatternInstance {

    private final List<Edge> edges;

    public PatternInstance(List<Edge> edges) {
        this.edges = edges;
    }

    /** 
     * @return List<Edge>
     */
    public List<Edge> getEdges() {
        return edges;
    }
}
