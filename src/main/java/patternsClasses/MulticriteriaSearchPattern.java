package patternsClasses;

import globalGraph.*;
import globalGraph.GraphNode.FieldInfo;
import it.davide.xml.PatternInstance;
import it.davide.xml.ProjectPatternsJson;
import it.davide.xml.utilityTools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MulticriteriaSearchPattern extends GenericGraphPattern {

    public MulticriteriaSearchPattern() {
        this.name = "Multicriteria Search Pattern";
    }

    /**
     * @param graph
     * @param startNode
     * @return List<PatternInstance>
     */
    @Override
    public List<PatternInstance> matches(IFMLGraph graph, GraphNode startNode) {

        if (startNode.getType() != NodeType.FORM)
            return null;

        Set<GraphNode.FieldInfo> fields = new HashSet<>();

        if (startNode != null && startNode.getFieldElementIds() != null) {
            for (Set<GraphNode.FieldInfo> set : startNode.getFieldElementIds().values()) {
                if (set != null) {
                    fields.addAll(set);
                }
            }
        }

        Set<String> fieldIds = utilityTools.extractAllFields(startNode);

        if (fieldIds.size() <= 1)
            return null;

        List<Edge> matched = new ArrayList<>();

        for (Edge edge : graph.getOutgoing(startNode.getId())) {

            if(edge.getType() != FlowType.NAVIGATION)
                continue;

            GraphNode target = graph.getNode(edge.getTargetId());

            if (target == null || target.getType() != NodeType.LIST)
                continue;

            Set<String> conditions = utilityTools.extractAllConditions(target);

            if (conditions.isEmpty())
                continue;

            boolean valid = utilityTools.checkFieldsAndConditions(
                    fieldIds,
                    conditions,
                    edge.getBindings());

            if (valid) {
                matched.add(edge);
            }
        }

        if (matched.isEmpty())
            return null;

        return List.of(new PatternInstance(matched, fields, null));
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

        entry.fields = buildFieldsEndpoint(instance.getFields());

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

    /**
     * @param node
     * @return Endpoint
     */
    private List<ProjectPatternsJson.FieldEndpoint> buildFieldsEndpoint(Set<FieldInfo> fields) {

        List<ProjectPatternsJson.FieldEndpoint> fieldEndpoints = new ArrayList<ProjectPatternsJson.FieldEndpoint>();

        for (FieldInfo field : fields) {

            ProjectPatternsJson.FieldEndpoint ep = new ProjectPatternsJson.FieldEndpoint();

            ep.fieldId = field.getId();
            ep.valueAttribute = field.getValueAttribute();
            ep.valueAssociationRole = field.getValueAssociationAttribute();
            fieldEndpoints.add(ep);
        }

        return fieldEndpoints;
    }
}
