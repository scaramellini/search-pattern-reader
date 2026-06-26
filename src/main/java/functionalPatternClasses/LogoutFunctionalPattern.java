package functionalPatternClasses;

import java.util.ArrayList;
import java.util.List;

import globalGraph.ActionDefinition;
import globalGraph.Edge;
import globalGraph.GraphNode;
import globalGraph.IFMLGraph;
import globalGraph.NodeType;
import globalGraph.OperationComponent;
import it.davide.xml.ActionRegistry;
import it.davide.xml.FunctionalPatternInterface;
import it.davide.xml.FunctionalPatternMatch;

public class LogoutFunctionalPattern extends FunctionalPatternInterface {

    public LogoutFunctionalPattern() {
        this.name = "Logout Functional Pattern";
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

            // Check if this action is a logout action
            if (!isLogoutAction(action)) {
                continue;
            }

            // Try to construct the full logout pattern
            GraphNode sourceNode = pageGraph.getNode(edge.getSourceId());

            if (sourceNode == null || (!sourceNode.getType().equals(NodeType.FORM)
                    && !sourceNode.getType().equals(NodeType.MYPROFILE)
                    && !sourceNode.getType().equals(NodeType.VIEW_COMPONENT))) {
                continue; // logout pattern should be triggered from a Form or MyProfile component
            }

            matched.add(edge);

            FunctionalPatternMatch match = new FunctionalPatternMatch(
                    getName(),
                    matched);
            matches.add(match);

        }
    }

    /**
     * Check if an action is a logout action
     * An action is considered a logout action if it contains a Logout operation
     * 
     * @param action The ActionDefinition to check
     * @return true if the action contains a Logout operation
     */
    private boolean isLogoutAction(ActionDefinition action) {
        for (OperationComponent op : action.getOperationComponents()) {
            if ("Logout".equals(op.getType())) {
                return true;
            }
        }
        return false;
    }
}
