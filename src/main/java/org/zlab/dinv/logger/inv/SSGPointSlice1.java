package org.zlab.dinv.logger.inv;

import org.zlab.dinv.logger.inv.unary.UnaryInvariant;

import java.util.LinkedList;
import java.util.List;

public class SSGPointSlice1 extends SSGPointSlice {
    static final long serialVersionUID = 20231120L;

    public SSGPointSlice1(VarInfo varInfo) {
        super(varInfo);
    }

    @Override
    public void instantiate_invariants() {
        for (Invariant protoInv : Engine.proto_invs) {
            Invariant inv = protoInv.instantiate(this);
            invs.add(inv);
        }
    }

    @Override
    public void instantiate_collection_size_invariants() {
        if (varInfo.isCollection()) {
            for (Invariant protoInv : Engine.proto_invs) {
                Invariant inv = protoInv.instantiate(this);
                addCollectionSizeInvariant(inv);
            }
        }
    }

    @Override
    public void addInvariant(Invariant inv) {
        invs.add(inv);
    }

    @Override
    public void addCollectionSizeInvariant(Invariant inv) {
        collection_size_invs.add(inv);
    }

    @Override
    public List<Invariant> add(Object value, int count) {
        // Update the invariant that's valid
        List<Invariant> valid_invs = new LinkedList<>();

        for (Invariant inv : invs) {
            UnaryInvariant uinv = (UnaryInvariant) inv;
            uinv.add(value, count);
            if (!uinv.is_false())
                valid_invs.add(inv);
        }
        invs = valid_invs;
        return valid_invs;
    }

    @Override
    public List<Invariant> addCollectionSize(Object value, int count) {
        // Update the invariant that's valid
        List<Invariant> valid_invs = new LinkedList<>();

        for (Invariant inv : collection_size_invs) {
            UnaryInvariant uinv = (UnaryInvariant) inv;
            uinv.add(value, count);
            if (!uinv.is_false())
                valid_invs.add(inv);
        }
        collection_size_invs = valid_invs;
        return valid_invs;
    }

}
