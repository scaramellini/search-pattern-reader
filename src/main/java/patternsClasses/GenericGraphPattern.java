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

    protected String getPatternTypeWithVariant(String baseName, IFMLGraph graph, PatternInstance instance,
            String hierarchyVariantName, String dialogVariantName) {
        GraphNode startNode = getStartNode(graph, instance);

        if (startNode != null && startNode.getType() == NodeType.HIERARCHY && hierarchyVariantName != null
                && !hierarchyVariantName.isEmpty()) {
            return hierarchyVariantName;
        }

        if (hasDialogVariant(graph, instance, startNode) && dialogVariantName != null
                && !dialogVariantName.isEmpty()) {
            return dialogVariantName;
        }

        return baseName;
    }

    protected GraphNode getStartNode(IFMLGraph graph, PatternInstance instance) {
        if (instance == null)
            return null;

        if (instance.getSingleNode() != null)
            return instance.getSingleNode();

        if (instance.getEdges() == null || instance.getEdges().isEmpty())
            return null;

        Edge first = instance.getEdges().get(0);
        return graph.getNode(first.getSourceId());
    }

    protected boolean hasDialogVariant(IFMLGraph graph, PatternInstance instance, GraphNode startNode) {
        if (instance == null)
            return false;

        if (instance.getSingleNode() != null)
            return instance.getSingleNode().isInDialogPage();

        if (instance.getEdges() == null)
            return false;

        String startId = startNode != null ? startNode.getId() : null;

        for (Edge edge : instance.getEdges()) {
            GraphNode target = graph.getNode(edge.getTargetId());
            if (target == null)
                continue;
            if (startId != null && target.getId().equals(startId))
                continue;
            if (target.isInDialogPage())
                return true;
        }

        return false;
    }

    //method that applies a list of rules to detect the patterns in the global graph, it returns a list of pattern instances that matched the rules
    public abstract List<PatternInstance> matches(IFMLGraph graph,
            GraphNode startNode);

    //method that creates the json report entry for a pattern instance, it takes as input the json object representing the project, the pattern instances and the global graph
    public abstract void createJsonPattern(ProjectPatternsJson projectJson,
            PatternInstance instance,
            IFMLGraph graph);
}
