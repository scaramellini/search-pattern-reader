package functionalPatternClasses;

import globalGraph.*;
import it.davide.xml.ActionRegistry;
import it.davide.xml.FunctionalPatternInterface;
import it.davide.xml.FunctionalPatternMatch;
import it.davide.xml.utilityTools;

import java.util.ArrayList;
import java.util.List;

/**
 * LoginFunctionalPattern detects the functional login pattern.
 * 
 * This pattern occurs when:
 * 1. A Form in a page has a NavigationFlow pointing to an Action
 * 2. The Action contains a Login operation
 * 3. The Login operation has success and error flows
 * 4. The Action's output ports are connected to navigation flows leading to
 * other pages
 * 
 * The pattern structure is:
 * Form (page) -> NavigationFlow -> Action SignIn -> InputPort
 * -> Login Operation
 * -> SuccessPort -> NavigationFlow -> Success Page
 * -> ErrorPort -> NavigationFlow -> Error Page
 */
public class LoginFunctionalPattern extends FunctionalPatternInterface {

    public LoginFunctionalPattern() {
        this.name = "Login Functional Pattern";
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

            // Check if this action is a login action
            if (!isLoginAction(action)) {
                continue;
            }

            // Try to construct the full login pattern
            GraphNode sourceNode = pageGraph.getNode(edge.getSourceId());
            if (sourceNode == null || !sourceNode.getType().equals(NodeType.FORM)) {
                continue; // Login pattern should be triggered from a Form
            }

            if (!utilityTools.checkFields(sourceNode)) {
                continue;
            }

            // Check if pattern matches
            if (validateLoginPattern(pageGraph, action, sourceNode, edge)) {
                matched.add(edge);
                FunctionalPatternMatch match = new FunctionalPatternMatch(
                        getName(),
                        matched);
                matches.add(match);
            }
        }
    }

    /**
     * Check if an action is a login action
     * An action is considered a login action if it contains a Login operation
     * 
     * @param action The ActionDefinition to check
     * @return true if the action contains a Login operation
     */
    private boolean isLoginAction(ActionDefinition action) {
        for (OperationComponent op : action.getOperationComponents()) {
            if ("Login".equals(op.getType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validate that the login pattern is complete
     * - The action has input parameters for username/password
     * - The action has a Login operation
     * - The Login operation has success and error flows
     * - Both flows lead to output ports
     * 
     * @param pageGraph        The page graph
     * @param action           The action definition
     * @param formNode         The form node that triggers login
     * @param formToActionEdge The edge from form to action
     * @return true if the pattern is valid
     */
    private boolean validateLoginPattern(IFMLGraph pageGraph, ActionDefinition action,
            GraphNode formNode, Edge formToActionEdge) {

        // Check: Action has at least 2 input parameters (username, password)
        if (action.getInputParameters().size() < 2) {
            return false;
        }

        // Check: Action has success and error output ports
        if (action.getSuccessOutputPorts().isEmpty() || action.getErrorOutputPorts().isEmpty()) {
            return false;
        }

        // Check: Action contains Login operation
        OperationComponent loginOp = null;
        for (OperationComponent op : action.getOperationComponents()) {
            if ("Login".equals(op.getType())) {
                loginOp = op;
                break;
            }
        }

        if (loginOp == null) {
            return false;
        }

        // Check: Login operation has both success and error flows
        boolean hasSuccessFlow = false;
        int errorFlowCounter = 0;

        for (ComponentFlow flow : loginOp.getFlows()) {
            if ("SuccessFlow".equals(flow.getType())) {
                hasSuccessFlow = true;
            } else if ("ErrorFlow".equals(flow.getType())) {
                errorFlowCounter++;
            }
        }

        if (!hasSuccessFlow || errorFlowCounter < 3) {
            return false;
        }

        if(!utilityTools.checkInputPortBindings(formToActionEdge, action)) {
            return false;
        }

        // Check that the Error flows of the action leads to a message component or an
        List<Edge> errorEventFlows = action.getErrorEventsMap() != null ? action.getErrorEventsMap().values().stream()
                .flatMap(event -> event.getNavigationFlows().stream())
                .toList() : List.of();

        for (Edge errorFlow : errorEventFlows) {
            GraphNode targetNode = pageGraph.getNode(errorFlow.getTargetId());
            if (targetNode != null && !targetNode.getType().equals(NodeType.MESSAGE)) {
                return false;
            }
        }

        // All validation passed
        return true;
    }
}
