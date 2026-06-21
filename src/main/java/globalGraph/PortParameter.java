package globalGraph;

/**
 * Represents a parameter of a port in an ActionDefinition.
 * Ports can be InputPort, SuccessPort, or ErrorPort.
 * Each port contains multiple parameters.
 */
public class PortParameter {
    private final String id;
    private final String name;

    public PortParameter(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "PortParameter{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
