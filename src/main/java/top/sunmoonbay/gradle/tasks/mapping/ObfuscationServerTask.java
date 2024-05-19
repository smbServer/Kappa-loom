package top.sunmoonbay.gradle.tasks.mapping;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import top.sunmoonbay.gradle.utils.game.GameUtils;
import top.sunmoonbay.gradle.utils.mapping.MappingUtil;
import top.sunmoonbay.gradle.utils.mapping.RemappingUtil;
import top.sunmoonbay.gradle.utils.mixin.MixinUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class ObfuscationServerTask extends MappingTaskBase {
    @TaskAction
    public void obfuscation() {
        File classes = getProject().getLayout().getBuildDirectory().dir("classes").get().getAsFile();

        MappingUtil mappingUtil = MappingUtil.getInstance(extension.mapping.mappingFile);
        RemappingUtil remappingUtil = RemappingUtil.getInstance("obfuscation", mappingUtil.getMap(false));
        remappingUtil.analyzeJar(GameUtils.getServerCleanFile(extension));

        JsonObject mixinReferenceMap = new JsonObject();
        JsonObject mixinMappings = new JsonObject();

        if (extension.referenceMap != null) {
            out.info("Generate mixin reference map");

            try {
                Files.walkFileTree(classes.toPath(), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        byte[] bytes = FileUtils.readFileToByteArray(file.toFile());
                        MixinUtils mixinMapping = new MixinUtils();
                        mixinMapping.accept(bytes);
                        remappingUtil.superHashMap.put(mixinMapping.className, new HashSet<>(mixinMapping.mixins));
                        for (String mixin : mixinMapping.mixins) {
                            JsonObject mapping = new JsonObject();

                            mixinMapping.methods.forEach((descriptor, methods) -> {
                                for (String method : methods) {
                                    if (method.contains("(")) {
                                        mapping.addProperty(method, getMethodObf(mappingUtil.methodCleanToObfMap, mixin, method, false));
                                    } else {
                                        mapping.addProperty(method, getMethodObf(mappingUtil.methodCleanToObfMap, mixin, method + descriptor.replace("Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;", ""), false));
                                    }
                                }
                            });

                            for (String mixinTarget : mixinMapping.targets) {
                                if (!mixinTarget.contains("field:")) {
                                    String targetClass = mixinTarget.substring(1, mixinTarget.indexOf(";"));

                                    String targetMethod = getMethodObf(mappingUtil.methodCleanToObfMap, targetClass, mixinTarget.substring(mixinTarget.indexOf(";") + 1), false);
                                    if (targetMethod == null) {
                                        continue;
                                    }
                                    mapping.addProperty(mixinTarget, targetMethod);
                                } else {
                                    String left = mixinTarget.split("field:")[0];
                                    String right = mixinTarget.split("field:")[1];
                                    String targetClass = mappingUtil.classCleanToObfMap.get(left.substring(1, left.indexOf(";")));
                                    String targetField = mappingUtil.classCleanToObfMap.get(right.substring(1, right.indexOf(";")));

                                    if (targetClass == null || targetField == null) {
                                        continue;
                                    }

                                    mapping.addProperty(mixinTarget, "L" + targetClass + ";field:L" + targetField + ";");
                                }
                            }

                            for (Map.Entry<String, String> entry : mixinMapping.accessors.entrySet()) {

                                String fieldName = mappingUtil.fieldCleanToObfMap.get(mixin + "/" + entry.getValue());

                                if (fieldName == null) {
                                    continue;
                                }

                                if (entry.getKey().contains(";")) {
                                    String arg;
                                    if (!entry.getKey().contains(")V")) {
                                        arg = entry.getKey().substring(entry.getKey().lastIndexOf(")") + 1);
                                    } else {
                                        arg = entry.getKey().substring(entry.getKey().indexOf("(") + 1, entry.getKey().lastIndexOf(")"));
                                    }

                                    arg = arg.substring(1, arg.lastIndexOf(";"));
                                    arg = mappingUtil.classCleanToObfMap.get(arg);
                                    if (arg == null) {
                                        continue;
                                    }
                                    mapping.addProperty(entry.getValue(), fieldName.split("/")[1] + ":L" + arg + ";");
                                } else {
                                    mapping.addProperty(entry.getValue(), entry.getKey());
                                }
                            }

                            for (Map.Entry<String, String> entry : mixinMapping.invokes.entrySet()) {
                                mapping.addProperty(entry.getValue(), getMethodObf(mappingUtil.methodCleanToObfMap, mixin, entry.getValue() + entry.getKey(), false));
                            }

                            mixinMappings.add(mixinMapping.className, mapping);
                        }
                        return super.visitFile(file, attrs);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
            mixinReferenceMap.add("mappings", mixinMappings);
        }

        JavaPluginExtension java = getProject().getExtensions().getByType(JavaPluginExtension.class);
        File resourceDir = getProject().getLayout().getBuildDirectory().dir("resources").get().getAsFile();
        for (SourceSet sourceSet : java.getSourceSets()) {
            if (!resourceDir.exists()) {
                resourceDir.mkdir();
            }
            File dir = new File(resourceDir, sourceSet.getName());
            if (!dir.exists()) {
                dir.mkdir();
            }

            if (extension.referenceMap != null) {
                try {
                    FileUtils.write(new File(dir, extension.referenceMap), new GsonBuilder().setPrettyPrinting().create().toJson(mixinReferenceMap), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
        }

        out.info("Obfuscating...");

        try {
            Files.walkFileTree(classes.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    byte[] bytes = FileUtils.readFileToByteArray(file.toFile());
                    MixinUtils mixinMapping = new MixinUtils();
                    mixinMapping.accept(bytes);
                    Files.write(file, remappingUtil.remapping(FileUtils.readFileToByteArray(file.toFile()),
                            mappingUtil.methodCleanToObfMap,
                            mixinMapping.methodOverwrites,
                            mappingUtil.fieldCleanToObfMap,
                            mixinMapping.fieldShadows,
                            mixinMapping.methodShadows));

                    remappingUtil.analyze(FileUtils.readFileToByteArray(file.toFile()));
                    return super.visitFile(file, attrs);
                }
            });
        } catch (IOException e) {
            getProject().getLogger().lifecycle(e.getMessage(), e);
        }

        out.info("Obfuscated.");
    }

    private String getMethodObf(Map<String, String> methodCleanToObfMap, String klass, String method, boolean only) {
        String methodName = method.substring(0, method.indexOf("("));
        String methodDescriptor = method.substring(method.indexOf("("));
        String methodObf = methodCleanToObfMap.get(klass + "." + methodName + " " + methodDescriptor);
        if (methodObf == null) {
            return null;
        }

        if (!only) {
            methodObf = "L" + methodObf.split(" ")[0].replace(".", ";") + methodObf.split(" ")[1];
        } else {
            methodObf = methodObf.split(" ")[0];
            methodObf = methodObf.substring(methodObf.lastIndexOf(".") + 1);
        }
        return methodObf;
    }
}
