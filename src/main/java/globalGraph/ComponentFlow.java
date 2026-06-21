package globalGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a flow within an Action definition.
 * This can be a SuccessFlow, ErrorFlow, or DataFlow.
 * It maps the output of an operation to a target (another operation, success port, or error port).
 */
public class ComponentFlow {
    private final String id;
    private final String type;  // "SuccessFlow", "ErrorFlow", "DataFlow"
    private final String to;    // target component or port id
    private final List<EdgeBinding> bindings;

    public ComponentFlow(String id, String type, String to) {
        this.id = id;
        this.type = type;
        this.to = to;
        this.bindings = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTo() {
        return to;
    }

    public List<EdgeBinding> getBindings() {
        return bindings;
    }

    public void addBinding(EdgeBinding binding) {
        bindings.add(binding);
    }

    @Override
    public String toString() {
        return "ComponentFlow{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", to='" + to + '\'' +
                ", bindingsCount=" + bindings.size() +
                '}';
    }
}
