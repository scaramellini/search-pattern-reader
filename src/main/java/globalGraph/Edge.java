package globalGraph;

import java.util.ArrayList;
import java.util.List;

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

    public String getSourceId() {
        return sourceId;
    }

    public String getTargetId() {
        return targetId;
    }

    public FlowType getType() {
        return type;
    }

    public List<EdgeBinding> getBindings() {
        return bindings;
    }

    public void addBinding(EdgeBinding binding) {
        bindings.add(binding);
    }
}
