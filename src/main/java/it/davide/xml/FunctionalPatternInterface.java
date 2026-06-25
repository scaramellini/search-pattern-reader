package it.davide.xml;

import globalGraph.IFMLGraph;

import java.util.List;

/**
 * Interface for functional patterns.
 * Functional patterns detect complex flows involving both page components and external Actions.
 */
public interface FunctionalPatternInterface {
    
    /**
     * Get the name of the pattern
     * 
     * @return String - the pattern name
     */
    String getName();

    /**
     * Get all detected matches of this pattern
     * 
     * @return List of FunctionalPatternMatch
     */
    List<FunctionalPatternMatch> getMatches();

    /**
     * Detect this pattern in the given graphs
     * 
     * @param pageGraph The page graph (contains page components and actions)
     * @param actionRegistry The action registry for lookup
     */
    void detect(IFMLGraph pageGraph, ActionRegistry actionRegistry);

    /**
     * Create JSON representations for all detected matches of this pattern
     * using the standard project pattern JSON structure.
     * 
     * @param projectJson The JSON report to populate
     * @param graph The unified graph containing UI components and actions
     */
    void createJsonPattern(ProjectPatternsJson projectJson, IFMLGraph graph);
}
