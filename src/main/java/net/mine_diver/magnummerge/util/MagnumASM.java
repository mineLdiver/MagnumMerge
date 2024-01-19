package net.mine_diver.magnummerge.util;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

public class MagnumASM {
    public static byte[] readClassBytes(Class<?> classObject) {
        byte[] bytes;
        int totalRead;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (InputStream classStream = Objects.requireNonNull(MagnumASM.class.getClassLoader().getResourceAsStream(classObject.getName().replace('.', '/').concat(".class")))) {
            while(classStream.available() > 0) {
                bytes = new byte[classStream.available()];
                totalRead = classStream.read(bytes);
                if (totalRead == -1)
                    throw new RuntimeException("Couldn't read class \"" + classObject.getName() + "\"!");
                buffer.write(bytes, 0, totalRead);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return buffer.toByteArray();
    }

    public static ClassNode readClassNode(Class<?> classObject) {
        ClassNode classNode = new ClassNode();
        new ClassReader(readClassBytes(classObject)).accept(classNode, ClassReader.EXPAND_FRAMES);
        return classNode;
    }

    public static boolean compare(InsnList listA, InsnList listB, AbstractInsnNode insnA, AbstractInsnNode insnB) {
        if (insnA.getType() != insnB.getType() || insnA.getOpcode() != insnB.getOpcode()) return false;

        switch (insnA.getType()) {
            case AbstractInsnNode.INT_INSN: {
                IntInsnNode a = (IntInsnNode) insnA;
                IntInsnNode b = (IntInsnNode) insnB;

                return a.operand == b.operand;
            }

            case AbstractInsnNode.VAR_INSN: {
                VarInsnNode a = (VarInsnNode) insnA;
                VarInsnNode b = (VarInsnNode) insnB;

                return a.var == b.var;
            }

            case AbstractInsnNode.TYPE_INSN: {
                TypeInsnNode a = (TypeInsnNode) insnA;
                TypeInsnNode b = (TypeInsnNode) insnB;

                return Objects.equals(a.desc, b.desc);
            }

            case AbstractInsnNode.FIELD_INSN: {
                FieldInsnNode a = (FieldInsnNode) insnA;
                FieldInsnNode b = (FieldInsnNode) insnB;

                return Objects.equals(a.owner, b.owner) && Objects.equals(a.name, b.name) && Objects.equals(a.desc, b.desc);
            }

            case AbstractInsnNode.METHOD_INSN: {
                MethodInsnNode a = (MethodInsnNode) insnA;
                MethodInsnNode b = (MethodInsnNode) insnB;

                if (!Objects.equals(a.owner, b.owner) || !Objects.equals(a.name, b.name) || !Objects.equals(a.desc, b.desc)) {
                    return false;
                }
                //More debatable if the actual method is the same, we'll go with it being a change for now
                return a.itf == b.itf;
            }

            case AbstractInsnNode.INVOKE_DYNAMIC_INSN: {
                InvokeDynamicInsnNode a = (InvokeDynamicInsnNode) insnA;
                InvokeDynamicInsnNode b = (InvokeDynamicInsnNode) insnB;

                if (!a.bsm.equals(b.bsm)) return false;

                if (isJavaLambdaMetafactory(a.bsm)) {
                    Handle implA = (Handle) a.bsmArgs[1];
                    Handle implB = (Handle) b.bsmArgs[1];

                    if (implA.getTag() != implB.getTag()) return false;

                    return switch (implA.getTag()) {
                        case Opcodes.H_INVOKEVIRTUAL, Opcodes.H_INVOKESTATIC, Opcodes.H_INVOKESPECIAL, Opcodes.H_NEWINVOKESPECIAL, Opcodes.H_INVOKEINTERFACE ->
//                            logOriginalLambda(a, implA);
//                            logPatchedLambda(b, implB);

                                true; //Taken as true so that lambda renames don't count as the entire method being different

                        default -> throw new IllegalStateException("Unexpected impl tag: " + implA.getTag());
                    };
                } else if ("java/lang/invoke/StringConcatFactory".equals(a.bsm.getOwner())) {
                    return true; //Could check the end result format...
                } else if ("java/lang/runtime/ObjectMethods".equals(a.bsm.getOwner())) {
                    return Objects.equals(a.name, b.name) && Arrays.asList(a.bsmArgs).subList(2, a.bsmArgs.length).equals(Arrays.asList(b.bsmArgs).subList(2, b.bsmArgs.length));
                } else {
                    throw new IllegalStateException(String.format("Unknown invokedynamic bsm: %s#%s%s (tag=%d iif=%b)", a.bsm.getOwner(), a.bsm.getName(), a.bsm.getDesc(), a.bsm.getTag(), a.bsm.isInterface()));
                }
            }

            case AbstractInsnNode.JUMP_INSN: {
                JumpInsnNode a = (JumpInsnNode) insnA;
                JumpInsnNode b = (JumpInsnNode) insnB;

                //Check if the 2 jumps have the same direction, possibly should check if it's to the same positioned labels
                return Integer.signum(listA.indexOf(a.label) - listA.indexOf(a)) == Integer.signum(listB.indexOf(b.label) - listB.indexOf(b));
            }

            case AbstractInsnNode.LDC_INSN: {
                LdcInsnNode a = (LdcInsnNode) insnA;
                LdcInsnNode b = (LdcInsnNode) insnB;
                Class<?> typeClsA = a.cst.getClass();

                if (typeClsA != b.cst.getClass()) return false;

                if (typeClsA == Type.class) {
                    Type typeA = (Type) a.cst;
                    Type typeB = (Type) b.cst;

                    if (typeA.getSort() != typeB.getSort()) return false;

                    switch (typeA.getSort()) {
                        case Type.ARRAY, Type.OBJECT -> {
                            return Objects.equals(typeA.getDescriptor(), typeB.getDescriptor());
                        }
                        case Type.METHOD -> throw new UnsupportedOperationException("Bad sort: " + typeA);
                    }
                } else {
                    return a.cst.equals(b.cst);
                }
            }

            case AbstractInsnNode.IINC_INSN: {
                IincInsnNode a = (IincInsnNode) insnA;
                IincInsnNode b = (IincInsnNode) insnB;

                return a.incr == b.incr && a.var == b.var;
            }

            case AbstractInsnNode.TABLESWITCH_INSN: {
                TableSwitchInsnNode a = (TableSwitchInsnNode) insnA;
                TableSwitchInsnNode b = (TableSwitchInsnNode) insnB;

                return a.min == b.min && a.max == b.max;
            }

            case AbstractInsnNode.LOOKUPSWITCH_INSN: {
                LookupSwitchInsnNode a = (LookupSwitchInsnNode) insnA;
                LookupSwitchInsnNode b = (LookupSwitchInsnNode) insnB;

                return a.keys.equals(b.keys);
            }

            case AbstractInsnNode.MULTIANEWARRAY_INSN: {
                MultiANewArrayInsnNode a = (MultiANewArrayInsnNode) insnA;
                MultiANewArrayInsnNode b = (MultiANewArrayInsnNode) insnB;

                return a.dims == b.dims && Objects.equals(a.desc, b.desc);
            }

            case AbstractInsnNode.INSN:
            case AbstractInsnNode.LABEL: {
                return true; //Doesn't need any additional comparisons
            }

            case AbstractInsnNode.LINE:
            case AbstractInsnNode.FRAME:
            default:
                throw new IllegalArgumentException("Unexpected instructions: " + insnA + ", " + insnB);
        }
    }

    static boolean isJavaLambdaMetafactory(Handle bsm) {
        return bsm.getTag() == Opcodes.H_INVOKESTATIC
                && "java/lang/invoke/LambdaMetafactory".equals(bsm.getOwner())
                && ("metafactory".equals(bsm.getName())
                && "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;".equals(bsm.getDesc())
                || "altMetafactory".equals(bsm.getName())
                && "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;".equals(bsm.getDesc())
        ) && !bsm.isInterface();
    }
}
