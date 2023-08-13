package org.zlab.dinv;

import java.nio.file.Path;

public class Utils {
    public static boolean exclude(Path p) {
        if (p.toString().contains("hadoop-yarn-project"))
            return true;
        return false;
    }
}
