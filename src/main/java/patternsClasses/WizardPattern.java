package patternsClasses;

import globalGraph.*;
import it.davide.xml.ProjectPatternsJson;
import it.davide.xml.GraphTraversal;
import it.davide.xml.PatternInstance;

import java.util.*;
import java.util.stream.Collectors;

public class WizardPattern extends GenericGraphPattern {

    public WizardPattern() {
        this.name = "Wizard Pattern";
    }

    /** 
     * @param graph
     * @param startNode
     * @return List<PatternInstance>
     */
    @Override
    public List<PatternInstance> matches(IFMLGraph graph,
            GraphNode startNode) {

        if (startNode.getType() != NodeType.FORM)
            return null;

        GraphTraversal traversal = new GraphTraversal(graph);
        List<List<GraphNode>> allPaths = traversal.dfsPaths(startNode, 0);

        List<PatternInstance> instances = new ArrayList<>();

        for (List<GraphNode> path : allPaths) {

            if (path.size() < 2)
                continue;

            if (!isValidWizardPath(path))
                continue;

            if (!isMultiPage(path))
                continue;

            if (!hasValidForwardBackward(graph, path))
                continue;

            if (!lastNodeHasFinalOutgoing(graph, path))
                continue;

            List<Edge> forwardEdges = extractForwardEdges(graph, path);

            instances.add(new PatternInstance(forwardEdges, null, null));
        }

        return instances.isEmpty() ? null : instances;
    }

    /** 
     * @param path
     * @return boolean
     */
    private boolean isValidWizardPath(List<GraphNode> path) {

        for (GraphNode node : path) {
            if (node.getType() != NodeType.FORM)
                return false;
        }

        return true;
    }

    /** 
     * @param path
     * @return boolean
     */
    private boolean isMultiPage(List<GraphNode> path) {

        return path.stream()
                .map(GraphNode::getPageId)
                .distinct()
                .count() == path.size();
    }

    /** 
     * @param graph
     * @param path
     * @return boolean
     */
    private boolean hasValidForwardBackward(IFMLGraph graph,
            List<GraphNode> path) {

        for (int i = 0; i < path.size() - 1; i++) {

            GraphNode from = path.get(i);
            GraphNode to = path.get(i + 1);

            Edge forward = findEdge(graph, from, to);
            Edge backward = findEdge(graph, to, from);

            if (forward == null || backward == null)
                return false;

            if (!sameBindings(forward, backward))
                return false;
        }

        return true;
    }

    /** 
     * @param graph
     * @param path
     * @return boolean
     */
    private boolean lastNodeHasFinalOutgoing(IFMLGraph graph,
            List<GraphNode> path) {

        GraphNode last = path.get(path.size() - 1);

        for (Edge outgoing : graph.getOutgoing(last.getId())) {

            GraphNode target = graph.getNode(outgoing.getTargetId());

            if (target.getType() == NodeType.FORM) {

                Edge backward = findEdge(graph, target, last);

                if (backward != null && sameBindings(outgoing, backward)) {
                    continue;
                }
            }

            return true;
        }

        return false;
    }

    /** 
     * @param graph
     * @param from
     * @param to
     * @return Edge
     */
    private Edge findEdge(IFMLGraph graph,
            GraphNode from,
            GraphNode to) {

        for (Edge edge : graph.getOutgoing(from.getId())) {
            if (edge.getTargetId().equals(to.getId()))
                return edge;
        }

        return null;
    }

    /** 
     * @param forward
     * @param backward
     * @return boolean
     */
    private boolean sameBindings(Edge forward,
            Edge backward) {

        if (forward.getBindings().size() != backward.getBindings().size())
            return false;

        Set<String> forwardSources = extractFieldIds(
                forward.getBindings().stream()
                        .map(EdgeBinding::getSourceAttribute)
                        .collect(Collectors.toList()));

        Set<String> forwardTargets = extractFieldIds(
                forward.getBindings().stream()
                        .map(EdgeBinding::getTargetAttribute)
                        .collect(Collectors.toList()));

        Set<String> backwardSources = extractFieldIds(
                backward.getBindings().stream()
                        .map(EdgeBinding::getSourceAttribute)
                        .collect(Collectors.toList()));

        Set<String> backwardTargets = extractFieldIds(
                backward.getBindings().stream()
                        .map(EdgeBinding::getTargetAttribute)
                        .collect(Collectors.toList()));

        return forwardSources.equals(backwardTargets)
                && forwardTargets.equals(backwardSources);
    }

    /** 
     * @param attributes
     * @return Set<String>
     */
    private Set<String> extractFieldIds(List<String> attributes) {

        return attributes.stream()
                .filter(Objects::nonNull)
                .map(this::extractBetweenBraces)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /** 
     * @param value
     * @return String
     */
    private String extractBetweenBraces(String value) {

        if (value.contains("{") && value.contains("}")) {
            return value.substring(
                    value.indexOf("{") + 1,
                    value.indexOf("}"));
        }

        return null;
    }

    /** 
     * @param graph
     * @param path
     * @return List<Edge>
     */
    private List<Edge> extractForwardEdges(IFMLGraph graph,
            List<GraphNode> path) {

        List<Edge> edges = new ArrayList<>();

        for (int i = 0; i < path.size() - 1; i++) {

            Edge edge = findEdge(graph,
                    path.get(i),
                    path.get(i + 1));

            if (edge != null)
                edges.add(edge);
        }

        return edges;
    }

    /** 
     * @param projectJson
     * @param instance
     * @param graph
     */
    @Override
    public void createJsonPattern(ProjectPatternsJson projectJson,
            PatternInstance instance,
            IFMLGraph graph) {

        ProjectPatternsJson.PatternEntry entry = new ProjectPatternsJson.PatternEntry();

        entry.patternType = name;

        Set<String> alreadyAdded = new HashSet<>();

        for (Edge edge : instance.getEdges()) {

            addFlowToJson(entry, edge, graph, alreadyAdded);

            GraphNode from = graph.getNode(edge.getSourceId());
            GraphNode to = graph.getNode(edge.getTargetId());

            Edge backward = findEdge(graph, to, from);

            if (backward != null)
                addFlowToJson(entry, backward, graph, alreadyAdded);
        }

        projectJson.patterns.add(entry);
    }

    /** 
     * @param entry
     * @param edge
     * @param graph
     * @param alreadyAdded
     */
    private void addFlowToJson(ProjectPatternsJson.PatternEntry entry,
            Edge edge,
            IFMLGraph graph,
            Set<String> alreadyAdded) {

        String key = edge.getSourceId() + "->" + edge.getTargetId();

        if (alreadyAdded.contains(key))
            return;

        alreadyAdded.add(key);

        GraphNode from = graph.getNode(edge.getSourceId());
        GraphNode to = graph.getNode(edge.getTargetId());

        ProjectPatternsJson.FlowEntry flow = new ProjectPatternsJson.FlowEntry();

        flow.from = buildEndpoint(from);
        flow.to = buildEndpoint(to);

        for (EdgeBinding b : edge.getBindings()) {

            ProjectPatternsJson.BindingEntry jsonBinding = new ProjectPatternsJson.BindingEntry();

            jsonBinding.automaticCoupling = b.isAutomaticCoupling();

            if (!b.isAutomaticCoupling()) {
                jsonBinding.source = b.getSourceAttribute();
                jsonBinding.target = b.getTargetAttribute();
            }

            flow.bindings.add(jsonBinding);
        }

        entry.flows.add(flow);
    }

    /** 
     * @param node
     * @return Endpoint
     */
    private ProjectPatternsJson.Endpoint buildEndpoint(GraphNode node) {

        ProjectPatternsJson.Endpoint ep = new ProjectPatternsJson.Endpoint();

        ep.id = node.getId();
        ep.type = node.getType().name();
        ep.pageId = node.getPageId();
        ep.dataBinding = node.getObjectId();

        return ep;
    }
}