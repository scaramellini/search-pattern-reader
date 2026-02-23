package patternsClasses;

import globalGraph.*;
import it.davide.xml.PatternInstance;
import it.davide.xml.ProjectPatternsJson;

import java.util.ArrayList;
import java.util.List;

public class QuickSearchPattern extends GenericGraphPattern {

    public QuickSearchPattern() {
        this.name = "Quick Search Pattern";
    }

    @Override
    public List<PatternInstance> matches(IFMLGraph graph,
                                         GraphNode startNode) {

        if (startNode.getType() != NodeType.FORM)
            return null;

        List<PatternInstance> instances = new ArrayList<>();

        // FORM → LIST
        for (Edge formToList : graph.getOutgoing(startNode.getId())) {

            GraphNode listNode = graph.getNode(formToList.getTargetId());
            if (listNode == null || listNode.getType() != NodeType.LIST)
                continue;

            // Cercare FORM → startNode (inverse)
            for (Edge incoming : graph.getIncoming(startNode.getId())) {

                GraphNode source = graph.getNode(incoming.getSourceId());
                if (source == null)
                    continue;

                if (source.getType() == NodeType.FORM) {

                    List<Edge> matched = new ArrayList<>();
                    matched.add(formToList);
                    matched.add(incoming);

                    instances.add(new PatternInstance(matched));
                }
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
