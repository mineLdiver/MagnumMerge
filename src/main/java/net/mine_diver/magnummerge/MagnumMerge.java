package net.mine_diver.magnummerge;

import com.github.difflib.algorithm.Change;
import com.github.difflib.algorithm.DiffAlgorithmI;
import com.github.difflib.algorithm.jgit.HistogramDiff;
import net.mine_diver.magnummerge.util.DiffableInsnNode;
import net.mine_diver.magnummerge.util.DiffableInsnNodeGroup;
import net.mine_diver.magnummerge.visitor.StackGroupingVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MagnumMerge {
    private static final DiffAlgorithmI<DiffableInsnNodeGroup> GROUP_DIFF = new HistogramDiff<>();

    private static final class MethodPair {
        private final MethodNode originalMethod;
        private final MethodNode modifiedMethod;
        private final List<DiffableInsnNodeGroup> originalInsnGroups;
        private final List<DiffableInsnNodeGroup> modifiedInsnGroups;
        private final List<Change> editScript;

        private MethodPair(
                ClassNode originalClass, ClassNode modifiedClass,
                MethodNode originalMethod, MethodNode modifiedMethod
        ) {
            this.originalMethod = originalMethod;
            this.modifiedMethod = modifiedMethod;
            var originalMethodVisitor = new StackGroupingVisitor(originalClass.name, originalMethod.access, originalMethod.name, originalMethod.desc);
            originalMethod.accept(originalMethodVisitor);
            originalInsnGroups = originalMethodVisitor.groups
                    .stream()
                    .map(abstractInsnNodes -> new DiffableInsnNodeGroup(abstractInsnNodes
                            .stream()
                            .filter(node -> node.getType() != AbstractInsnNode.LINE && node.getType() != AbstractInsnNode.FRAME)
                            .map(abstractInsnNode -> new DiffableInsnNode(originalMethod.instructions, abstractInsnNode))
                            .toList()))
                    .toList();
            var modifiedMethodVisitor = new StackGroupingVisitor(modifiedClass.name, modifiedMethod.access, modifiedMethod.name, modifiedMethod.desc);
            modifiedMethod.accept(modifiedMethodVisitor);
            modifiedInsnGroups = modifiedMethodVisitor.groups
                    .stream()
                    .map(abstractInsnNodes -> new DiffableInsnNodeGroup(abstractInsnNodes
                            .stream()
                            .filter(node -> node.getType() != AbstractInsnNode.LINE && node.getType() != AbstractInsnNode.FRAME)
                            .map(abstractInsnNode -> new DiffableInsnNode(modifiedMethod.instructions, abstractInsnNode))
                            .toList()))
                    .toList();
            editScript = GROUP_DIFF.computeDiff(originalInsnGroups, modifiedInsnGroups, null);
            for (var change : editScript) {
                System.out.println(change.deltaType + " " + change.startOriginal + " " + change.endOriginal + " " + change.startRevised + " " + change.endRevised);
            }
        }
    }

    public static ClassNode generateMixin(ClassNode originalClass, ClassNode modifiedClass) {
        // Discover all methods with identical attributes
        var methodPairs = new ArrayList<MethodPair>();
        for (var modifiedMethod : modifiedClass.methods) {
            for (var originalMethod : originalClass.methods) {
                if (
                        modifiedMethod.access == originalMethod.access &&
                                Objects.equals(modifiedMethod.name, originalMethod.name) &&
                                Objects.equals(modifiedMethod.desc, originalMethod.desc)
                ) {
                    methodPairs.add(new MethodPair(
                            originalClass, modifiedClass,
                            originalMethod, modifiedMethod
                    ));
                    break;
                }
            }
        }
        return null;
    }
}
