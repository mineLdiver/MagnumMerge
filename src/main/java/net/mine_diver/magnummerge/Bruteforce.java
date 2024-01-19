package net.mine_diver.magnummerge;

import com.github.difflib.algorithm.Change;
import com.github.difflib.algorithm.jgit.HistogramDiff;
import com.github.difflib.algorithm.myers.MeyersDiffWithLinearSpace;
import net.mine_diver.magnummerge.util.DiffableInsnNode;
import net.mine_diver.magnummerge.util.MagnumASM;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.stream.StreamSupport;

public class Bruteforce {
    public static void main(String[] args) {
        ClassNode originalClass = MagnumASM.readClassNode(OriginalCode.class);
        ClassNode modifiedClass = MagnumASM.readClassNode(ModifiedCode.class);

        MethodNode originalMethod = originalClass.methods.get(1);
        MethodNode modifiedMethod = modifiedClass.methods.get(1);

        List<DiffableInsnNode> originalInstructions = StreamSupport
                .stream(originalMethod.instructions.spliterator(), false)
                .filter(node -> node.getType() != AbstractInsnNode.LINE && node.getType() != AbstractInsnNode.FRAME)
                .map(node -> new DiffableInsnNode(originalMethod.instructions, node)).toList();
        List<DiffableInsnNode> modifiedInstructions = StreamSupport
                .stream(modifiedMethod.instructions.spliterator(), false)
                .filter(node -> node.getType() != AbstractInsnNode.LINE && node.getType() != AbstractInsnNode.FRAME)
                .map(node -> new DiffableInsnNode(modifiedMethod.instructions, node)).toList();

        var classDiff = new MeyersDiffWithLinearSpace<DiffableInsnNode>();
        var classEdit = classDiff.computeDiff(originalInstructions, modifiedInstructions, null);

        for (Change change : classEdit) {
            System.out.println(change.deltaType + " " + change.startOriginal + " " + change.endOriginal + " " + change.startRevised + " " + change.endRevised);
            System.out.println(originalInstructions.subList(change.startOriginal, change.endOriginal)
                    .stream()
                    .map(comparableInsnNode -> comparableInsnNode.node)
                    .toList()
            );
        }
    }
}