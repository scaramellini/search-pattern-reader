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

public class CreateFunctionalPattern extends FunctionalPatternInterface {
    public CreateFunctionalPattern() {
        this.name = "Create Functional Pattern";
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

            if (actionId.contains("act6")) {
                System.out.println("Debug: Found action with ID act6");
            }

            if (action == null) {
                continue;
            }

            // Check if this action is a create action
            if (!isCreateAction(action)) {
                continue;
            }

            // Try to construct the full create pattern
            GraphNode sourceNode = pageGraph.getNode(edge.getSourceId());
            if (sourceNode == null || !sourceNode.getType().equals(NodeType.FORM)) {
                continue; // Create pattern should be triggered from a Form
            }

            if (!utilityTools.checkFields(sourceNode)) {
                continue;
            }

            // Check if pattern matches
            if (validateCreatePattern(pageGraph, action, actionId, sourceNode, edge)) {
                matched.add(edge);
                FunctionalPatternMatch match = new FunctionalPatternMatch(
                        getName(),
                        matched);
                matches.add(match);
            }
        }
    }

    /**
     * Check if an action is a create action
     * An action is considered a create action if it contains a Create operation
     * 
     * @param action The ActionDefinition to check
     * @return true if the action contains a Create operation
     */
    private boolean isCreateAction(ActionDefinition action) {
        for (OperationComponent op : action.getOperationComponents()) {
            if ("Save".equals(op.getType())
                    && ("create".equals(op.getOperationActionType()) || "save".equals(op.getOperationActionType()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validate that the create pattern is complete
     * - The action has input parameters for the item to create
     * - The action has a Create operation
     * - The Create operation has success and error flows
     * - Both flows lead to output ports
     * 
     * @param pageGraph        The page graph
     * @param action           The action definition
     * @param formNode         The form node that triggers create
     * @param formToActionEdge The edge from form to action
     * @return true if the pattern is valid
     */
    private boolean validateCreatePattern(IFMLGraph pageGraph, ActionDefinition action, String actionId,
            GraphNode formNode, Edge edge) {

        // Check: Action has at least 1 input parameter (the item to create)
        if (action.getInputParameters().size() < 1) {
            return false;
        }

        // Check: Action has success and error output ports
        if (action.getSuccessOutputPorts().isEmpty() || action.getErrorOutputPorts().isEmpty()) {
            return false;
        }

        // Check: Action contains Create operation
        OperationComponent createOp = null;
        for (OperationComponent op : action.getOperationComponents()) {
            if ("Save".equals(op.getType())
                    && ("create".equals(op.getOperationActionType()) || "save".equals(op.getOperationActionType()))) {
                createOp = op;
                break;
            }
        }

        if (createOp == null) {
            return false;
        }

        // Check: Create operation has both success
        boolean hasSuccessFlow = false;

        for (ComponentFlow flow : createOp.getFlows()) {
            if ("SuccessFlow".equals(flow.getType())) {
                hasSuccessFlow = true;
            }
        }

        if (!hasSuccessFlow) {
            return false;
        }

        if (!utilityTools.checkInputPortBindings(edge, action)) {
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
