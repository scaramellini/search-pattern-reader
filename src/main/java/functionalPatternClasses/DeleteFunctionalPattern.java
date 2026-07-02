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
import it.davide.xml.utilityTools;

public class DeleteFunctionalPattern  extends FunctionalPatternInterface {
    public DeleteFunctionalPattern() {
        this.name = "Delete Functional Pattern";
    }

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

            // Check if this action is a delete action
            if (!isDeleteAction(action)) {
                continue;
            }

            // Try to construct the full delete pattern
            GraphNode sourceNode = pageGraph.getNode(edge.getSourceId());
            if (sourceNode == null || !(sourceNode.getType().equals(NodeType.LIST) || sourceNode.getType().equals(NodeType.DETAILS))) {
                continue; // Delete pattern should be triggered from a List or Detail
            }

            // Check if pattern matches
            if (validateDeletePattern(pageGraph, action, actionId, sourceNode, edge)) {
                matched.add(edge);
                FunctionalPatternMatch match = new FunctionalPatternMatch(
                        getName(),
                        matched);
                matches.add(match);
            }
        }
    }

    /**
     * Check if an action is a delete action
     * An action is considered a delete action if it contains a Delete operation
     * 
     * @param action The ActionDefinition to check
     * @return true if the action contains a Delete operation
     */
    private boolean isDeleteAction(ActionDefinition action) {
        for (OperationComponent op : action.getOperationComponents()) {
            if ("Delete".equals(op.getType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validate that the delete pattern is complete
     * - The action has input parameters for the item to delete
     * - The action has a Delete operation
     * - The Delete operation has success and error flows
     * - Both flows lead to output ports
     * 
     * @param pageGraph        The page graph
     * @param action           The action definition
     * @param formNode         The form node that triggers delete
     * @param formToActionEdge The edge from form to action
     * @return true if the pattern is valid
     */
    private boolean validateDeletePattern(IFMLGraph pageGraph, ActionDefinition action, String actionId,
            GraphNode formNode, Edge edge) {

        // Check: Action has at least 1 input parameter (the item to delete)
        if (action.getInputParameters().size() < 1) {
            return false;
        }

        // Check: Action has success and error output ports
        if (action.getSuccessOutputPorts().isEmpty() || action.getErrorOutputPorts().isEmpty()) {
            return false;
        }

        // Check: Action contains Delete operation
        OperationComponent deleteOp = null;
        for (OperationComponent op : action.getOperationComponents()) {
            if ("Delete".equals(op.getType())) {
                deleteOp = op;
                break;
            }
        }

        if (deleteOp == null) {
            return false;
        }

        // Check: Delete operation has both success and error flows
        boolean hasSuccessFlow = false;
        boolean hasErrorFlow = false;

        for (ComponentFlow flow : deleteOp.getFlows()) {
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
        List<Edge> eventFlows = action.getErrorEvents(actionId) != null ? action.getErrorEvents(actionId).stream()
                .flatMap(event -> event.getNavigationFlows().stream())
                .toList() : List.of();

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
