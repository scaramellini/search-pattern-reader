package globalGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Collection;

public class IFMLGraph {

    private final Map<String, GraphNode> nodes = new HashMap<>();
    private final List<Edge> edges = new ArrayList<>();

    private final Map<String, List<Edge>> outgoing = new HashMap<>();
    private final Map<String, List<Edge>> incoming = new HashMap<>();

    public void addNode(GraphNode node) {
        nodes.put(node.getId(), node);
        outgoing.putIfAbsent(node.getId(), new ArrayList<>());
        incoming.putIfAbsent(node.getId(), new ArrayList<>());
    }

    public void addEdge(Edge edge) {
        if (!nodes.containsKey(edge.getSourceId()) ||
            !nodes.containsKey(edge.getTargetId())) {
            return; // Ignore edges with missing nodes
        }

        edges.add(edge);
        outgoing.get(edge.getSourceId()).add(edge);
        incoming.get(edge.getTargetId()).add(edge);
    }

    public GraphNode getNode(String id) {
        return nodes.get(id);
    }

    public List<Edge> getOutgoing(String nodeId) {
        return outgoing.getOrDefault(nodeId, Collections.emptyList());
    }

    public List<Edge> getIncoming(String nodeId) {
        return incoming.getOrDefault(nodeId, Collections.emptyList());
    }

    public Collection<GraphNode> getAllNodes() {
        return nodes.values();
    }

    public List<Edge> getAllEdges() {
        return edges;
    }
}
