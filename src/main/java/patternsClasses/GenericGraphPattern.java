package patternsClasses;

import globalGraph.*;
import it.davide.xml.PatternInstance;
import it.davide.xml.ProjectPatternsJson;

import java.util.List;

public abstract class GenericGraphPattern {

    protected String name;

    public String getName() {
        return name;
    }

    public abstract List<PatternInstance> matches(IFMLGraph graph,
                                                  GraphNode startNode);

    public abstract void createJsonPattern(ProjectPatternsJson projectJson,
                                           PatternInstance instance,
                                           IFMLGraph graph);
}
