package net.mine_diver.magnummerge.util;

import java.util.ArrayList;
import java.util.List;

public class DiffableInsnNodeGroup {
    private final List<DiffableInsnNode> insns;

    public DiffableInsnNodeGroup(List<DiffableInsnNode> insns) {
        this.insns = new ArrayList<>(insns);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof DiffableInsnNodeGroup other && insns.equals(other.insns));
    }
}
