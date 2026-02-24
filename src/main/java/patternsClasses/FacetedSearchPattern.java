package patternsClasses;

import globalGraph.*;
import it.davide.xml.PatternInstance;
import it.davide.xml.ProjectPatternsJson;

import java.util.ArrayList;
import java.util.List;

public class FacetedSearchPattern extends GenericGraphPattern {

    public FacetedSearchPattern() {
        this.name = "Faceted Search Pattern"; 
    }

    @Override
    public List<PatternInstance> matches(IFMLGraph graph,
            GraphNode startNode) {

        if (startNode.getType() != NodeType.FORM)
            return null;

        List<PatternInstance> instances = new ArrayList<>();

        // 1️⃣ FORM → LIST₁
        for (Edge formToList : graph.getOutgoing(startNode.getId())) {

            GraphNode list1 = graph.getNode(formToList.getTargetId());

            if (list1 == null ||
                    list1.getType() != NodeType.LIST)
                continue;

            // 2️⃣ LIST₂ → LIST₁ (usiamo incoming)
            for (Edge incoming : graph.getIncoming(list1.getId())) {

                GraphNode source = graph.getNode(incoming.getSourceId());

                if (source == null)
                    continue;

                // Deve essere una LIST diversa da LIST₁
                if (source.getType() == NodeType.LIST &&
                        !source.getId().equals(list1.getId())) {

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

        ProjectPatternsJson.PatternEntry entry = new ProjectPatternsJson.PatternEntry();

        entry.patternType = name;

        for (Edge edge : instance.getEdges()) {

            GraphNode from = graph.getNode(edge.getSourceId());
            GraphNode to = graph.getNode(edge.getTargetId());

            ProjectPatternsJson.FlowEntry flow = new ProjectPatternsJson.FlowEntry();

            flow.from = buildEndpoint(from);
            flow.to = buildEndpoint(to);

            // Esportazione bindings
            for (EdgeBinding b : edge.getBindings()) {

                ProjectPatternsJson.BindingEntry jb = new ProjectPatternsJson.BindingEntry();

                jb.automaticCoupling = b.isAutomaticCoupling();

                if (!b.isAutomaticCoupling()) {
                    jb.source = b.getSourceAttribute();
                    jb.target = b.getTargetAttribute();
                }

                flow.bindings.add(jb);
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
