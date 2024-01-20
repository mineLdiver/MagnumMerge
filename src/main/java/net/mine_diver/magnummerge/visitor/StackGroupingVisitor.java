package net.mine_diver.magnummerge.visitor;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

public class StackGroupingVisitor extends MethodVisitor {
    private final MethodNode methodNode;
    private final AnalyzerAdapter analyzerAdapter;
    public final List<List<AbstractInsnNode>> groups = new ArrayList<>();

    public StackGroupingVisitor(String owner, int access, String name, String descriptor) {
        this(Opcodes.ASM9, owner, access, name, descriptor, new MethodNode());
    }

    protected StackGroupingVisitor(int api, String owner, int access, String name, String descriptor, MethodNode methodNode) {
        this(api, methodNode, new AnalyzerAdapter(owner, access, name, descriptor, methodNode));
    }

    protected StackGroupingVisitor(int api, MethodNode methodNode, AnalyzerAdapter analyzerAdapter) {
        super(api, analyzerAdapter);
        this.methodNode = methodNode;
        this.analyzerAdapter = analyzerAdapter;
    }

    private void checkStackState() {
        if (analyzerAdapter.stack == null || analyzerAdapter.stack.isEmpty()) {
            groups.add(StreamSupport.stream(methodNode.instructions.spliterator(), false).toList());
            methodNode.instructions.clear();
        }
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
        checkStackState();
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
        checkStackState();
    }

    @Override
    public void visitVarInsn(int opcode, int varIndex) {
        super.visitVarInsn(opcode, varIndex);
        checkStackState();
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);
        checkStackState();
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        super.visitFieldInsn(opcode, owner, name, descriptor);
        checkStackState();
    }

    @Override
    public void visitMethodInsn(int opcodeAndSource, String owner, String name, String descriptor, boolean isInterface) {
        super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface);
        checkStackState();
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        checkStackState();
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
        checkStackState();
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
        checkStackState();
    }

    @Override
    public void visitLdcInsn(Object value) {
        super.visitLdcInsn(value);
        checkStackState();
    }

    @Override
    public void visitIincInsn(int varIndex, int increment) {
        super.visitIincInsn(varIndex, increment);
        checkStackState();
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        checkStackState();
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        checkStackState();
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
        checkStackState();
    }
}
