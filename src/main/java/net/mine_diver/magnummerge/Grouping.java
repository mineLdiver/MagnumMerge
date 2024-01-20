package net.mine_diver.magnummerge;

import com.github.difflib.algorithm.Change;
import com.github.difflib.algorithm.jgit.HistogramDiff;
import net.mine_diver.magnummerge.util.DiffableInsnNode;
import net.mine_diver.magnummerge.util.DiffableInsnNodeGroup;
import net.mine_diver.magnummerge.util.MagnumASM;
import net.mine_diver.magnummerge.visitor.StackGroupingVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public class Grouping {
    public static void main(String[] args) {
        ClassNode originalClass = MagnumASM.readClassNode(OriginalCode.class);
        ClassNode modifiedClass = MagnumASM.readClassNode(ModifiedCode.class);

        MethodNode originalMethod = originalClass.methods.get(1);
        MethodNode modifiedMethod = modifiedClass.methods.get(1);

        List<DiffableInsnNodeGroup> originalInstructions = getGroups(originalClass, originalMethod);
        List<DiffableInsnNodeGroup> modifiedInstructions = getGroups(modifiedClass, modifiedMethod);

        var classDiff = new HistogramDiff<DiffableInsnNodeGroup>();
        var classEdit = classDiff.computeDiff(originalInstructions, modifiedInstructions, null);

        for (Change change : classEdit) {
            System.out.println(change.deltaType + " " + change.startOriginal + " " + change.endOriginal + " " + change.startRevised + " " + change.endRevised);
//            System.out.println(originalInstructions.subList(change.startOriginal, change.endOriginal)
//                    .stream()
//                    .map(comparableInsnNode -> comparableInsnNode.node)
//                    .toList()
//            );
        }
    }

    public static List<DiffableInsnNodeGroup> getGroups(ClassNode classNode, MethodNode methodNode) {
        InsnList list = methodNode.instructions;
        var grouping = new StackGroupingVisitor(classNode.name, methodNode.access, methodNode.name, methodNode.desc);
        methodNode.accept(grouping);
        List<List<AbstractInsnNode>> groups = grouping.groups;
        for (int i = 0, groupsSize = groups.size(); i < groupsSize; i++) {
            List<AbstractInsnNode> group = groups.get(i);
            System.out.println("GROUP " + i);
            System.out.println(group);
        }
        return grouping.groups
                .stream()
                .map(abstractInsnNodes -> new DiffableInsnNodeGroup(abstractInsnNodes
                        .stream()
                        .filter(node -> node.getType() != AbstractInsnNode.LINE && node.getType() != AbstractInsnNode.FRAME)
                        .map(abstractInsnNode -> new DiffableInsnNode(list, abstractInsnNode))
                        .toList()))
                .toList();
    }
}