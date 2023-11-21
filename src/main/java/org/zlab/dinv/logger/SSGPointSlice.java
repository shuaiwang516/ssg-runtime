package org.zlab.dinv.logger;

import org.zlab.dinv.logger.inv.Invariant;
import org.zlab.dinv.logger.inv.VarInfo;

import java.util.LinkedList;
import java.util.List;

public abstract class SSGPointSlice extends SSGPoint {
    static final long serialVersionUID = 20231120L;

    public List<Invariant> invs = new LinkedList<>();
    public List<Invariant> collection_size_invs = new LinkedList<>();

    public SSGPointSlice(VarInfo varInfo) {
        super(varInfo);
    }

    public abstract void instantiate_invariants();
    public abstract void instantiate_collection_size_invariants();
    public abstract void addInvariant(Invariant inv);
    public abstract void addCollectionSizeInvariant(Invariant inv);

    public abstract List<Invariant> add(Object value, int count);
    public abstract List<Invariant> addCollectionSize(Object value, int count);

}
