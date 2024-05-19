package top.sunmoonbay.gradle.utils.mapping;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;

import static org.objectweb.asm.Opcodes.ASM9;

public class RemappingUtil {

    private static final ConcurrentHashMap<String, RemappingUtil> instanceMap = new ConcurrentHashMap<>();

    public static RemappingUtil getInstance(String name, Map<String, String> map) {
        if (!instanceMap.containsKey(name)) {
            instanceMap.put(name, new RemappingUtil(map));
        }
        return instanceMap.get(name);
    }

    private final Map<String, String> map;

    public final Map<String, Set<String>> superHashMap = new HashMap<>();

    private RemappingUtil(Map<String, String> map) {
        this.map = map;
    }

    public void analyzeJar(File inputFile) {
        try {
            JarFile jarFile = new JarFile(inputFile);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                if (jarEntry.isDirectory())
                    continue;
                if (!jarEntry.getName().endsWith(".class"))
                    continue;

                analyze(IOUtils.toByteArray(jarFile.getInputStream(jarEntry)));

            }
            jarFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void analyze(byte[] bytes) {
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(new ClassVisitor(ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                Set<String> strings = new HashSet<>();
                if (superHashMap.containsKey(name)) {
                    if (superName != null) {
                        if (!superHashMap.get(name).contains(superName)) {
                            strings.add(superName);
                        }
                    }

                    if (interfaces != null) {
                        for (String inter : interfaces) {
                            if (!superHashMap.get(name).contains(inter)) {
                                strings.add(inter);
                            }
                        }
                    }
                    superHashMap.get(name).addAll(strings);
                } else {
                    if (superName != null) {
                        strings.add(superName);
                    }

                    if (interfaces != null) {
                        Collections.addAll(strings, interfaces);
                    }
                    superHashMap.put(name, strings);
                }
                super.visit(version, access, name, signature, superName, interfaces);
            }
        }, 0);
    }

    public void remappingJar(File inputFile, File outFile) throws IOException {
        analyzeJar(inputFile);
        ZipOutputStream jarOutputStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)), StandardCharsets.UTF_8);
        JarFile jarFile = new JarFile(inputFile);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            if (jarEntry.isDirectory())
                continue;
            if (jarEntry.getName().endsWith(".class")) {
                String name = map.get(jarEntry.getName().replace(".class", ""));
                if (name != null) {
                    name += ".class";
                } else {
                    name = jarEntry.getName();
                }
                jarOutputStream.putNextEntry(new JarEntry(name));
                remappingClass(jarFile.getInputStream(jarEntry), jarOutputStream);
            } else {
                if (jarEntry.getName().endsWith("MANIFEST.MF"))
                    continue;

                jarOutputStream.putNextEntry(new JarEntry(jarEntry.getName()));
                IOUtils.copy(jarFile.getInputStream(jarEntry), jarOutputStream);
            }
            jarOutputStream.closeEntry();
        }
        jarFile.close();
        jarOutputStream.close();
    }

    private void remappingClass(InputStream input, OutputStream output) throws IOException {
        output.write(remapping(IOUtils.toByteArray(input)));
        output.flush();
    }

    public byte[] remapping(byte[] bytes) {
        ClassReader classReader = new ClassReader(bytes);
        ClassWriter classWriter = new ClassWriter(0);
        ClassRemapper classRemapper = new ClassRemapper(new ClassVisitor(ASM9, classWriter) {
        }, simpleRemapper(map));
        classReader.accept(classRemapper, 0);
        return classWriter.toByteArray();
    }

    public byte[] remapping(byte[] bytes,
                            Map<String, String> methodCleanObf,
                            ArrayList<String> methodOverwrites,
                            Map<String, String> fieldCleanObf,
                            ArrayList<String> fieldShadows,
                            ArrayList<String> methodShadows) {
        HashMap<String, String> methodObfMap = new HashMap<>();
        HashMap<String, String> fieldObfMap = new HashMap<>();

        for (Map.Entry<String, String> stringStringEntry : methodCleanObf.entrySet()) {
            String key = stringStringEntry.getKey().split(" ")[0];
            String value = stringStringEntry.getValue().split(" ")[0];

            methodObfMap.put(key, value);
        }

        for (Map.Entry<String, String> stringStringEntry : fieldCleanObf.entrySet()) {
            String key = stringStringEntry.getKey().split(" ")[0];
            String value = stringStringEntry.getValue().split(" ")[0];

            fieldObfMap.put(key, value);
        }

        ClassReader classReader = new ClassReader(bytes);
        ClassWriter classWriter = new ClassWriter(0);
        ClassRemapper classRemapper = new ClassRemapper(new ClassVisitor(ASM9, classWriter) {
            String className = null;

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                className = name;

                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                for (String methodOverwrite : methodOverwrites) {
                    String methodNames = methodOverwrite.substring(methodOverwrite.indexOf(":") + 1);

                    if (name.equals(methodNames)) {
                        String trim = methodOverwrite.substring(methodOverwrite.indexOf(";") + 1).replace(":", ".").trim();
                        String methodObf = methodObfMap.getOrDefault(trim, trim);

                        return super.visitMethod(access, methodObf.substring(methodObf.indexOf(".") + 1), descriptor, signature, exceptions);
                    }
                }

                for (String methodShadow : methodShadows) {
                    String methodNames = methodShadow.substring(methodShadow.indexOf(":") + 1);

                    if (name.equals(methodNames)) {
                        String trim = methodShadow.substring(methodShadow.indexOf(";") + 1).replace(":", ".").trim();
                        String methodObf = methodObfMap.getOrDefault(trim, trim);

                        return super.visitMethod(access, methodObf.substring(methodObf.indexOf(".") + 1), descriptor, signature, exceptions);
                    }
                }


                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                for (String fieldShadows : fieldShadows) {
                    String fieldNames = fieldShadows.substring(fieldShadows.indexOf(":") + 1);

                    if (name.equals(fieldNames)) {
                        String trim = fieldShadows.substring(fieldShadows.indexOf(";") + 1).replace(":", ".").trim();
                        String fieldObf = fieldObfMap.getOrDefault(trim, trim);

                        String substring = fieldObf.substring(fieldObf.indexOf(".") + 1);

                        return super.visitField(access, substring, descriptor, signature, value);
                    }
                }

                return super.visitField(access, name, descriptor, signature, value);
            }
        }, simpleRemapper(map));
        classReader.accept(classRemapper, 0);
        return classWriter.toByteArray();
    }

    private SimpleRemapper simpleRemapper(Map<String, String> map) {
        return new SimpleRemapper(map) {
            @Override
            public String mapFieldName(String owner, String name, String descriptor) {
                String remappedName = map(owner + '.' + name);
                if (remappedName == null) {
                    if (superHashMap.containsKey(owner)) {
                        for (String s : superHashMap.get(owner)) {
                            String rn = mapFieldName(s, name, descriptor);
                            if (rn != null) {
                                return rn;
                            }
                        }
                    }
                }


                return remappedName == null ? name : remappedName;
            }

            @Override
            public String mapMethodName(String owner, String name, String descriptor) {
                String remappedName = map(owner + '.' + name + descriptor);
                if (remappedName == null) {
                    if (superHashMap.containsKey(owner)) {
                        for (String s : superHashMap.get(owner)) {
                            String rn = mapMethodName(s, name, descriptor);
                            if (rn != null) {
                                return rn;
                            }
                        }
                    }
                }
                return remappedName == null ? name : remappedName;
            }
        };
    }
}
