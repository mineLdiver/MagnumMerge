package net.mine_diver.magnummerge.test;

import net.mine_diver.magnummerge.MagnumMerge;
import net.mine_diver.magnummerge.util.MagnumASM;
import org.objectweb.asm.tree.ClassNode;

public class Test {
    public static void main(String[] args) {
        ClassNode originalClass = MagnumASM.readClassNode(OriginalCode.class);
        ClassNode modifiedClass = MagnumASM.readClassNode(ModifiedCode.class);

        MagnumMerge.generateMixin(originalClass, modifiedClass);
    }
}