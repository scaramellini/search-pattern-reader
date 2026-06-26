package it.davide.xml;

import globalGraph.ActionDefinition;
import globalGraph.ActionEvent;
import globalGraph.Edge;
import globalGraph.EdgeBinding;
import globalGraph.GraphNode;
import globalGraph.IFMLGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Interface for functional patterns.
 * Functional patterns detect complex flows involving both page components and
 * external Actions.
 */
public abstract class FunctionalPatternInterface {

    protected String name;

    protected List<FunctionalPatternMatch> matches = new ArrayList<>();

    public String getName() {
        return name;
    }

    public List<FunctionalPatternMatch> getMatches() {
        return matches;
    }

    /**
     * Detect this pattern in the given graphs
     * 
     * @param pageGraph      The page graph (contains page components and actions)
     * @param actionRegistry The action registry for lookup
     */
    public abstract void detect(IFMLGraph pageGraph, ActionRegistry actionRegistry);

    public void createJsonPattern(ProjectPatternsJson projectJson, IFMLGraph graph) {
        for (FunctionalPatternMatch match : matches) {

            ProjectPatternsJson.PatternEntry entry = new ProjectPatternsJson.PatternEntry();

            for (Edge edge : match.getEdges()) {
                GraphNode sourceNode = graph.getNode(edge.getSourceId());
                GraphNode actionNode = graph.getNode(edge.getTargetId());

                if (sourceNode == null || actionNode == null) {
                    continue;
                }

                
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
