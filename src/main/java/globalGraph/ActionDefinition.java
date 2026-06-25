package globalGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a complete Action definition extracted from a Properties.wr file.
 * An Action is a reusable module that can be called from pages or other actions.
 * It contains input parameters, output ports (success/error), internal operations, and events.
 * 
 * Note: An ActionDefinition can have multiple implementation IDs for the same definition.
 * For example, "LoginAction" might have implementations "LoginAction_v1", "LoginAction_v2", etc.
 */
public class ActionDefinition {
    private final List<String> ids;
    private final String definition;
    private final String webviewId;
    private final String filePath;
    private final List<PortParameter> inputParameters;
    private final Map<String, List<PortParameter>> successOutputPorts;
    private final Map<String, List<PortParameter>> errorOutputPorts;
    private final List<OperationComponent> operationComponents;
    private final Map<String, ActionEvent> events;

    public ActionDefinition(
            List<String> ids,
            String definition,
            String webviewId,
            String filePath,
            List<PortParameter> inputParameters,
            Map<String, List<PortParameter>> successOutputPorts,
            Map<String, List<PortParameter>> errorOutputPorts,
            List<OperationComponent> operationComponents,
            Map<String, ActionEvent> events) {
        this.ids = ids != null ? new ArrayList<>(ids) : new ArrayList<>();
        this.definition = definition;
        this.webviewId = webviewId;
        this.filePath = filePath;
        this.inputParameters = inputParameters;
        this.successOutputPorts = successOutputPorts;
        this.errorOutputPorts = errorOutputPorts;
        this.operationComponents = operationComponents;
        this.events = events;
    }

    public String getId() {
        // Return primary ID (first one)
        return ids.isEmpty() ? null : ids.get(0);
    }

    public List<String> getIds() {
        return new ArrayList<>(ids);
    }

    public void setId(String actionId) {
        this.ids.clear();
        if (actionId != null) {
            this.ids.add(actionId);
        }
    }

    public void addId(String actionId) {
        if (actionId != null && !ids.contains(actionId)) {
            ids.add(actionId);
        }
    }

    public String getDefinition() {
        return definition;
    }

    public String getWebviewId() {
        return webviewId;
    }

    public String getFilePath() {
        return filePath;
    }

    public List<PortParameter> getInputParameters() {
        return inputParameters;
    }

    public Map<String, List<PortParameter>> getSuccessOutputPorts() {
        return successOutputPorts;
    }

    public Map<String, List<PortParameter>> getErrorOutputPorts() {
        return errorOutputPorts;
    }

    public List<OperationComponent> getOperationComponents() {
        return operationComponents;
    }

    public Map<String, ActionEvent> getEvents() {
        return events;
    }

    /**
     * Get a success output port by its id
     */
    public List<PortParameter> getSuccessPort(String portId) {
        return successOutputPorts.getOrDefault(portId, new ArrayList<>());
    }

    /**
     * Get an error output port by its id
     */
    public List<PortParameter> getErrorPort(String portId) {
        return errorOutputPorts.getOrDefault(portId, new ArrayList<>());
    }

    /**
     * Get an event by its id
     */
    public ActionEvent getEvent(String eventId) {
        return events.get(eventId);
    }

    /**
     * Get an operation component by its id
     */
    public OperationComponent getOperationComponent(String componentId) {
        return operationComponents.stream()
                .filter(op -> op.getId().equals(componentId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String toString() {
        return "ActionDefinition{" +
                "ids=" + ids +
                ", definition='" + definition + '\'' +
                ", webviewId='" + webviewId + '\'' +
                ", filePath='" + filePath + '\'' +
                ", inputParametersCount=" + inputParameters.size() +
                ", successPortsCount=" + successOutputPorts.size() +
                ", errorPortsCount=" + errorOutputPorts.size() +
                ", operationsCount=" + operationComponents.size() +
                ", eventsCount=" + events.size() +
                '}';
    }
}
