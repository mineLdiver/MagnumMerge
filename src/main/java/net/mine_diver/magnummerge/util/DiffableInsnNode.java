package net.mine_diver.magnummerge.util;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

public class DiffableInsnNode {
    private final InsnList list;
    public final AbstractInsnNode node;

    public DiffableInsnNode(InsnList list, AbstractInsnNode node) {
        this.list = list;
        this.node = node;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof DiffableInsnNode other && MagnumASM.compare(list, other.list, node, other.node));
    }
}
