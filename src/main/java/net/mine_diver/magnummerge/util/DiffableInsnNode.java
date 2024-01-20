package net.mine_diver.magnummerge.util;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

public record DiffableInsnNode(InsnList list, AbstractInsnNode node) {
    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof DiffableInsnNode other && MagnumASM.compare(list, other.list, node, other.node));
    }
}
