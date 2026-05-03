package globalGraph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

//class representing a node in the global graph, it corresponds to a viewcomponent
public class GraphNode {

    private final String id;
    private final NodeType type;
    private final String pageId;
    private final String objectId;

    private Map<FieldElementCategory, Set<FieldInfo>> fieldElementIds = new HashMap<>();
    private Map<ConditionalExpressionCategory, Set<String>> conditionalExpressions = new HashMap<>();

    public static class FieldInfo {
        private String id;
        private String valueAttribute;
        private String valueAssociationAttribute;

        public FieldInfo(String id, String valueAttribute, String valueAssociationAttribute) {
            this.id = id;
            this.valueAttribute = valueAttribute;
            this.valueAssociationAttribute = valueAssociationAttribute;
        }

        public String getId() {
            return id;
        }

        public String getValueAttribute() {
            return valueAttribute;
        }

        public String getValueAssociationAttribute() {
            return valueAssociationAttribute;
        }
    }

    public static enum FieldElementCategory {
        Field,
        SelectionField
    }

    public static enum ConditionalExpressionCategory {
        associationCondition,
        attributeConditions,
        keyCondition
    }

    public GraphNode(String id, NodeType type, String pageId, String objectId,
            Map<FieldElementCategory, Set<FieldInfo>> fieldElementIds,
            Map<ConditionalExpressionCategory, Set<String>> conditionalExpressions) {
        this.id = id;
        this.type = type;
        this.pageId = pageId;
        this.objectId = objectId;
        this.fieldElementIds = fieldElementIds;
        this.conditionalExpressions = conditionalExpressions;   
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

    /**
     * @return String
     */
    public String getObjectId() {
        return objectId;
    }

    public Map<FieldElementCategory, Set<FieldInfo>> getFieldElementIds() {
        return fieldElementIds;
    }

    public Map<ConditionalExpressionCategory, Set<String>> getConditionalExpressions() {
        return conditionalExpressions;
    }
}
