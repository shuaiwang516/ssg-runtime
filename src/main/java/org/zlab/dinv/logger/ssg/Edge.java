package org.zlab.dinv.logger.ssg;

import java.io.Serializable;
import java.util.Objects;

public class Edge implements Serializable {
    private static final long serialVersionUID = 20231122L;

    public final String name;
    public final int timestamp; // simulated with log order

    public Edge(String name, int timestamp) {
        this.name = name;
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Edge)) {
            return false;
        }
        Edge other = (Edge) obj;
        return this.name.equals(other.name) && this.timestamp == other.timestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, timestamp);
    }

    @Override
    public String toString() {
        return "Edge{" + "name='" + name + '\'' + ", timestamp=" + timestamp + '}';
    }

}
