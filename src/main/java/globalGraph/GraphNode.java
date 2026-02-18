package globalGraph;

public class GraphNode {

    private final String id;
    private final NodeType type;
    private final String pageId;

    public GraphNode(String id, NodeType type, String pageId) {
        this.id = id;
        this.type = type;
        this.pageId = pageId;
    }

    public String getId() { return id; }
    public NodeType getType() { return type; }
    public String getPageId() { return pageId; }
}
