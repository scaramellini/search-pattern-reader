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

public class PreloadedFormPattern extends GenericGraphPattern {

    public PreloadedFormPattern() {
        this.name = "Preloaded Form Pattern";
    }

    /**
     * @param graph
     * @param startNode
     * @return List<PatternInstance>
     */
    @Override
    public List<PatternInstance> matches(IFMLGraph graph,
            GraphNode startNode) {

        if (startNode.getType() != NodeType.DETAILS)
            return null;

        List<PatternInstance> instances = new ArrayList<>();

        for (Edge edge : graph.getOutgoing(startNode.getId())) {

            if (edge.getType() != FlowType.DATA_FLOW)
                continue;

            int preloadedFieldsCount = 0;

            GraphNode target = graph.getNode(edge.getTargetId());

            if (target == null ||
                    target.getType() != NodeType.FORM)
                continue;

            Set<GraphNode.FieldInfo> fields = new HashSet<>();

            if (target != null && target.getFieldElementIds() != null) {
                for (Set<GraphNode.FieldInfo> set : target.getFieldElementIds().values()) {
                    if (set != null) {
                        fields.addAll(set);
                    }
                }
            }

            for (FieldInfo field : fields) {
                if ((field.getValueAttribute() != null && !field.getValueAttribute().isEmpty())
                        || (field.getValueAssociationAttribute() != null
                                && !field.getValueAssociationAttribute().isEmpty())) {
                    preloadedFieldsCount++;
                }
            }

            boolean valid = true;

            Set<String> fieldIds = utilityTools.extractAllFields(target);

            for (EdgeBinding binding : edge.getBindings()) {

                String targetAttr = binding.getTargetAttribute();

                if (targetAttr == null)
                    continue;

                if (fieldIds.contains(utilityTools.extractFieldId(targetAttr))) {
                    preloadedFieldsCount++;
                }
            }

            if (valid && preloadedFieldsCount >= fields.size()) {
                List<Edge> matched = new ArrayList<>();
                matched.add(edge);

                instances.add(new PatternInstance(matched, fields, null));
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