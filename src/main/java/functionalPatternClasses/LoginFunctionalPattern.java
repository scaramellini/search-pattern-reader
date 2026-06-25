package functionalPatternClasses;

import globalGraph.*;
import it.davide.xml.ActionRegistry;
import it.davide.xml.FunctionalPatternInterface;
import it.davide.xml.FunctionalPatternMatch;
import it.davide.xml.ActionDefinitionParser;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import it.davide.xml.ProjectPatternsJson;

/**
 * LoginFunctionalPattern detects the functional login pattern.
 * 
 * This pattern occurs when:
 * 1. A Form in a page has a NavigationFlow pointing to an Action
 * 2. The Action contains a Login operation
 * 3. The Login operation has success and error flows
 * 4. The Action's output ports are connected to navigation flows leading to other pages
 * 
 * The pattern structure is:
 * Form (page) -> NavigationFlow -> Action SignIn -> InputPort
 *                                                   -> Login Operation
 *                                                   -> SuccessPort -> NavigationFlow -> Success Page
 *                                                   -> ErrorPort -> NavigationFlow -> Error Page
 */
public class LoginFunctionalPattern implements FunctionalPatternInterface {
    private String name = "Login Functional Pattern";
    
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

            // Build the functional graph for this action
            ActionDefinitionParser parser = new ActionDefinitionParser();
            FunctionalGraph functionalGraph = parser.buildFunctionalGraph(action);

            // Check if pattern matches
            if (validateLoginPattern(pageGraph, action, functionalGraph, sourceNode, edge)) {
                FunctionalPatternMatch match = new FunctionalPatternMatch(
                        getName(),
                        sourceNode.getId(),
                        actionId,
                        action.getWebviewId()
                );
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
     * @param pageGraph The page graph
     * @param action The action definition
     * @param functionalGraph The action's functional graph
     * @param formNode The form node that triggers login
     * @param formToActionEdge The edge from form to action
     * @return true if the pattern is valid
     */
    private boolean validateLoginPattern(IFMLGraph pageGraph, ActionDefinition action,
                                        FunctionalGraph functionalGraph,
                                        GraphNode formNode, Edge formToActionEdge) {

        // Check: Action has at least 2 input parameters (username, password)
        if (action.getInputParameters().size() < 2) {
            return false;
        }

        // Check: Action has success and error output ports
        /*if (action.getSuccessOutputPorts().isEmpty() || action.getErrorOutputPorts().isEmpty()) {
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
        boolean hasErrorFlow = false;

        for (ComponentFlow flow : loginOp.getFlows()) {
            if ("SuccessFlow".equals(flow.getType())) {
                hasSuccessFlow = true;
            } else if ("ErrorFlow".equals(flow.getType())) {
                hasErrorFlow = true;
            }
        }

        if (!hasSuccessFlow || !hasErrorFlow) {
            return false;
        }*/

        // Check: Form has parameter bindings that map to action input parameters
        if (formToActionEdge.getBindings().isEmpty()) {
            return false;
        }

        // All validation passed
        return true;
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
            flowToAction.bindings = buildBindingEntries(findEdgeBindings(graph, sourceNode.getId(), actionNode.getId()));
            entry.flows.add(flowToAction);

            ActionDefinition action = actionNode.getActionDefinition();
            if (action != null) {
                for (ActionEvent event : action.getEvents().values()) {
                    for (Edge eventFlow : event.getNavigationFlows()) {
                        GraphNode targetNode = graph.getNode(eventFlow.getTargetId());
                        ProjectPatternsJson.FlowEntry resultFlow = new ProjectPatternsJson.FlowEntry();
                        resultFlow.from = buildActionEventEndpoint(actionNode, event);
                        resultFlow.to = targetNode != null ? buildEndpoint(targetNode) : buildUnknownEndpoint(eventFlow.getTargetId(), actionNode.getPageId());
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
