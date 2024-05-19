package top.sunmoonbay.gradle.utils.mixin;

import org.gradle.internal.impldep.com.jcraft.jsch.HASH;
import org.gradle.internal.impldep.it.unimi.dsi.fastutil.Hash;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import top.sunmoonbay.gradle.utils.asm.AnnotationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MixinUtils {
    public final List<String> mixins = new ArrayList<>();
    public final List<String> targets = new ArrayList<>();
    public final ArrayList<String> methodOverwrites = new ArrayList<String>();
    public final ArrayList<String> fieldShadows = new ArrayList<String>();
    public final ArrayList<String> methodShadows = new ArrayList<String>();
    public final HashMap<String, String> invokes = new HashMap<>();
    public final HashMap<String, String> accessors = new HashMap<>();
    public final HashMap<String, List<String>> methods = new HashMap<>();
    public String className = null;
    public void accept(byte[] basic) {
        ClassReader classReader = new ClassReader(basic);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        HashMap<String, String> mixin = new HashMap<>();

        if (classNode.invisibleAnnotations == null) {
            return;
        }

        for (AnnotationNode invisibleAnnotation : classNode.invisibleAnnotations) {
            if (invisibleAnnotation.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")) {
                className = classNode.name;
                List<Type> values = AnnotationUtils.getAnnotationValue(invisibleAnnotation, "value");
                if (values != null) {
                    for (Type type : values) {
                        mixin.put(className, type.getClassName().replace(".", "/"));
                        mixins.add(type.getClassName().replace(".", "/"));
                    }
                }
            }
        }

        if (className == null) {
            return;
        }

        for (MethodNode methodNode : classNode.methods) {

            if (methodNode.visibleAnnotations == null) {
                continue;
            }

            for (AnnotationNode visibleAnnotation : methodNode.visibleAnnotations) {
                if (visibleAnnotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;")
                        || visibleAnnotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Redirect;")) {

                    List<String> method = AnnotationUtils.getAnnotationValue(visibleAnnotation, "method");
                    if (method != null) {
                        methods.put(methodNode.desc, method);
                    }

                    List<AnnotationNode> at = AnnotationUtils.getAnnotationValue(visibleAnnotation, "at");
                    if (at != null) {
                        String target = AnnotationUtils.getAnnotationValue(visibleAnnotation, "target");
                        if (target != null) {
                            targets.add(target);
                        }
                    }
                }

                if (visibleAnnotation.desc.equals("Lorg/spongepowered/asm/mixin/gen/Invoker;")) {
                    String value = AnnotationUtils.getAnnotationValue(visibleAnnotation, "value");
                    if (value != null) {
                        invokes.put(methodNode.desc, value);
                    }
                }

                if (visibleAnnotation.desc.equals("Lorg/spongepowered/asm/mixin/gen/Accessor;")) {
                    String value = AnnotationUtils.getAnnotationValue(visibleAnnotation, "value");
                    if (value != null) {
                        accessors.put(methodNode.desc, value);
                    }
                }

                if (visibleAnnotation.desc.equals("Lorg/spongepowered/asm/mixin/Overwrite;")) {
                    methodOverwrites.add(className + ";" + mixin.get(className) + ":" + methodNode.name);
                }

                if (visibleAnnotation.desc.equals("Lorg/spongepowered/asm/mixin/Shadow;")) {
                    methodShadows.add(className + ";" + mixin.get(className) + ":" + methodNode.name);
                }
            }
        }

        for (FieldNode fieldNode : classNode.fields) {
            if (fieldNode.visibleAnnotations == null) {
                continue;
            }

            for (AnnotationNode visibleAnnotation : fieldNode.visibleAnnotations) {
                if (visibleAnnotation.desc.equals("Lorg/spongepowered/asm/mixin/Shadow;")) {
                    fieldShadows.add(className + ";" + mixin.get(className) + ":" + fieldNode.name);
                }
            }
        }
    }
}
