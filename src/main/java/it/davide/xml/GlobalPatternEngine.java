package it.davide.xml;

import globalGraph.*;
import patternsClasses.*;
import java.util.List;


//class responsible for applying the patterns rules on the global IFML graph, in order to detect the patterns in the IFML model
public class GlobalPatternEngine {

    private final List<GenericGraphPattern> rules;

    public GlobalPatternEngine(List<GenericGraphPattern> rules) {
        this.rules = rules;
    }

    /** 
     * @param graph the global graph representing the IFML model
     * @param projectJson the json object representing the project, to which the detected patterns will be added
     * @return ProjectPatternsJson
     */
    public ProjectPatternsJson detect(IFMLGraph graph, ProjectPatternsJson projectJson) {

        for (GraphNode node : graph.getAllNodes()) {

            for (GenericGraphPattern rule : rules) {

                List<PatternInstance> instances =
                        rule.matches(graph, node);

                if (instances != null) {

                    for (PatternInstance instance : instances) {
                        rule.createJsonPattern(projectJson, instance, graph);
                    }
                }
            }
        }

        return projectJson;
    }
}
