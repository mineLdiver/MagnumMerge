package net.mine_diver.magnummerge.test;

import net.mine_diver.magnummerge.MagnumMerge;
import net.mine_diver.magnummerge.util.MagnumASM;
import org.objectweb.asm.ClassWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Test {
    private static final Path
            RUN_DIR = Path.of("run"),
            TEST_MIXIN_FILE = RUN_DIR.resolve("TestMixin.class");

    public static void main(String[] args) throws IOException {
        var originalClass = MagnumASM.readClassNode(OriginalCode.class);
        var modifiedClass = MagnumASM.readClassNode(ModifiedCode.class);

        var mixinClass = MagnumMerge.generateMixin(originalClass, modifiedClass);

        var classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        mixinClass.accept(classWriter);
        Files.createDirectories(RUN_DIR);
        if (Files.notExists(TEST_MIXIN_FILE))
            Files.createFile(TEST_MIXIN_FILE);
        try (var fos = new FileOutputStream(TEST_MIXIN_FILE.toString())) {
            fos.write(classWriter.toByteArray());
        }
    }
}