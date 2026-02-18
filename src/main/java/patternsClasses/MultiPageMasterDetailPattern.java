package patternsClasses;

import globalGraph.*;
import it.davide.xml.ProjectPatternsJson;
import it.davide.xml.GraphTraversal;
import it.davide.xml.PatternInstance;

import java.util.ArrayList;
import java.util.List;

public class MultiPageMasterDetailPattern extends GenericGraphPattern {

    public MultiPageMasterDetailPattern() {
        this.name = "Multi-Page List Chain Pattern";
    }

    @Override
    public List<PatternInstance> matches(IFMLGraph graph,
            GraphNode startNode) {

        if (startNode.getType() != NodeType.LIST)
            return null;

        GraphTraversal traversal = new GraphTraversal(graph);

        List<List<GraphNode>> allPaths = traversal.dfsPaths(startNode, 0);

        List<PatternInstance> instances = new ArrayList<>();

        for (List<GraphNode> path : allPaths) {

            if (path.size() < 3)
                continue;

            if (!isValidPath(path))
                continue;

            if (!isMultiPage(path))
                continue;

            List<Edge> edges = extractEdgesFromPath(graph, path);

            instances.add(new PatternInstance(edges));
        }

        return instances.isEmpty() ? null : instances;
    }

    /**
     * Regole:
     * - primo nodo: LIST
     * - intermedi: solo LIST
     * - ultimo: LIST o DETAILS
     * - tutti i nodi: solo LIST o DETAILS
     */
    private boolean isValidPath(List<GraphNode> path) {

        if (path.get(0).getType() != NodeType.LIST)
            return false;

        for (int i = 0; i < path.size(); i++) {

            NodeType type = path.get(i).getType();

            if (i == path.size() - 1) {
                if (type != NodeType.LIST && type != NodeType.DETAILS)
                    return false;
            } else {
                if (type != NodeType.LIST)
                    return false;
            }
        }

        return true;
    }

    private boolean isMultiPage(List<GraphNode> path) {

        return path.stream()
                .map(GraphNode::getPageId)
                .distinct()
                .count() == path.size();
    }

    private List<Edge> extractEdgesFromPath(IFMLGraph graph, List<GraphNode> path) {

        List<Edge> edges = new ArrayList<>();

        for (int i = 0; i < path.size() - 1; i++) {

            GraphNode from = path.get(i);
            GraphNode to = path.get(i + 1);

            for (Edge edge : graph.getOutgoing(from.getId())) {
                if (edge.getTargetId().equals(to.getId())) {
                    edges.add(edge);
                }
            }
        }

        return edges;
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
