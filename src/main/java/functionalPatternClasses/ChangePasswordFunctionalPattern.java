package functionalPatternClasses;

import java.util.ArrayList;
import java.util.List;

import globalGraph.ActionDefinition;
import globalGraph.ComponentFlow;
import globalGraph.Edge;
import globalGraph.GraphNode;
import globalGraph.IFMLGraph;
import globalGraph.NodeType;
import globalGraph.OperationComponent;
import it.davide.xml.ActionRegistry;
import it.davide.xml.FunctionalPatternInterface;
import it.davide.xml.FunctionalPatternMatch;

public class ChangePasswordFunctionalPattern extends FunctionalPatternInterface {
    public ChangePasswordFunctionalPattern() {
        this.name = "Change Password Functional Pattern";
    }

    @Override
    public void detect(IFMLGraph pageGraph, ActionRegistry actionRegistry) {

        matches.clear();

        // For each edge in the page graph that points to an action
        for (Edge edge : pageGraph.getAllEdges()) {

            List<Edge> matched = new ArrayList<>();

            if (!edge.pointsToAction()) {
                continue; // Skip edges that don't point to actions
            }

            String actionId = edge.getTargetId();

            ActionDefinition action = actionRegistry.getAction(actionId);

            if (action == null) {
                continue;
            }

            // Check if this action is a change password action
            if (!isChangePasswordAction(action)) {
                continue;
            }

            // Try to construct the full change password pattern
            GraphNode sourceNode = pageGraph.getNode(edge.getSourceId());

            if (sourceNode == null || !sourceNode.getType().equals(NodeType.FORM)) {
                continue; // change password pattern should be triggered from a Form component
            }

            // Check if pattern matches
            if (validateChangePasswordPattern(pageGraph, action, sourceNode, edge)) {
                matched.add(edge);
                FunctionalPatternMatch match = new FunctionalPatternMatch(
                        getName(),
                        matched);
                matches.add(match);
            }
        }
    }

    private boolean isChangePasswordAction(ActionDefinition action) {
        for (OperationComponent op : action.getOperationComponents()) {
            if ("ChangePassword".equals(op.getType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validate that the change password pattern is complete
     * - The action has input parameters for username/password
     * - The action has a ChangePassword operation
     * - The ChangePassword operation has success and error flows
     * - Both flows lead to output ports
     * 
     * @param pageGraph        The page graph
     * @param action           The action definition
     * @param formNode         The form node that triggers change password
     * @param formToActionEdge The edge from form to action
     * @return true if the pattern is valid
     */
    private boolean validateChangePasswordPattern(IFMLGraph pageGraph, ActionDefinition action,
            GraphNode formNode, Edge formToActionEdge) {

        // Check: Action has at least 2 input parameters (new password, old password)
        if (action.getInputParameters().size() < 2) {
            return false;
        }

        // Check: Action has success and error output ports
        if (action.getSuccessOutputPorts().isEmpty() || action.getErrorOutputPorts().isEmpty()) {
            return false;
        }

        // Check: Action contains ChangePassword operation
        OperationComponent changePasswordOp = null;
        for (OperationComponent op : action.getOperationComponents()) {
            if ("ChangePassword".equals(op.getType())) {
                changePasswordOp = op;
                break;
            }
        }

        if (changePasswordOp == null) {
            return false;
        }

        // Check: ChangePassword operation has both success and error flows
        boolean hasSuccessFlow = false;
        boolean hasErrorFlow = false;

        for (ComponentFlow flow : changePasswordOp.getFlows()) {
            if ("SuccessFlow".equals(flow.getType())) {
                hasSuccessFlow = true;
            } else if ("ErrorFlow".equals(flow.getType())) {
                hasErrorFlow = true;
            }
        }

        if (!hasSuccessFlow || !hasErrorFlow) {
            return false;
        }

        // Check: Form has parameter bindings that map to action input parameters
        if (formToActionEdge.getBindings().isEmpty()) {
            return false;
        }

        // Check that the flows of the action leads to a message component
        List<Edge> eventFlows = action.getAllEvents() != null ? action.getAllEvents().stream()
                .flatMap(event -> event.getNavigationFlows().stream())
                .toList() : List.of();

        if (eventFlows.size() < 1) {
            return false;
        }

        for (Edge flow : eventFlows) {
            GraphNode targetNode = pageGraph.getNode(flow.getTargetId());
            if (targetNode != null && !targetNode.getType().equals(NodeType.MESSAGE)) {
                return false;
            }
        }

        // All validation passed
        return true;
    }
}
