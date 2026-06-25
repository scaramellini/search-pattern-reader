package globalGraph;

import java.util.ArrayList;
import java.util.List;

//class representing the edge between two nodes in the global graph, it corresponds to a flow between two components
public class Edge {

    private final String sourceId;
    private final String targetId;
    private final FlowType type;
    private final boolean fieldTriggered;
    
    private final List<EdgeBinding> bindings = new ArrayList<>();

    public Edge(String sourceId, String targetId, FlowType type, boolean fieldTriggered) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.type = type;
        this.fieldTriggered = fieldTriggered;
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

    public boolean isFieldTriggered() {
        return fieldTriggered;
    }

    /**
     * @param binding
     */
    public void addBinding(EdgeBinding binding) {
        bindings.add(binding);
    }

    public boolean pointsToAction() {
        return targetId != null && targetId.contains("act");
    }
}
