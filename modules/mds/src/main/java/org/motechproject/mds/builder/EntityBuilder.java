package org.motechproject.mds.builder;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import org.motechproject.mds.ex.EntityBuilderException;
import org.motechproject.mds.service.JDOClassLoader;

import java.io.IOException;

public class EntityBuilder {
    private static final String PACKAGE = "org.motechproject.mds.domain";

    private String className;
    private JDOClassLoader classLoader;

    public EntityBuilder withSingleName(String simpleName) {
        return withClassName(String.format("%s.%s", PACKAGE, simpleName));
    }

    public EntityBuilder withClassName(String className) {
        this.className = className;
        return this;
    }

    public EntityBuilder withClassLoader(JDOClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    public String getClassName() {
        return className;
    }

    public byte[] build() {
        try {
            CtClass ctClass = ClassPool.getDefault().makeClass(className);
            byte[] classBytes = ctClass.toBytecode();
            classLoader.defineClass(className, classBytes);
            return classBytes;
        } catch (IOException | CannotCompileException e) {
            throw new EntityBuilderException();
        }
    }
}
