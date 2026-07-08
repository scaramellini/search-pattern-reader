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

    public void setName(String name) {
        this.name = name;
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

        List<PatternInstance> instances = new ArrayList<>();

        for (Edge edge : graph.getOutgoing(startNode.getId())) {

            if(edge.getType() != FlowType.NAVIGATION)
                continue;

            GraphNode target = graph.getNode(edge.getTargetId());

            if (target == null)
                continue;

            if (startNode.getObjectId() == null || target.getObjectId() == null
                    || !startNode.getObjectId().equals(target.getObjectId()))
                continue;

            if (!startNode.getPageId().equals(target.getPageId()))
                //continue;
                setName("Multi Detail Multipage Variant Pattern");

            if (target.getType() == NodeType.DETAILS) {

                List<Edge> matched = new ArrayList<>();
                matched.add(edge);

                instances.add(new PatternInstance(matched, null, null));
            }
        }

        return instances.isEmpty() ? null : instances;
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

        entry.patternType = getPatternTypeWithVariant(name, graph, instance,
                "Master Detail Hierachy Variant Pattern", "Master Detail Dialog Variant Pattern");

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
        ep.dataBinding = node.getObjectId();
        ep.isInDialogPage = node.isInDialogPage();

        return ep;
    }
}
