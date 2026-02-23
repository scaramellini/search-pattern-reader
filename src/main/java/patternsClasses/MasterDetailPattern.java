package patternsClasses;

import globalGraph.*;
import it.davide.xml.PatternInstance;
import it.davide.xml.ProjectPatternsJson;

import java.util.ArrayList;
import java.util.List;

public class MasterDetailPattern extends GenericGraphPattern {

    public MasterDetailPattern() {
        this.name = "Master Detail Pattern";
    }

    @Override
    public List<PatternInstance> matches(IFMLGraph graph,
                                         GraphNode startNode) {

        // Il pattern parte da una LIST
        if (startNode.getType() != NodeType.LIST)
            return null;

        List<PatternInstance> instances = new ArrayList<>();

        // Lista delle uscite LIST → DETAILS
        for (Edge edge : graph.getOutgoing(startNode.getId())) {

            GraphNode target = graph.getNode(edge.getTargetId());

            if (target == null)
                continue;

            if (target.getType() == NodeType.DETAILS) {

                List<Edge> matched = new ArrayList<>();
                matched.add(edge);

                instances.add(new PatternInstance(matched));
            }
        }

        return instances.isEmpty() ? null : instances;
    }

    @Override
    public void createJsonPattern(ProjectPatternsJson projectJson,
                                  PatternInstance instance,
                                  IFMLGraph graph) {

        ProjectPatternsJson.PatternEntry entry =
                new ProjectPatternsJson.PatternEntry();

        entry.patternType = name;

        for (Edge edge : instance.getEdges()) {

            GraphNode from = graph.getNode(edge.getSourceId());
            GraphNode to = graph.getNode(edge.getTargetId());

            ProjectPatternsJson.FlowEntry flow =
                    new ProjectPatternsJson.FlowEntry();

            flow.from = buildEndpoint(from);
            flow.to = buildEndpoint(to);

            // Copia dei bindings
            for (EdgeBinding b : edge.getBindings()) {
                ProjectPatternsJson.BindingEntry jsonBinding =
                        new ProjectPatternsJson.BindingEntry();

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

        ProjectPatternsJson.Endpoint ep =
                new ProjectPatternsJson.Endpoint();

        ep.id = node.getId();
        ep.type = node.getType().name();
        ep.pageId = node.getPageId();

        return ep;
    }
}
