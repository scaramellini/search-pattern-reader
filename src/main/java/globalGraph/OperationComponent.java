package globalGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an operation component within an Action's <Operations> tag.
 * This can be: Login, Save, Delete, Switch, Loop, Selector, Script, Query, Time, Mail, Logout, Register, UpdateProfile, ChangePassword
 */
public class OperationComponent {
    private final String id;
    private final String type;
    private final String name;
    private final String operationActionType;  //for save components it can be save, update (save or update counts as save)
    private final String actionDefinitionRef;  // if this operation is actually an Action called via definition
    private final List<ComponentFlow> flows;

    public OperationComponent(String id, String type, String name, String operationActionType, String actionDefinitionRef) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.operationActionType = operationActionType;
        this.actionDefinitionRef = actionDefinitionRef;
        this.flows = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getOperationActionType() {
        return operationActionType;
    }

    public String getActionDefinitionRef() {
        return actionDefinitionRef;
    }

    public List<ComponentFlow> getFlows() {
        return flows;
    }

    public void addFlow(ComponentFlow flow) {
        flows.add(flow);
    }

    @Override
    public String toString() {
        return "OperationComponent{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", actionDefinitionRef='" + actionDefinitionRef + '\'' +
                ", flowsCount=" + flows.size() +
                '}';
    }
}
