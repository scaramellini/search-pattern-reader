package globalGraph;

public class Edge {

    private final String sourceId;
    private final String targetId;
    private final FlowType type;

    public Edge(String sourceId, String targetId, FlowType type) {
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.type = type;
    }

    public String getSourceId() { return sourceId; }
    public String getTargetId() { return targetId; }
    public FlowType getType() { return type; }
}
