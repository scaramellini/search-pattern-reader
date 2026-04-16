package globalGraph;

//class representing a node in the global graph, it corresponds to a viewcomponent
public class GraphNode {

    private final String id;
    private final NodeType type;
    private final String pageId;

    public GraphNode(String id, NodeType type, String pageId) {
        this.id = id;
        this.type = type;
        this.pageId = pageId;
    }

    /** 
     * @return String
     */
    public String getId() {
        return id;
    }

    /** 
     * @return NodeType
     */
    public NodeType getType() {
        return type;
    }

    /** 
     * @return String
     */
    public String getPageId() {
        return pageId;
    }
}
