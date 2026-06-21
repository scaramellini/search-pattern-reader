package it.davide.xml;

import globalGraph.*;
import java.util.*;

/**
 * ActionDefinitionParser extracts and builds the FunctionalGraph from an ActionDefinition.
 * It processes the internal structure of an Action including:
 * - Input ports
 * - Output ports (success and error)
 * - Operation components
 * - Flows between them
 */
public class ActionDefinitionParser {

    /**
     * Build a FunctionalGraph from an ActionDefinition
     * This graph represents the internal flow structure of the Action
     * 
     * @param actionDef The ActionDefinition to process
     * @return FunctionalGraph representing the internal structure
     */
    public FunctionalGraph buildFunctionalGraph(ActionDefinition actionDef) {
        FunctionalGraph graph = new FunctionalGraph(actionDef.getId());

        // Add all input port parameter nodes
        for (PortParameter param : actionDef.getInputParameters()) {
            graph.addInputPortNode(param);
        }

        // Add all success output port parameter nodes
        for (List<PortParameter> portParams : actionDef.getSuccessOutputPorts().values()) {
            for (PortParameter param : portParams) {
                graph.addSuccessPortNode(param);
            }
        }

        // Add all error output port parameter nodes
        for (List<PortParameter> portParams : actionDef.getErrorOutputPorts().values()) {
            for (PortParameter param : portParams) {
                graph.addErrorPortNode(param);
            }
        }

        // Add all operation component nodes
        for (OperationComponent operation : actionDef.getOperationComponents()) {
            graph.addOperationNode(operation);
        }

        // Add all flows
        for (OperationComponent operation : actionDef.getOperationComponents()) {
            for (ComponentFlow flow : operation.getFlows()) {
                graph.addFlow(flow);
            }
        }

        return graph;
    }

    /**
     * Get all operations that directly start from input ports
     * These are the entry points of the action execution
     * 
     * @param graph The functional graph
     * @return List of operations that are directly triggered by input ports
     */
    public List<OperationComponent> getStartingOperations(FunctionalGraph graph) {
        List<OperationComponent> startingOps = new ArrayList<>();

        for (PortParameter inputParam : graph.getAllInputPortNodes()) {
            List<ComponentFlow> outgoing = graph.getOutgoing(inputParam.getId());
            
            for (ComponentFlow flow : outgoing) {
                OperationComponent targetOp = graph.getOperationNode(flow.getTo());
                if (targetOp != null && !startingOps.contains(targetOp)) {
                    startingOps.add(targetOp);
                }
            }
        }

        return startingOps;
    }

    /**
     * Get all operations that lead to a specific output port
     * These are the terminal operations before exiting the action
     * 
     * @param graph The functional graph
     * @param outputPortId The ID of the output port
     * @return List of operations that have flows to the specified output port
     */
    public List<OperationComponent> getTerminalOperations(FunctionalGraph graph, String outputPortId) {
        List<OperationComponent> terminalOps = new ArrayList<>();

        List<ComponentFlow> incomingFlows = graph.getIncoming(outputPortId);
        
        for (ComponentFlow flow : incomingFlows) {
            // The source of the flow should be an operation
            // We need to find which operation has this flow
            for (OperationComponent op : graph.getAllOperationNodes()) {
                if (op.getFlows().contains(flow)) {
                    if (!terminalOps.contains(op)) {
                        terminalOps.add(op);
                    }
                }
            }
        }

        return terminalOps;
    }

    /**
     * Get all operations of a specific type
     * 
     * @param graph The functional graph
     * @param operationType The type of operation (e.g., "Login", "Save", "Query")
     * @return List of operations of the specified type
     */
    public List<OperationComponent> getOperationsByType(FunctionalGraph graph, String operationType) {
        List<OperationComponent> result = new ArrayList<>();

        for (OperationComponent op : graph.getAllOperationNodes()) {
            if (operationType.equals(op.getType())) {
                result.add(op);
            }
        }

        return result;
    }

    /**
     * Trace a path through the functional graph from an input port to an output port
     * This is useful for understanding the sequence of operations
     * 
     * @param graph The functional graph
     * @param startPortId The starting input port ID
     * @param endPortId The ending output port ID
     * @return List of operations in the path, or empty list if no path exists
     */
    public List<OperationComponent> tracePath(FunctionalGraph graph, String startPortId, String endPortId) {
        List<OperationComponent> path = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        tracePathDFS(graph, startPortId, endPortId, path, visited);

        return path;
    }

    /**
     * Recursive DFS helper for path tracing
     */
    private boolean tracePathDFS(FunctionalGraph graph, String currentId, String targetId,
                                  List<OperationComponent> path, Set<String> visited) {
        if (currentId.equals(targetId)) {
            return true;
        }

        if (visited.contains(currentId)) {
            return false;
        }

        visited.add(currentId);

        List<ComponentFlow> outgoing = graph.getOutgoing(currentId);

        for (ComponentFlow flow : outgoing) {
            String nextId = flow.getTo();
            
            // If next is an operation, add it to path and continue
            OperationComponent op = graph.getOperationNode(nextId);
            if (op != null) {
                path.add(op);
                if (tracePathDFS(graph, nextId, targetId, path, visited)) {
                    return true;
                }
                path.remove(path.size() - 1);
            } else {
                // Next might be a port, continue tracing
                if (tracePathDFS(graph, nextId, targetId, path, visited)) {
                    return true;
                }
            }
        }

        visited.remove(currentId);
        return false;
    }
}
