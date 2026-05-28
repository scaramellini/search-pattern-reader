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

public class PreloadedFormPatternClassVariant extends GenericGraphPattern {

    public PreloadedFormPatternClassVariant() {
        this.name = "Preloaded Form Pattern Class Variant";
    }

    @Override
    public List<PatternInstance> matches(IFMLGraph graph, GraphNode startNode) {

        // The pattern starts from a Details component
        if (startNode.getType() != NodeType.DETAILS)
            return null;

        List<PatternInstance> instances = new ArrayList<>();

        for (Edge edge : graph.getOutgoing(startNode.getId())) {

            // Must be a data flow
            if (edge.getType() != FlowType.DATA_FLOW)
                continue;

            int preloadedFieldsCount = 0;

            GraphNode target = graph.getNode(edge.getTargetId());

            // Target must be a Form
            if (target == null || target.getType() != NodeType.FORM)
                continue;

            // Details and Form must insist on the same entity
            if (startNode.getObjectId() == null || target.getObjectId() == null
                    || !startNode.getObjectId().equals(target.getObjectId()))
                continue;

            // The form must contain at least one key condition or one association condition
            boolean hasKeyCondition = false;
            boolean hasAssociationCondition = false;

            if (target.getConditionalExpressions() != null) {

                Set<String> keyConditions = target.getConditionalExpressions()
                        .get(GraphNode.ConditionalExpressionCategory.keyCondition);

                Set<String> associationConditions = target.getConditionalExpressions()
                        .get(GraphNode.ConditionalExpressionCategory.associationCondition);

                hasKeyCondition = keyConditions != null && !keyConditions.isEmpty();

                hasAssociationCondition = associationConditions != null
                        && !associationConditions.isEmpty();
            }

            if (!hasKeyCondition && !hasAssociationCondition)
                continue;

            // The data flow must contain at least one parameter binding
            if (edge.getBindings() == null || edge.getBindings().isEmpty())
                continue;

            // the target of the parameter binding must be the key condition of the target
            // form
            boolean keyConditionValid = false;

             Set<String> fieldIds = utilityTools.extractAllFields(target);

            for (EdgeBinding binding : edge.getBindings()) {

                String targetAttr = binding.getTargetAttribute();

                if (targetAttr == null || targetAttr.isEmpty())
                    continue;

                if (hasKeyCondition) {
                    Set<String> keyConditions = target.getConditionalExpressions()
                            .get(GraphNode.ConditionalExpressionCategory.keyCondition);

                    if (keyConditions != null && targetAttr.contains(keyConditions.iterator().next())) {
                        keyConditionValid = true;
                        preloadedFieldsCount++;
                    }
                }

                if (fieldIds.contains(utilityTools.extractFieldId(targetAttr))) {
                    preloadedFieldsCount++;
                }

            }

            // Validate fields
            Set<GraphNode.FieldInfo> fields = new HashSet<>();

            if (target != null && target.getFieldElementIds() != null) {
                for (Set<GraphNode.FieldInfo> set : target.getFieldElementIds().values()) {
                    if (set != null) {
                        fields.addAll(set);
                    }
                }
            }

            if (fields == null || fields.isEmpty())
                continue;

            boolean valid = true;

            for (FieldInfo field : fields) {
                if ((field.getValueAttribute() != null && !field.getValueAttribute().isEmpty())
                        || (field.getValueAssociationAttribute() != null
                                && !field.getValueAssociationAttribute().isEmpty())) {
                    preloadedFieldsCount++;
                }
            }

            // Selection fields must have their own data binding
            Set<GraphNode.FieldInfo> selectionFields = new HashSet<>();

            if (target.getFieldElementIds().get(GraphNode.FieldElementCategory.SelectionField) != null) {
                selectionFields
                        .addAll(target.getFieldElementIds().get(GraphNode.FieldElementCategory.SelectionField));
            }

            for (GraphNode.FieldInfo selectionField : selectionFields) {
                if (selectionField.getFieldDataBinding() != null
                        && !selectionField.getFieldDataBinding().isEmpty()) {

                    // They must also expose at least one value attribute
                    if (selectionField.getValueAttribute() == null
                            || selectionField.getValueAttribute().isEmpty()) {

                        valid = false;
                        break;
                    }
                    preloadedFieldsCount++;
                }
            }

            if (!valid && !keyConditionValid && preloadedFieldsCount < fields.size())
                continue;

            List<Edge> matchedEdges = new ArrayList<>();
            matchedEdges.add(edge);

            instances.add(new PatternInstance(matchedEdges, fields, null));
        }

        return instances.isEmpty() ? null : instances;
    }

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