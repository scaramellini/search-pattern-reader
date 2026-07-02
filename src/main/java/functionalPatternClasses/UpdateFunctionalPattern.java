package functionalPatternClasses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import globalGraph.ActionDefinition;
import globalGraph.ComponentFlow;
import globalGraph.Edge;
import globalGraph.EdgeBinding;
import globalGraph.FlowType;
import globalGraph.GraphNode;
import globalGraph.IFMLGraph;
import globalGraph.NodeType;
import globalGraph.OperationComponent;
import it.davide.xml.ActionRegistry;
import it.davide.xml.FunctionalPatternInterface;
import it.davide.xml.FunctionalPatternMatch;
import it.davide.xml.utilityTools;

public class UpdateFunctionalPattern extends FunctionalPatternInterface {
    public UpdateFunctionalPattern() {
        this.name = "Update Functional Pattern";
    }

    public void detect(IFMLGraph pageGraph, ActionRegistry actionRegistry) {
        List<GraphNode> components = pageGraph.getNodesByType(NodeType.LIST);

        for (GraphNode listComponent : components) {

            List<Edge> matched = new ArrayList<>();

            for (Edge listEdge : pageGraph.getOutgoing(listComponent.getId())) {

                if (listEdge.getType() != FlowType.NAVIGATION)
                    continue;

                GraphNode formComponent = pageGraph.getNode(listEdge.getTargetId());

                if (formComponent == null || formComponent.getType() != NodeType.FORM)
                    continue;

                Set<GraphNode.FieldInfo> fields = new HashSet<>();

                if (formComponent != null && formComponent.getFieldElementIds() != null) {
                    for (Set<GraphNode.FieldInfo> set : formComponent.getFieldElementIds().values()) {
                        if (set != null) {
                            fields.addAll(set);
                        }
                    }
                }

                if (!utilityTools.checkFields(formComponent)) {
                    continue;
                }
                
                List<EdgeBinding> bindings = listEdge.getBindings();

                // Check: edge has parameter bindings that map to action input parameters
                if (bindings.isEmpty()) {
                    continue;
                }

                // Check that each binding's target attribute corresponds to target conditional
                // expressions of the form component
                for (EdgeBinding binding : bindings) {
                    String targetParamId = utilityTools.extractConditionId(binding.getTargetAttribute());

                    boolean targetParamExists = Optional.ofNullable(formComponent.getConditionalExpressions())
                            .orElse(Collections.emptyMap())
                            .values().stream()
                            .filter(Objects::nonNull)
                            .flatMap(Set::stream)
                            .anyMatch(targetParamId::contains);

                    if (!targetParamExists) {
                        continue;
                    }
                }

                for (Edge formEdge : pageGraph.getOutgoing(formComponent.getId())) {
                    if (!formEdge.pointsToAction()) {
                        continue; // Skip edges that don't point to actions
                    }

                    String actionId = formEdge.getTargetId();
                    ActionDefinition action = actionRegistry.getAction(actionId);

                    if (action == null) {
                        continue;
                    }

                    // Check if this action is a update profile action
                    if (!isUpdateProfileAction(action)) {
                        continue;
                    }

                    // Check if pattern matches
                    if (validateUpdateProfilePattern(pageGraph, action, actionId, formComponent, formEdge)) {
                        matched.add(listEdge);
                        matched.add(formEdge);
                        FunctionalPatternMatch match = new FunctionalPatternMatch(
                                getName(),
                                matched);
                        matches.add(match);
                    }
                }

            }
        }
    }

    private boolean isUpdateProfileAction(ActionDefinition action) {

        for (OperationComponent op : action.getOperationComponents()) {
            // to update an object you use a create component that has a conditional
            // expression that identifies the object to update
            if ("Create".equals(op.getType())) {
                return true;
            }
        }
        return false;
    }

    private boolean validateUpdateProfilePattern(IFMLGraph pageGraph, ActionDefinition action, String actionId,
            GraphNode formNode, Edge edge) {
        // Check: Action has at least 1 input parameters
        if (action.getInputParameters().size() < 1) {
            return false;
        }

        // Check: Action has success and error output ports
        if (action.getSuccessOutputPorts().isEmpty() || action.getErrorOutputPorts().isEmpty()) {
            return false;
        }

        // Check: Action contains update profile operation
        OperationComponent updateProfileOp = null;
        for (OperationComponent op : action.getOperationComponents()) {
            if ("Create".equals(op.getType())) {
                updateProfileOp = op;
                break;
            }
        }

        if (updateProfileOp == null) {
            return false;
        }

        // Check: UpdateProfile operation has both success and error flows
        boolean hasSuccessFlow = false;
        boolean hasErrorFlow = false;

        for (ComponentFlow flow : updateProfileOp.getFlows()) {
            if ("SuccessFlow".equals(flow.getType())) {
                hasSuccessFlow = true;
            } else if ("ErrorFlow".equals(flow.getType())) {
                hasErrorFlow = true;
            }
        }

        if (!hasSuccessFlow || !hasErrorFlow) {
            return false;
        }

        if(!utilityTools.checkInputPortBindings(edge, action)) {
            return false;
        }

        // Check that the flows of the action leads to a message component
        List<Edge> eventFlows = action.getAllEvents(actionId) != null ? action.getAllEvents(actionId).stream()
                .flatMap(event -> event.getNavigationFlows().stream())
                .toList() : List.of();

        for (Edge flow : eventFlows) {
            GraphNode targetNode = pageGraph.getNode(flow.getTargetId());
            if (targetNode != null && !(targetNode.getType().equals(NodeType.MESSAGE)
                    || targetNode.getType().equals(NodeType.DETAILS) || targetNode.getType().equals(NodeType.LIST))) {
                return false;
            }
        }

        // All validation passed
        return true;
    }
}
