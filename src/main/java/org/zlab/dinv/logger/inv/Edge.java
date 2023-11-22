package org.zlab.dinv.logger.inv;

import java.io.Serializable;

public class Edge  implements Serializable {
    private static final long serialVersionUID = 20231122L;

    public String name;
    public int timestamp; // simulated with log order

    public Edge(String name, int timestamp) {
        this.name = name;
        this.timestamp = timestamp;
    }
}
