package it.davide.xml;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import globalGraph.Edge;
import globalGraph.EdgeBinding;
import globalGraph.GraphNode;

public class utilityTools {
    private static String extractConditionIdFromBinding(String targetAttr) {

        if (targetAttr == null)
            return null;

        int braceIndex = targetAttr.indexOf("}");
        if (braceIndex != -1) {
            targetAttr = targetAttr.substring(braceIndex + 1);
        }

        int dotIndex = targetAttr.indexOf(".");
        if (dotIndex != -1) {
            targetAttr = targetAttr.substring(0, dotIndex);
        }

        return targetAttr;
    }

    public static boolean hasMatchingCondition(Edge edge, GraphNode targetNode) {

        if(edge == null || targetNode == null || targetNode.getConditionalExpressions() == null) {
            return false;
        }

        Set<String> targetConditions = targetNode.getConditionalExpressions()
                .get(GraphNode.ConditionalExpressionCategory.associationCondition);

        if (targetConditions == null || targetConditions.isEmpty())
            return false;

        for (EdgeBinding binding : edge.getBindings()) {

            String targetAttr = binding.getTargetAttribute();
            String bindingConditionId = extractConditionIdFromBinding(targetAttr);

            if (bindingConditionId == null)
                continue;

            // on econdition is enough, but we check all the conditions of the target node to be sure
            for (String condId : targetConditions) {

                if (condId.contains("#") &&
                        bindingConditionId.endsWith(
                                condId.substring(condId.lastIndexOf("#")))) {
                    return true;
                }

                if (bindingConditionId.equals(condId)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean checkFieldsAndConditions(
            Set<String> fieldIds,
            Set<String> conditions,
            List<EdgeBinding> bindings) {

        if (bindings == null || bindings.isEmpty())
            return false;

        Set<String> boundFields = new HashSet<>();
        Set<String> satisfiedConditions = new HashSet<>();

        for (EdgeBinding binding : bindings) {

            String fieldId = extractFieldId(binding.getSourceAttribute());
            String target = extractConditionId(binding.getTargetAttribute());

            // source parameter has to be a field of the source node
            if (fieldId == null || !fieldIds.contains(fieldId)) {
                return false;
            }

            // target parameter has to be a condition of the target node
            if (!conditions.contains(target)) {
                return false;
            }

            boundFields.add(fieldId);
            satisfiedConditions.add(target);
        }

        // each field has to be used to valorize at least one condition
        if (!boundFields.containsAll(fieldIds))
            return false;

        // each condition has to be satisfied
        /* if (!satisfiedConditions.containsAll(conditions))
            return false; */

        return true;
    }

    public static int getConditionCount(Edge edge, GraphNode targetNode) {
        if (edge == null || targetNode == null || targetNode.getConditionalExpressions() == null) {
            return 0;
        }

        int count = 0;

        Set<String> targetConditionsId = extractAllConditions(targetNode);

        for (EdgeBinding binding : edge.getBindings()) {

            String target = extractConditionIdFromBinding(binding.getTargetAttribute());

            if (target != null && targetConditionsId.contains(target)) {
                count++;
            }
        }

        return count;
    }

    // example of source: {fieldId}.value, return only fieldId
    public static String extractFieldId(String source) {
        if (source == null)
            return null;

        int start = source.indexOf('{');
        int end = source.indexOf('}');

        if (start >= 0 && end > start) {
            return source.substring(start + 1, end);
        }

        return null;
    }

    public static String extractConditionId(String conditionId) {
        int dotIndex = conditionId.indexOf(".");
        if (dotIndex != -1) {
            conditionId = conditionId.substring(0, dotIndex);
        }

        return conditionId;
    }

    public static Set<String> extractAllConditions(GraphNode node) {

        if (node == null || node.getConditionalExpressions() == null) {
            return Collections.emptySet();
        }

        return node.getConditionalExpressions()
                .values()
                .stream()
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public static Set<String> extractAllFields(GraphNode node) {

        if (node == null || node.getFieldElementIds() == null) {
            return Collections.emptySet();
        }

        return node.getFieldElementIds()
                .values()
                .stream()
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .filter(Objects::nonNull)
                .map(GraphNode.FieldInfo::getId)
                .collect(Collectors.toSet());
    }

    public static Set<String> extractSimpleFields(GraphNode node) {
        if (node == null)
            return Collections.emptySet();

        Map<GraphNode.FieldElementCategory, Set<GraphNode.FieldInfo>> map = node.getFieldElementIds();

        if (map == null)
            return Collections.emptySet();

        Set<GraphNode.FieldInfo> fields = map.get(GraphNode.FieldElementCategory.Field);

        if (fields == null)
            return Collections.emptySet();

        return fields.stream()
                .map(GraphNode.FieldInfo::getId)
                .collect(Collectors.toSet());
    }
}
