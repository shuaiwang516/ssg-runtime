package org.zlab.dinv.logger.inference.template;

public abstract class UnaryTemplate {
    public abstract boolean validate(Object val);
    public abstract void update(Object val);
    public abstract boolean isValid();
}
