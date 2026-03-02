package patternsClasses;

import globalGraph.*;
import it.davide.xml.PatternInstance;
import it.davide.xml.ProjectPatternsJson;

import java.util.ArrayList;
import java.util.List;

public class MasterMultiDetailsPattern extends GenericGraphPattern {

    public MasterMultiDetailsPattern() {
        this.name = "Master MultiDetails Pattern";
    }

    @Override
    public List<PatternInstance> matches(IFMLGraph graph,
            GraphNode startNode) {

        // Il pattern parte da una LIST
        if (startNode.getType() != NodeType.LIST)
            return null;

        List<PatternInstance> instances = new ArrayList<>();

        // Cerco navigation flow LIST → DETAILS
        for (Edge navEdge : graph.getOutgoing(startNode.getId())) {

            if (navEdge.getType() != FlowType.NAVIGATION)
                continue;

            GraphNode detailsNode = graph.getNode(navEdge.getTargetId());

            if (detailsNode == null)
                continue;

            if (detailsNode.getType() != NodeType.DETAILS)
                continue;

            // Ora verifico che nella pagina della DETAILS
            // esistano altri componenti (LIST o DETAILS)
            // collegati con DATA FLOW dalla DETAILS

            List<Edge> matchedEdges = new ArrayList<>();
            matchedEdges.add(navEdge);

            boolean hasAtLeastOneDataFlow = false;

            for (Edge outgoing : graph.getOutgoing(detailsNode.getId())) {

                if (outgoing.getType() != FlowType.DATA_FLOW)
                    continue;

                GraphNode target = graph.getNode(outgoing.getTargetId());

                if (target == null)
                    continue;

                // Deve essere nella stessa pagina della DETAILS
                if (!detailsNode.getPageId().equals(target.getPageId()))
                    continue;

                // Deve essere LIST o DETAILS
                if (target.getType() == NodeType.LIST ||
                        target.getType() == NodeType.DETAILS) {

                    hasAtLeastOneDataFlow = true;
                    matchedEdges.add(outgoing);
                }
            }

            if (hasAtLeastOneDataFlow) {
                instances.add(new PatternInstance(matchedEdges));
            }
        }

        return instances.isEmpty() ? null : instances;
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

    private ProjectPatternsJson.Endpoint buildEndpoint(GraphNode node) {

        ProjectPatternsJson.Endpoint ep = new ProjectPatternsJson.Endpoint();

        ep.id = node.getId();
        ep.type = node.getType().name();
        ep.pageId = node.getPageId();

        return ep;
    }
}