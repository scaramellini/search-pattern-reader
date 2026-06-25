package functionalPatternClasses;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import globalGraph.ActionDefinition;
import globalGraph.ActionEvent;
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

public class LogoutFunctionalPattern implements FunctionalPatternInterface {
    private String name = "Logout Functional Pattern";

    private List<FunctionalPatternMatch> matches = new ArrayList<>();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<FunctionalPatternMatch> getMatches() {
        return matches;
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

            if(actionId.contains("act59b")) {
                System.out.println("Debug: Found actionId containing 'act59b' in detect");
            }

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
                    && !sourceNode.getType().equals(NodeType.MYPROFILE) && !sourceNode.getType().equals(NodeType.VIEW_COMPONENT))) {
                continue; // logout pattern should be triggered from a Form or MyProfile component
            }

            FunctionalPatternMatch match = new FunctionalPatternMatch(
                    getName(),
                    sourceNode.getId(),
                    actionId,
                    action.getWebviewId());
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

    @Override
    public void createJsonPattern(ProjectPatternsJson projectJson, IFMLGraph graph) {
        for (FunctionalPatternMatch match : matches) {
            GraphNode sourceNode = graph.getNode(match.getSourceComponentId());
            GraphNode actionNode = graph.getNode(match.getActionId());

            if (match.getActionId().contains("act36d")) {
                System.out.println("Debug: Found actionId containing 'act36d' in createJsonPattern");
            }

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
                for (ActionEvent event : action.getEvents().values()) {
                    for (Edge eventFlow : event.getNavigationFlows()) {
                        GraphNode targetNode = graph.getNode(eventFlow.getTargetId());
                        ProjectPatternsJson.FlowEntry resultFlow = new ProjectPatternsJson.FlowEntry();
                        resultFlow.from = buildActionEventEndpoint(actionNode, event);
                        resultFlow.to = targetNode != null ? buildEndpoint(targetNode)
                                : buildUnknownEndpoint(eventFlow.getTargetId(), actionNode.getPageId());
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

    private ProjectPatternsJson.Endpoint buildUnknownEndpoint(String id, String pageId) {
        ProjectPatternsJson.Endpoint ep = new ProjectPatternsJson.Endpoint();
        ep.id = id;
        ep.type = "UNKNOWN";
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
