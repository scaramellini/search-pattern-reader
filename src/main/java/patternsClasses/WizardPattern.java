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

            List<Edge> edges = extractForwardEdges(graph, path);

            instances.add(new PatternInstance(edges));
        }

        return instances.isEmpty() ? null : instances;
    }

    // --------------------------------------------------
    // VALIDAZIONI
    // --------------------------------------------------

    private boolean isValidWizardPath(List<GraphNode> path) {

        // Tutti FORM
        for (GraphNode node : path) {
            if (node.getType() != NodeType.FORM)
                return false;
        }

        return true;
    }

    private boolean isMultiPage(List<GraphNode> path) {

        return path.stream()
                .map(GraphNode::getPageId)
                .distinct()
                .count() == path.size();
    }

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

    // --------------------------------------------------
    // EDGE LOGIC
    // --------------------------------------------------

    private Edge findEdge(IFMLGraph graph,
            GraphNode from,
            GraphNode to) {

        for (Edge edge : graph.getOutgoing(from.getId())) {
            if (edge.getTargetId().equals(to.getId())) {
                return edge;
            }
        }

        return null;
    }

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

        // incrocio corretto
        return forwardSources.equals(backwardTargets)
                && forwardTargets.equals(backwardSources);
    }

    private Set<String> extractFieldIds(List<String> attributes) {

        return attributes.stream()
                .filter(Objects::nonNull)
                .map(this::extractBetweenBraces)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private String extractBetweenBraces(String value) {

        if (value.contains("{") && value.contains("}")) {
            return value.substring(
                    value.indexOf("{") + 1,
                    value.indexOf("}"));
        }

        return null;
    }

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

    // --------------------------------------------------
    // JSON EXPORT
    // --------------------------------------------------

    @Override
    public void createJsonPattern(ProjectPatternsJson projectJson,
            PatternInstance instance,
            IFMLGraph graph) {

        ProjectPatternsJson.PatternEntry entry = new ProjectPatternsJson.PatternEntry();

        entry.patternType = name;

        for (Edge edge : instance.getEdges()) {

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

        projectJson.patterns.add(entry);
    }

    private ProjectPatternsJson.Endpoint buildEndpoint(GraphNode node) {

        ProjectPatternsJson.Endpoint ep = new ProjectPatternsJson.Endpoint();

        ep.id = node.getId();
        ep.type = node.getType().name();
        ep.pageId = node.getPageId();

        return ep;
    }
}