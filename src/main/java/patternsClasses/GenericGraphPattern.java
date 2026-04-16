package patternsClasses;

import globalGraph.*;
import it.davide.xml.PatternInstance;
import it.davide.xml.ProjectPatternsJson;

import java.util.List;

// abstract class representing a generic graph pattern, it defines the methods that must be implemented by all the specific pattern classes
public abstract class GenericGraphPattern {

    protected String name;

    public String getName() {
        return name;
    }

    //method that applies a list of rules to detect the patterns in the global graph, it returns a list of pattern instances that matched the rules
    public abstract List<PatternInstance> matches(IFMLGraph graph,
            GraphNode startNode);

    //method that creates the json report entry for a pattern instance, it takes as input the json object representing the project, the pattern instances and the global graph
    public abstract void createJsonPattern(ProjectPatternsJson projectJson,
            PatternInstance instance,
            IFMLGraph graph);
}
