package patternsClasses;

import globalGraph.*;
import it.davide.xml.PatternInstance;
import it.davide.xml.ProjectPatternsJson;
import it.davide.xml.utilityTools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MasterMultiDetailsPattern extends GenericGraphPattern {

    public MasterMultiDetailsPattern() {
        this.name = "Master MultiDetails Pattern";
    }

    @Override
    public List<PatternInstance> matches(IFMLGraph graph, GraphNode startNode) {

        if (startNode.getType() != NodeType.LIST && startNode.getType() != NodeType.HIERARCHY)
            return null;

        List<PatternInstance> instances = new ArrayList<>();

        for (Edge navEdge : graph.getOutgoing(startNode.getId())) {

            if (navEdge.getType() != FlowType.NAVIGATION)
                continue;

            GraphNode firstDetails = graph.getNode(navEdge.getTargetId());

            if (firstDetails == null || firstDetails.getType() != NodeType.DETAILS)
                continue;

            if (startNode.getObjectId() == null || firstDetails.getObjectId() == null
                    || !startNode.getObjectId().equals(firstDetails.getObjectId()))
                continue;

            Set<String> visitedNodes = new HashSet<>();
            List<Edge> collectedEdges = new ArrayList<>();

            // add the initial navigation edge
            collectedEdges.add(navEdge);

            collectSubgraph(
                    graph,
                    firstDetails,
                    visitedNodes,
                    collectedEdges);

            // at least one additional details page must be found to consider it a master
            // multi details pattern, otherwise it would be a simple master detail pattern
            if (collectedEdges.size() > 1) {
                instances.add(new PatternInstance(collectedEdges));
            }

        }

        return instances.isEmpty() ? null : instances;
    }

    private void collectSubgraph(IFMLGraph graph,
            GraphNode current,
            Set<String> visitedNodes,
            List<Edge> collectedEdges) {

        visitedNodes.add(current.getId());

        for (Edge edge : graph.getOutgoing(current.getId())) {

            if (edge.getType() != FlowType.DATA_FLOW)
                continue;

            GraphNode target = graph.getNode(edge.getTargetId());

            if (!utilityTools.hasMatchingCondition(edge, target))
                continue;

            if (target == null)
                continue;

            if (!current.getPageId().equals(target.getPageId()))
                continue;

            if (target.getType() != NodeType.LIST &&
                    target.getType() != NodeType.DETAILS)
                continue;

            // evita duplicati di archi
            if (!collectedEdges.contains(edge)) {
                collectedEdges.add(edge);
            }

            if (!visitedNodes.contains(target.getId())) {
                collectSubgraph(graph, target, visitedNodes, collectedEdges);
            }
        }
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