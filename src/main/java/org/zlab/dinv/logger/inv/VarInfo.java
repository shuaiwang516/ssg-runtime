package org.zlab.dinv.logger.inv;

import java.io.Serializable;

public class VarInfo implements Serializable {

    static final long serialVersionUID = 20231120L;

    private String name;
    private String type;
    private String value; // className

    private String rootClassName; // In ssg, the root node
}
