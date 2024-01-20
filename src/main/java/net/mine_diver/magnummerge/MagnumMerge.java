package net.mine_diver.magnummerge;

import com.github.difflib.algorithm.DiffAlgorithmI;
import com.github.difflib.algorithm.jgit.HistogramDiff;
import net.mine_diver.magnummerge.util.DiffableInsnNode;
import net.mine_diver.magnummerge.util.DiffableInsnNodeGroup;
import net.mine_diver.magnummerge.visitor.StackGroupingVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Modifier;
import java.util.*;

public class MagnumMerge {
    private static final DiffAlgorithmI<DiffableInsnNodeGroup> GROUP_DIFF = new HistogramDiff<>();

    private static List<MethodNode> generateHandlers(
            ClassNode originalClass, ClassNode modifiedClass,
            MethodNode originalMethod, MethodNode modifiedMethod
    ) {
        var originalMethodVisitor = new StackGroupingVisitor(originalClass.name, originalMethod.access, originalMethod.name, originalMethod.desc);
        originalMethod.accept(originalMethodVisitor);
        var originalInsnGroups = originalMethodVisitor.groups
                .stream()
                .map(abstractInsnNodes -> new DiffableInsnNodeGroup(abstractInsnNodes
                        .stream()
                        .filter(node -> node.getType() != AbstractInsnNode.LINE && node.getType() != AbstractInsnNode.FRAME)
                        .map(abstractInsnNode -> new DiffableInsnNode(originalMethod.instructions, abstractInsnNode))
                        .toList()))
                .toList();
        var modifiedMethodVisitor = new StackGroupingVisitor(modifiedClass.name, modifiedMethod.access, modifiedMethod.name, modifiedMethod.desc);
        modifiedMethod.accept(modifiedMethodVisitor);
        var modifiedInsnGroups = modifiedMethodVisitor.groups
                .stream()
                .map(abstractInsnNodes -> new DiffableInsnNodeGroup(abstractInsnNodes
                        .stream()
                        .filter(node -> node.getType() != AbstractInsnNode.LINE && node.getType() != AbstractInsnNode.FRAME)
                        .map(abstractInsnNode -> new DiffableInsnNode(modifiedMethod.instructions, abstractInsnNode))
                        .toList()))
                .toList();
        var editScript = GROUP_DIFF.computeDiff(originalInsnGroups, modifiedInsnGroups, null);
        for (var change : editScript) {
            System.out.println(change.deltaType + " " + change.startOriginal + " " + change.endOriginal + " " + change.startRevised + " " + change.endRevised);
        }
        List<MethodNode> handlers = new ArrayList<>();
        for (var change : editScript) {
            switch (change.deltaType) {
                case INSERT -> {
                    var arguments = new ArrayList<>(List.of(Type.getArgumentTypes(originalMethod.desc)));
                    arguments.add(Type.getObjectType(Type.getReturnType(originalMethod.desc) == Type.VOID_TYPE ?
                            "org/spongepowered/asm/mixin/injection/callback/CallbackInfo" :
                            "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable"
                    ));
                    var methodNode = new MethodNode(
                            Modifier.PRIVATE | (Modifier.isStatic(originalMethod.access) ? Modifier.STATIC : 0),
                            UUID.randomUUID().toString(),
                            Type.getMethodDescriptor(Type.VOID_TYPE, arguments.toArray(Type[]::new)),
                            null,
                            null
                    );
                    var annotation = methodNode.visitAnnotation("Lorg/spongepowered/asm/mixin/injection/Inject;", false);
                    annotation.visit("method", originalMethod.name + originalMethod.desc);
                    // figure out target for the annotation
                    modifiedInsnGroups.subList(change.startRevised, change.endRevised)
                            .stream()
                            .map(DiffableInsnNodeGroup::insns)
                            .flatMap(Collection::stream)
                            .map(DiffableInsnNode::node)
                            .forEach(methodNode.instructions::add);
                    methodNode.instructions.add(new InsnNode(Opcodes.RETURN));
                    handlers.add(methodNode);
                }
            }
        }
        return handlers;
    }

    public static ClassNode generateMixin(ClassNode originalClass, ClassNode modifiedClass) {
        ClassNode classNode = new ClassNode();
        classNode.visit(originalClass.version, 0, UUID.randomUUID().toString(), null, Type.getInternalName(Object.class), null);
        for (var modifiedMethod : modifiedClass.methods) {
            for (var originalMethod : originalClass.methods) {
                if (
                        modifiedMethod.access == originalMethod.access &&
                                Objects.equals(modifiedMethod.name, originalMethod.name) &&
                                Objects.equals(modifiedMethod.desc, originalMethod.desc)
                ) {
                    classNode.methods.addAll(generateHandlers(
                            originalClass, modifiedClass,
                            originalMethod, modifiedMethod
                    ));
                    break;
                }
            }
        }
        return classNode;
    }
}
