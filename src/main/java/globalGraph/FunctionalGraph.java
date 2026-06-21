package globalGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * FunctionalGraph represents the internal flow structure of an ActionDefinition.
 * Unlike IFMLGraph which connects UI components in pages, FunctionalGraph connects
 * the operations and ports within a single Action.
 * 
 * Nodes: OperationComponent, InputPort, SuccessPort, ErrorPort
 * Edges: SuccessFlow, ErrorFlow, DataFlow that connect operations and ports
 */
public class FunctionalGraph {
    private final String actionId;
    
    private final Map<String, OperationComponent> operationNodes = new HashMap<>();
    private final Map<String, PortParameter> inputPortNodes = new HashMap<>();
    private final Map<String, PortParameter> successPortNodes = new HashMap<>();
    private final Map<String, PortParameter> errorPortNodes = new HashMap<>();
    
    private final List<ComponentFlow> flows = new ArrayList<>();
    
    private final Map<String, List<ComponentFlow>> outgoing = new HashMap<>();
    private final Map<String, List<ComponentFlow>> incoming = new HashMap<>();

    public FunctionalGraph(String actionId) {
        this.actionId = actionId;
    }

    /**
     * Add an operation component node
     * 
     * @param operation The operation component
     */
    public void addOperationNode(OperationComponent operation) {
        operationNodes.put(operation.getId(), operation);
        outgoing.putIfAbsent(operation.getId(), new ArrayList<>());
        incoming.putIfAbsent(operation.getId(), new ArrayList<>());
    }

    /**
     * Add an input port parameter node
     * 
     * @param parameter The input port parameter
     */
    public void addInputPortNode(PortParameter parameter) {
        inputPortNodes.put(parameter.getId(), parameter);
        outgoing.putIfAbsent(parameter.getId(), new ArrayList<>());
        incoming.putIfAbsent(parameter.getId(), new ArrayList<>());
    }

    /**
     * Add a success output port parameter node
     * 
     * @param parameter The success port parameter
     */
    public void addSuccessPortNode(PortParameter parameter) {
        successPortNodes.put(parameter.getId(), parameter);
        outgoing.putIfAbsent(parameter.getId(), new ArrayList<>());
        incoming.putIfAbsent(parameter.getId(), new ArrayList<>());
    }

    /**
     * Add an error output port parameter node
     * 
     * @param parameter The error port parameter
     */
    public void addErrorPortNode(PortParameter parameter) {
        errorPortNodes.put(parameter.getId(), parameter);
        outgoing.putIfAbsent(parameter.getId(), new ArrayList<>());
        incoming.putIfAbsent(parameter.getId(), new ArrayList<>());
    }

    /**
     * Add a flow between nodes
     * 
     * @param flow The component flow
     */
    public void addFlow(ComponentFlow flow) {
        // Verify that target exists
        String targetId = flow.getTo();
        if (!nodeExists(targetId)) {
            return; // Skip flows with missing target nodes
        }

        flows.add(flow);
        outgoing.computeIfAbsent(flow.getId(), k -> new ArrayList<>()).add(flow);
        incoming.computeIfAbsent(targetId, k -> new ArrayList<>()).add(flow);
    }

    /**
     * Check if a node with the given ID exists in the graph
     * 
     * @param nodeId The node ID
     * @return true if the node exists, false otherwise
     */
    private boolean nodeExists(String nodeId) {
        return operationNodes.containsKey(nodeId) ||
               inputPortNodes.containsKey(nodeId) ||
               successPortNodes.containsKey(nodeId) ||
               errorPortNodes.containsKey(nodeId);
    }

    /**
     * Get an operation node by ID
     * 
     * @param operationId The operation ID
     * @return The OperationComponent, or null if not found
     */
    public OperationComponent getOperationNode(String operationId) {
        return operationNodes.get(operationId);
    }

    /**
     * Get an input port node by ID
     * 
     * @param parameterId The parameter ID
     * @return The PortParameter, or null if not found
     */
    public PortParameter getInputPortNode(String parameterId) {
        return inputPortNodes.get(parameterId);
    }

    /**
     * Get a success port node by ID
     * 
     * @param parameterId The parameter ID
     * @return The PortParameter, or null if not found
     */
    public PortParameter getSuccessPortNode(String parameterId) {
        return successPortNodes.get(parameterId);
    }

    /**
     * Get an error port node by ID
     * 
     * @param parameterId The parameter ID
     * @return The PortParameter, or null if not found
     */
    public PortParameter getErrorPortNode(String parameterId) {
        return errorPortNodes.get(parameterId);
    }

    /**
     * Get all operation nodes
     * 
     * @return Collection of all operations
     */
    public java.util.Collection<OperationComponent> getAllOperationNodes() {
        return operationNodes.values();
    }

    /**
     * Get all input port nodes
     * 
     * @return Collection of all input ports
     */
    public java.util.Collection<PortParameter> getAllInputPortNodes() {
        return inputPortNodes.values();
    }

    /**
     * Get all success port nodes
     * 
     * @return Collection of all success ports
     */
    public java.util.Collection<PortParameter> getAllSuccessPortNodes() {
        return successPortNodes.values();
    }

    /**
     * Get all error port nodes
     * 
     * @return Collection of all error ports
     */
    public java.util.Collection<PortParameter> getAllErrorPortNodes() {
        return errorPortNodes.values();
    }

    /**
     * Get all flows in the graph
     * 
     * @return List of all ComponentFlows
     */
    public List<ComponentFlow> getAllFlows() {
        return flows;
    }

    /**
     * Get outgoing flows from a node
     * 
     * @param nodeId The node ID
     * @return List of outgoing flows
     */
    public List<ComponentFlow> getOutgoing(String nodeId) {
        return outgoing.getOrDefault(nodeId, Collections.emptyList());
    }

    /**
     * Get incoming flows to a node
     * 
     * @param nodeId The node ID
     * @return List of incoming flows
     */
    public List<ComponentFlow> getIncoming(String nodeId) {
        return incoming.getOrDefault(nodeId, Collections.emptyList());
    }

    /**
     * Get the action ID this functional graph represents
     * 
     * @return The action ID
     */
    public String getActionId() {
        return actionId;
    }

    /**
     * Get statistics about the functional graph
     * 
     * @return String describing the graph structure
     */
    public String getStatistics() {
        return String.format(
                "FunctionalGraph for %s - Operations: %d, InputPorts: %d, SuccessPorts: %d, ErrorPorts: %d, Flows: %d",
                actionId,
                operationNodes.size(),
                inputPortNodes.size(),
                successPortNodes.size(),
                errorPortNodes.size(),
                flows.size()
        );
    }
}
