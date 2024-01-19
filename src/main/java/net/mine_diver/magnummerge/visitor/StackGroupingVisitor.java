package net.mine_diver.magnummerge.visitor;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

public class StackGroupingVisitor extends AnalyzerAdapter {
    private final List<AbstractInsnNode> currentGroup = new ArrayList<>();
    public final List<List<AbstractInsnNode>> groups = new ArrayList<>();

    public StackGroupingVisitor(String owner, int access, String name, String descriptor, MethodVisitor methodVisitor) {
        this(Opcodes.ASM9, owner, access, name, descriptor, methodVisitor);
    }

    protected StackGroupingVisitor(int api, String owner, int access, String name, String descriptor, MethodVisitor methodVisitor) {
        super(api, owner, access, name, descriptor, methodVisitor);
    }

    private void checkStackState() {
        if (stack == null || stack.isEmpty()) {
            groups.add(new ArrayList<>(currentGroup));
            currentGroup.clear();
        }
    }

    protected LabelNode getLabelNode(final Label label) {
        if (!(label.info instanceof LabelNode))
            label.info = new LabelNode();
        return (LabelNode) label.info;
    }

    private LabelNode[] getLabelNodes(final Label[] labels) {
        LabelNode[] labelNodes = new LabelNode[labels.length];
        for (int i = 0, n = labels.length; i < n; ++i)
            labelNodes[i] = getLabelNode(labels[i]);
        return labelNodes;
    }

    @Override
    public void visitInsn(int opcode) {
        currentGroup.add(new InsnNode(opcode));
        super.visitInsn(opcode);
        checkStackState();
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        currentGroup.add(new IntInsnNode(opcode, operand));
        super.visitIntInsn(opcode, operand);
        checkStackState();
    }

    @Override
    public void visitVarInsn(int opcode, int varIndex) {
        currentGroup.add(new VarInsnNode(opcode, varIndex));
        super.visitVarInsn(opcode, varIndex);
        checkStackState();
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        currentGroup.add(new TypeInsnNode(opcode, type));
        super.visitTypeInsn(opcode, type);
        checkStackState();
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        currentGroup.add(new FieldInsnNode(opcode, owner, name, descriptor));
        super.visitFieldInsn(opcode, owner, name, descriptor);
        checkStackState();
    }

    @Override
    public void visitMethodInsn(int opcodeAndSource, String owner, String name, String descriptor, boolean isInterface) {
        currentGroup.add(new MethodInsnNode(opcodeAndSource, owner, name, descriptor, isInterface));
        super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface);
        checkStackState();
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        currentGroup.add(new InvokeDynamicInsnNode(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments));
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        checkStackState();
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        currentGroup.add(new JumpInsnNode(opcode, getLabelNode(label)));
        super.visitJumpInsn(opcode, label);
        checkStackState();
    }

    @Override
    public void visitLabel(Label label) {
        currentGroup.add(getLabelNode(label));
        super.visitLabel(label);
        checkStackState();
    }

    @Override
    public void visitLdcInsn(Object value) {
        currentGroup.add(new LdcInsnNode(value));
        super.visitLdcInsn(value);
        checkStackState();
    }

    @Override
    public void visitIincInsn(int varIndex, int increment) {
        currentGroup.add(new IincInsnNode(varIndex, increment));
        super.visitIincInsn(varIndex, increment);
        checkStackState();
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        currentGroup.add(new TableSwitchInsnNode(min, max, getLabelNode(dflt), getLabelNodes(labels)));
        super.visitTableSwitchInsn(min, max, dflt, labels);
        checkStackState();
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        currentGroup.add(new LookupSwitchInsnNode(getLabelNode(dflt), keys, getLabelNodes(labels)));
        super.visitLookupSwitchInsn(dflt, keys, labels);
        checkStackState();
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        currentGroup.add(new MultiANewArrayInsnNode(descriptor, numDimensions));
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
        checkStackState();
    }
}
