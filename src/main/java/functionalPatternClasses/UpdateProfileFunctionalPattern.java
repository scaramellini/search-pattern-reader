package functionalPatternClasses;

import java.util.ArrayList;
import java.util.List;

import globalGraph.ActionDefinition;
import globalGraph.ComponentFlow;
import globalGraph.Edge;
import globalGraph.FlowType;
import globalGraph.GraphNode;
import globalGraph.IFMLGraph;
import globalGraph.NodeType;
import globalGraph.OperationComponent;
import it.davide.xml.ActionRegistry;
import it.davide.xml.FunctionalPatternInterface;
import it.davide.xml.FunctionalPatternMatch;

public class UpdateProfileFunctionalPattern extends FunctionalPatternInterface {
    public UpdateProfileFunctionalPattern() {
        this.name = "Update Profile Functional Pattern";
    }

    @Override
    public void detect(IFMLGraph pageGraph, ActionRegistry actionRegistry) {
        List<GraphNode> components = pageGraph.getNodesByType(NodeType.MYPROFILE);

        for (GraphNode myProfileComponent : components) {
            
            List<Edge> matched = new ArrayList<>();
            
            for (Edge myProfileEdge : pageGraph.getOutgoing(myProfileComponent.getId())) {

                if (myProfileEdge.getType() != FlowType.NAVIGATION)
                    continue;

                GraphNode formComponent = pageGraph.getNode(myProfileEdge.getTargetId());

                if (formComponent == null || formComponent.getType() != NodeType.FORM)
                    continue;

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

                    // Try to construct the full update profile pattern
                    GraphNode sourceNode = pageGraph.getNode(formEdge.getSourceId());
                    if (sourceNode == null || !sourceNode.getType().equals(NodeType.FORM)) {
                        continue; // Update profile pattern should be triggered from a Form
                    }

                    // Check if pattern matches
                    if (validateUpdateProfilePattern(pageGraph, action, sourceNode, formEdge)) {
                        matched.add(myProfileEdge);
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
            if ("UpdateProfile".equals(op.getType())) {
                return true;
            }
        }
        return false;
    }

    private boolean validateUpdateProfilePattern(IFMLGraph pageGraph, ActionDefinition action,
            GraphNode formNode, Edge formToActionEdge) {
        // Check: Action has at least 3 input parameters (email, name, lastname)
        if (action.getInputParameters().size() < 3) {
            return false;
        }

        // Check: Action has success and error output ports
        if (action.getSuccessOutputPorts().isEmpty() || action.getErrorOutputPorts().isEmpty()) {
            return false;
        }

        // Check: Action contains update profile operation
        OperationComponent updateProfileOp = null;
        for (OperationComponent op : action.getOperationComponents()) {
            if ("UpdateProfile".equals(op.getType())) {
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
