package it.davide.xml;

import java.util.List;

import globalGraph.Edge;

/**
 * Represents a detected functional pattern match
 */
public class FunctionalPatternMatch {
    private final String patternName;
    private final List<Edge> edges;

    public FunctionalPatternMatch(String patternName, List<Edge> edges) {
        this.patternName = patternName;
        this.edges = edges;
    }

    public String getPatternName() {
        return patternName;
    }

    public List<Edge> getEdges() {
        return edges;
    }
}
