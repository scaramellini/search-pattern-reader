package patternsClasses;

import globalGraph.*;
import it.davide.xml.PatternInstance;
import it.davide.xml.ProjectPatternsJson;

import java.util.ArrayList;
import java.util.List;

public class NewBasicSearchPattern extends GenericGraphPattern {

    public NewBasicSearchPattern() {
        this.name = "New Basic Search Pattern";
    }

    @Override
    public List<PatternInstance> matches(IFMLGraph graph,
            GraphNode startNode) {

        if (startNode.getType() != NodeType.FORM)
            return null;

        List<Edge> matched = new ArrayList<>();

        for (Edge edge : graph.getOutgoing(startNode.getId())) {

            GraphNode target = graph.getNode(edge.getTargetId());

            if (target != null && target.getType() == NodeType.LIST) {
                matched.add(edge);
            }
        }

        if (matched.isEmpty())
            return null;

        return List.of(new PatternInstance(matched));
    }

    @Override
    public void createJsonPattern(ProjectPatternsJson projectJson,
            PatternInstance instance,
            IFMLGraph graph) {

        ProjectPatternsJson.PatternEntry entry = new ProjectPatternsJson.PatternEntry();

        entry.patternType = name;

        for (Edge edge : instance.getEdges()) {

            GraphNode from = graph.getNode(edge.getSourceId());
            GraphNode to = graph.getNode(edge.getTargetId());

            ProjectPatternsJson.FlowEntry flow = new ProjectPatternsJson.FlowEntry();

            flow.from = buildEndpoint(from);
            flow.to = buildEndpoint(to);

            entry.flows.add(flow);
        }

        projectJson.patterns.add(entry);
    }

    private ProjectPatternsJson.Endpoint buildEndpoint(GraphNode node) {

        ProjectPatternsJson.Endpoint ep = new ProjectPatternsJson.Endpoint();

        ep.id = node.getId();
        ep.type = node.getType().name();
        ep.pageId = node.getPageId();

        return ep;
    }
}
