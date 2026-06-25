package globalGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Collection;

//class representing the global graph, it contains all the nodes and edges of the graph, reconstructing the IFML model
public class IFMLGraph {

    private final Map<String, GraphNode> nodes = new HashMap<>();
    private final List<Edge> edges = new ArrayList<>();

    private final Map<String, List<Edge>> outgoing = new HashMap<>();
    private final Map<String, List<Edge>> incoming = new HashMap<>();

    /** 
     * @param node
     */
    public void addNode(GraphNode node) {
        nodes.put(node.getId(), node);
        outgoing.putIfAbsent(node.getId(), new ArrayList<>());
        incoming.putIfAbsent(node.getId(), new ArrayList<>());
    }

    /** 
     * @param edge
     */
    public void addEdge(Edge edge) {
        if (edge.getSourceId() == null || !nodes.containsKey(edge.getSourceId())) {
            return; // Ignore edges with missing source node
        }

        if (edge.getTargetId() == null) {
            return; // Ignore edges with missing target ID
        }

        edges.add(edge);
        outgoing.get(edge.getSourceId()).add(edge);
        incoming.putIfAbsent(edge.getTargetId(), new ArrayList<>());
        incoming.get(edge.getTargetId()).add(edge);
    }

    public void replaceEdge(Edge oldEdge, Edge newEdge) {
        if (!edges.remove(oldEdge)) {
            return;
        }

        List<Edge> oldOutgoing = outgoing.get(oldEdge.getSourceId());
        if (oldOutgoing != null) {
            oldOutgoing.remove(oldEdge);
        }

        if (oldEdge.getTargetId() != null) {
            List<Edge> oldIncoming = incoming.get(oldEdge.getTargetId());
            if (oldIncoming != null) {
                oldIncoming.remove(oldEdge);
            }
        }

        addEdge(newEdge);
    }

    /** 
     * @param id
     * @return GraphNode
     */
    public GraphNode getNode(String id) {
        return nodes.get(id);
    }

    /** 
     * @param nodeId
     * @return List<Edge>
     */
    public List<Edge> getOutgoing(String nodeId) {
        return outgoing.getOrDefault(nodeId, Collections.emptyList());
    }

    /** 
     * @param nodeId
     * @return List<Edge>
     */
    public List<Edge> getIncoming(String nodeId) {
        return incoming.getOrDefault(nodeId, Collections.emptyList());
    }

    /** 
     * @return Collection<GraphNode>
     */
    public Collection<GraphNode> getAllNodes() {
        return nodes.values();
    }

    /** 
     * @return List<Edge>
     */
    public List<Edge> getAllEdges() {
        return edges;
    }
}
