package it.davide.xml;

import globalGraph.*;
import patternsClasses.*;
import java.util.List;

public class GlobalPatternEngine {

    private final List<GenericGraphPattern> rules;

    public GlobalPatternEngine(List<GenericGraphPattern> rules) {
        this.rules = rules;
    }

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
