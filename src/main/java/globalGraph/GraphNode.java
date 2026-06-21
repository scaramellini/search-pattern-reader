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
    private final Boolean inDialogPage;
    
    private final boolean isAction;
    private final ActionDefinition actionDefinition;

    private Map<FieldElementCategory, Set<FieldInfo>> fieldElementIds = new HashMap<>();
    private Map<ConditionalExpressionCategory, Set<String>> conditionalExpressions = new HashMap<>();

    public static class FieldInfo {
        private String id;
        private String valueAttribute;
        private String valueAssociationAttribute;
        private String fieldDataBinding;
        

        public FieldInfo(String id, String valueAttribute, String valueAssociationAttribute, String fieldDataBinding) {
            this.id = id;
            this.valueAttribute = valueAttribute;
            this.valueAssociationAttribute = valueAssociationAttribute;
            this.fieldDataBinding = fieldDataBinding;
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

        public String getFieldDataBinding() {
            return fieldDataBinding;
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

    public GraphNode(String id, NodeType type, String pageId, String objectId, Boolean inDialogPage,
            Map<FieldElementCategory, Set<FieldInfo>> fieldElementIds,
            Map<ConditionalExpressionCategory, Set<String>> conditionalExpressions) {
        this.id = id;
        this.type = type;
        this.pageId = pageId;
        this.objectId = objectId;
        this.inDialogPage = inDialogPage;
        this.fieldElementIds = fieldElementIds;
        this.conditionalExpressions = conditionalExpressions;
        this.isAction = false;
        this.actionDefinition = null;
    }

    /**
     * Constructor for nodes representing external Actions
     */
    public GraphNode(String id, ActionDefinition actionDefinition) {
        this.id = id;
        this.actionDefinition = actionDefinition;
        this.isAction = true;
        this.type = NodeType.ACTION;
        this.pageId = actionDefinition.getWebviewId();
        this.objectId = null;
        this.inDialogPage = false;
        this.fieldElementIds = new HashMap<>();
        this.conditionalExpressions = new HashMap<>();
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

    public Boolean isInDialogPage() {
        return inDialogPage;
    }

    public Map<FieldElementCategory, Set<FieldInfo>> getFieldElementIds() {
        return fieldElementIds;
    }

    public Map<ConditionalExpressionCategory, Set<String>> getConditionalExpressions() {
        return conditionalExpressions;
    }

    /**
     * @return boolean - true if this node represents an external Action
     */
    public boolean isAction() {
        return isAction;
    }

    /**
     * @return ActionDefinition - the action definition if isAction is true, null otherwise
     */
    public ActionDefinition getActionDefinition() {
        return actionDefinition;
    }
}
