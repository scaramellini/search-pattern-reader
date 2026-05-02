package globalGraph;

import java.util.ArrayList;
import java.util.List;

//class representing the edge between two nodes in the global graph, it corresponds to a flow between two components
public class Edge {

    private final String sourceId;
    private final String targetId;
    private final FlowType type;

    private final List<EdgeBinding> bindings = new ArrayList<>();

    public Edge(String sourceId, String targetId, FlowType type) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.type = type;
    }

    /**
     * @return String
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * @return String
     */
    public String getTargetId() {
        return targetId;
    }

    /**
     * @return FlowType
     */
    public FlowType getType() {
        return type;
    }

    /**
     * @return List<EdgeBinding>
     */
    public List<EdgeBinding> getBindings() {
        return bindings;
    }

    /**
     * @param binding
     */
    public void addBinding(EdgeBinding binding) {
        bindings.add(binding);
    }

    
}
