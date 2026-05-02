package patternsClasses;

import globalGraph.*;
import it.davide.xml.ProjectPatternsJson;
import it.davide.xml.utilityTools;
import it.davide.xml.GraphTraversal;
import it.davide.xml.PatternInstance;

import java.util.ArrayList;
import java.util.List;

public class MultiLevelMasterDetailPattern extends GenericGraphPattern {

    public MultiLevelMasterDetailPattern() {
        this.name = "Multilevel Master Detail Pattern";
    }

    /**
     * @param graph
     * @param startNode
     * @return List<PatternInstance>
     */
    @Override
    public List<PatternInstance> matches(IFMLGraph graph,
            GraphNode startNode) {

        if (startNode.getType() != NodeType.LIST && startNode.getType() != NodeType.HIERARCHY)
            return null;

        GraphTraversal traversal = new GraphTraversal(graph);

        List<List<GraphNode>> allPaths = traversal.dfsPaths(startNode, 0);

        List<PatternInstance> instances = new ArrayList<>();

        for (List<GraphNode> path : allPaths) {

            // the path must have at least 3 nodes to be a valid multi level master detail
            // pattern, otherwise it would be a simple master detail pattern
            if (path.size() < 3)
                continue;

            if (!isValidPath(path))
                continue;

            // considering only single page patterns
            if (isMultiPage(path))
                continue;

            List<Edge> edges = extractEdgesFromPath(graph, path);

            if(edges.size() != path.size() - 1)
                continue;

            instances.add(new PatternInstance(edges));
        }

        return instances.isEmpty() ? null : instances;
    }

    /**
     * @param path
     * @return boolean
     */
    private boolean isValidPath(List<GraphNode> path) {

        if (path.get(0).getType() != NodeType.LIST)
            return false;

        for (int i = 0; i < path.size(); i++) {

            NodeType type = path.get(i).getType();

            // last node must be details or list, all the others must be list
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

    /**
     * @param path
     * @return boolean
     */
    private boolean isMultiPage(List<GraphNode> path) {

        return path.stream()
                .map(GraphNode::getPageId)
                .distinct()
                .count() == path.size();
    }

    /**
     * @param graph
     * @param path
     * @return List<Edge>
     */
    private List<Edge> extractEdgesFromPath(IFMLGraph graph, List<GraphNode> path) {

        List<Edge> edges = new ArrayList<>();

        for (int i = 0; i < path.size() - 1; i++) {

            GraphNode from = path.get(i);
            GraphNode to = path.get(i + 1);

            for (Edge edge : graph.getOutgoing(from.getId())) {
                if (edge.getTargetId().equals(to.getId())) {
                    if (utilityTools.hasMatchingCondition(edge, to)) {
                        edges.add(edge);
                    }
                }
            }
        }

        return edges;
    }

    /**
     * @param projectJson
     * @param instance
     * @param graph
     */
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

            for (EdgeBinding b : edge.getBindings()) {

                ProjectPatternsJson.BindingEntry jsonBinding = new ProjectPatternsJson.BindingEntry();

                jsonBinding.automaticCoupling = b.isAutomaticCoupling();

                if (!b.isAutomaticCoupling()) {
                    jsonBinding.source = b.getSourceAttribute();
                    jsonBinding.target = b.getTargetAttribute();
                }

                flow.bindings.add(jsonBinding);
            }

            entry.flows.add(flow);
        }

        projectJson.patterns.add(entry);
    }

    /**
     * @param node
     * @return Endpoint
     */
    private ProjectPatternsJson.Endpoint buildEndpoint(GraphNode node) {

        ProjectPatternsJson.Endpoint ep = new ProjectPatternsJson.Endpoint();

        ep.id = node.getId();
        ep.type = node.getType().name();
        ep.pageId = node.getPageId();

        return ep;
    }
}
