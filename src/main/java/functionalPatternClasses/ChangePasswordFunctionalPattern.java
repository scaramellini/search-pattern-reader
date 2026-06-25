package functionalPatternClasses;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import globalGraph.ActionDefinition;
import globalGraph.ActionEvent;
import globalGraph.ComponentFlow;
import globalGraph.Edge;
import globalGraph.EdgeBinding;
import globalGraph.GraphNode;
import globalGraph.IFMLGraph;
import globalGraph.NodeType;
import globalGraph.OperationComponent;
import it.davide.xml.ActionRegistry;
import it.davide.xml.FunctionalPatternInterface;
import it.davide.xml.FunctionalPatternMatch;
import it.davide.xml.ProjectPatternsJson;

public class ChangePasswordFunctionalPattern implements FunctionalPatternInterface {
    private String name = "Change Password Functional Pattern";

    private List<FunctionalPatternMatch> matches = new ArrayList<>();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void detect(IFMLGraph pageGraph, ActionRegistry actionRegistry) {
        matches.clear();

        // For each edge in the page graph that points to an action
        for (Edge edge : pageGraph.getAllEdges()) {
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
                FunctionalPatternMatch match = new FunctionalPatternMatch(
                        getName(),
                        sourceNode.getId(),
                        actionId,
                        action.getWebviewId());
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

        if(eventFlows.size() < 1) {
            return false;
        }

        for(Edge flow : eventFlows) {
            GraphNode targetNode = pageGraph.getNode(flow.getTargetId());
            if (targetNode != null && !targetNode.getType().equals(NodeType.MESSAGE)) {
                return false;
            }
        }

        // All validation passed
        return true;
    }

    @Override
    public List<FunctionalPatternMatch> getMatches() {
        return matches;
    }

    @Override
    public void createJsonPattern(ProjectPatternsJson projectJson, IFMLGraph graph) {
        for (FunctionalPatternMatch match : matches) {
            GraphNode sourceNode = graph.getNode(match.getSourceComponentId());
            GraphNode actionNode = graph.getNode(match.getActionId());

            if (sourceNode == null || actionNode == null) {
                continue;
            }

            ProjectPatternsJson.PatternEntry entry = new ProjectPatternsJson.PatternEntry();
            entry.patternType = getName();
            entry.fields = buildFieldsEndpoint(sourceNode);

            ProjectPatternsJson.FlowEntry flowToAction = new ProjectPatternsJson.FlowEntry();
            flowToAction.from = buildEndpoint(sourceNode);
            flowToAction.to = buildEndpoint(actionNode);
            flowToAction.bindings = buildBindingEntries(
                    findEdgeBindings(graph, sourceNode.getId(), actionNode.getId()));
            entry.flows.add(flowToAction);

            ActionDefinition action = actionNode.getActionDefinition();
            if (action != null) {
                for (ActionEvent event : action.getAllEvents()) {
                    for (Edge eventFlow : event.getNavigationFlows()) {
                        GraphNode targetNode = graph.getNode(eventFlow.getTargetId());
                        boolean isPage = eventFlow.getTargetId().contains("page");
                        ProjectPatternsJson.FlowEntry resultFlow = new ProjectPatternsJson.FlowEntry();
                        resultFlow.from = buildActionEventEndpoint(actionNode, event);
                        resultFlow.to = targetNode != null ? buildEndpoint(targetNode)
                                : buildUnknownEndpoint(eventFlow.getTargetId(), actionNode.getPageId(), isPage);
                        resultFlow.bindings = buildBindingEntries(eventFlow.getBindings());
                        entry.flows.add(resultFlow);
                    }
                }
            }

            projectJson.patterns.add(entry);
        }
    }

    private ProjectPatternsJson.Endpoint buildActionEventEndpoint(GraphNode actionNode, ActionEvent event) {
        ProjectPatternsJson.Endpoint ep = new ProjectPatternsJson.Endpoint();
        ep.id = event.getId();
        ep.type = event.getType();
        ep.pageId = actionNode.getPageId();
        ep.dataBinding = actionNode.getId();
        ep.isInDialogPage = false;
        return ep;
    }

    private ProjectPatternsJson.Endpoint buildUnknownEndpoint(String id, String pageId, boolean isPage) {
        ProjectPatternsJson.Endpoint ep = new ProjectPatternsJson.Endpoint();
        ep.id = id;
        ep.type = isPage ? "PAGE" : "UNKNOWN";
        ep.pageId = pageId;
        ep.dataBinding = null;
        ep.isInDialogPage = false;
        return ep;
    }

    private List<ProjectPatternsJson.BindingEntry> buildBindingEntries(List<EdgeBinding> bindings) {
        List<ProjectPatternsJson.BindingEntry> entries = new ArrayList<>();

        for (EdgeBinding binding : bindings) {
            ProjectPatternsJson.BindingEntry jsonBinding = new ProjectPatternsJson.BindingEntry();
            jsonBinding.automaticCoupling = binding.isAutomaticCoupling();
            if (!binding.isAutomaticCoupling()) {
                jsonBinding.source = binding.getSourceAttribute();
                jsonBinding.target = binding.getTargetAttribute();
            }
            entries.add(jsonBinding);
        }

        return entries;
    }

    private List<EdgeBinding> findEdgeBindings(IFMLGraph graph, String sourceId, String targetId) {
        for (Edge edge : graph.getOutgoing(sourceId)) {
            if (targetId.equals(edge.getTargetId()) && edge.pointsToAction()) {
                return edge.getBindings();
            }
        }
        return List.of();
    }

    private List<ProjectPatternsJson.FieldEndpoint> buildFieldsEndpoint(GraphNode node) {
        Set<GraphNode.FieldInfo> fields = new HashSet<>();
        if (node.getFieldElementIds() != null) {
            for (Set<GraphNode.FieldInfo> set : node.getFieldElementIds().values()) {
                if (set != null) {
                    fields.addAll(set);
                }
            }
        }

        List<ProjectPatternsJson.FieldEndpoint> fieldEndpoints = new ArrayList<>();
        for (GraphNode.FieldInfo field : fields) {
            ProjectPatternsJson.FieldEndpoint ep = new ProjectPatternsJson.FieldEndpoint();
            ep.fieldId = field.getId();
            ep.valueAttribute = field.getValueAttribute();
            ep.valueAssociationRole = field.getValueAssociationAttribute();
            fieldEndpoints.add(ep);
        }
        return fieldEndpoints;
    }

    private ProjectPatternsJson.Endpoint buildEndpoint(GraphNode node) {
        ProjectPatternsJson.Endpoint ep = new ProjectPatternsJson.Endpoint();
        ep.id = node.getId();
        ep.type = node.getType().name();
        ep.pageId = node.getPageId();
        ep.dataBinding = node.getObjectId();
        ep.isInDialogPage = node.isInDialogPage();
        return ep;
    }

}
