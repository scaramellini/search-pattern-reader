package it.davide.xml;

import globalGraph.Edge;
import java.util.List;

public class PatternInstance {

    private final List<Edge> edges;

    public PatternInstance(List<Edge> edges) {
        this.edges = edges;
    }

    public List<Edge> getEdges() {
        return edges;
    }
}
