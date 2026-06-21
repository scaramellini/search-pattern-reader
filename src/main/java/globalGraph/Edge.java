package globalGraph;

import java.util.ArrayList;
import java.util.List;

//class representing the edge between two nodes in the global graph, it corresponds to a flow between two components
public class Edge {

    private final String sourceId;
    private final String targetId;
    private final FlowType type;
    private final boolean fieldTriggered;
    
    private final String actionId;  // if target is an external Action
    private final String targetActionInputPort;  // which InputPort of the Action receives data

    private final List<EdgeBinding> bindings = new ArrayList<>();

    public Edge(String sourceId, String targetId, FlowType type, boolean fieldTriggered) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.type = type;
        this.fieldTriggered = fieldTriggered;
        this.actionId = null;
        this.targetActionInputPort = null;
    }
    
    /**
     * Constructor for edges that point to an external Action
     */
    public Edge(String sourceId, String actionId, String targetActionInputPort, FlowType type) {
        this.sourceId = sourceId;
        this.targetId = actionId;  // target is the action node id
        this.actionId = actionId;
        this.targetActionInputPort = targetActionInputPort;
        this.type = type;
        this.fieldTriggered = false;
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

    /**
     * @return String - action ID if this edge points to an external Action, null otherwise
     */
    public String getActionId() {
        return actionId;
    }

    /**
     * @return String - target input port of the Action, null if not applicable
     */
    public String getTargetActionInputPort() {
        return targetActionInputPort;
    }

    /**
     * @return boolean - true if this edge points to an external Action
     */
    public boolean pointsToAction() {
        return actionId != null;
    }

    
}
