package com.xyhyouka.sootbasic;

import soot.ArrayType;
import soot.Local;
import soot.Modifier;
import soot.Printer;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Type;
import soot.Unit;
import soot.VoidType;
import soot.baf.BafASMBackend;
import soot.jimple.JasminClass;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.StringConstant;
import soot.options.Options;
import soot.util.Chain;
import soot.util.JasminOutputStream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;

public class CreateHelloWorld {

    public static void main(String[] args) throws IOException {

        SootClass sClass;
        SootMethod sMethod;

        /* Loading java.lang.Object and Library Classes
         * This step is not necessary when building code that extends the Soot framework;
         * in that case, loading of classfiles is already done when user code is called.
         */
        Scene.v().loadClassAndSupport("java.lang.Object");

        // Since our HelloWorld program will be using classes in the standard library,
        // we must also resolve these:
        Scene.v().loadClassAndSupport("java.lang.System");

        // Create the 'HelloWorld' SootClass, and set its super class as ''java.lang.Object''.
        sClass = new SootClass("HelloWorld", Modifier.PUBLIC);
        sClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));

        // Add the newly-created HelloWorld class to the Scene.
        // All classes should belong to the Scene once they are created.
        Scene.v().addClass(sClass);

        // Create "public static void main(String[])" method for HelloWorld with an empty body.
        sMethod = new SootMethod("main",
                Arrays.asList(new Type[] {ArrayType.v(RefType.v("java.lang.String"), 1)}),
                VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);

        sClass.addMethod(sMethod);

        /* Add code to methods (Create the method body)
         * In Soot, we attach a Body to a SootMethod to associate some code with the method.
         * Each Body knows which SootMethod it corresponds to, but a SootMethod
         * only has one active Body at once (accessible via SootMethod.getActiveBody()).
         * Different types of Body's are provided by the various intermediate representations;
         * Soot has JimpleBody, ShimpleBody, BafBody and GrimpBody.
         * Here we choose to create a Jimple Body for main class:
         */
        {
            JimpleBody jBody = Jimple.v().newBody(sMethod);
            sMethod.setActiveBody(jBody);

            /* More precisely, a Body has three important features:
             * chains of Locals, Traps and Units.
             * A Chain is a list-like structure that provides O(1) access to insert and delete elements.
             * Locals are the local variables in the body;
             * Traps say which units catch which exceptions;
             * and Units are the statements themselves.
             */
            Local arg, tmpRef;
            Chain<Unit> units;

            // Add Local java.lang.String l0
            arg = Jimple.v().newLocal("l0", ArrayType.v(RefType.v("java.lang.String"), 1));
            jBody.getLocals().add(arg);
            // Add Local java.io.PrintStream tmpRef
            tmpRef = Jimple.v().newLocal("tmpRef", RefType.v("java.io.PrintStream"));
            jBody.getLocals().add(tmpRef);

            // Add Unit "l0 = @parameter0"
            units = jBody.getUnits();
            units.add(Jimple.v().newIdentityStmt(arg,
                    Jimple.v().newParameterRef(ArrayType.v
                            (RefType.v("java.lang.String"), 1), 0)));
            // Add Unit "tmpRef = java.lang.System.out"
            units.add(Jimple.v().newAssignStmt(tmpRef, Jimple.v().newStaticFieldRef(
                    Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())));

            // Insert "tmpRef.println("Hello world!")"
            SootMethod toCall = Scene.v().getMethod
                    ("<java.io.PrintStream: void println(java.lang.String)>");
            units.add(Jimple.v().newInvokeStmt
                    (Jimple.v().newVirtualInvokeExpr
                            (tmpRef, toCall.makeRef(), StringConstant.v("Hello world!"))));

            // Insert "return"
            units.add(Jimple.v().newReturnVoidStmt());
        }

        // Write to class file (3 ways to choose)
        // Options.v().set_java_version(Options.java_version_8);
        int outputMethod = 1;   // 0: Normal  1: ASM  2: Jasmin  other: jimple source instead of .class
        String fileName = SourceLocator.v().getFileNameFor(sClass, Options.output_format_class);
        if (outputMethod == 0) {
            // Generate raw source file (not java or class
            OutputStream streamOut = new FileOutputStream(fileName);
            PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
            Printer.v().printTo(sClass, writerOut);
            writerOut.flush();
            streamOut.close();
        } else if (outputMethod == 1) {
            // ASM backend (preferred)
            // using baf without this will cause error
            Scene.v().loadNecessaryClasses();
            //Scene.v().setSootClassPath(System.getProperty("java.class.path"));
            int java_version = Options.v().java_version();
            OutputStream streamOut = new FileOutputStream(fileName);
            // Although it's official preferred, it will cause series of Exception
            // Not recommended to use it
            BafASMBackend backend = new BafASMBackend(sClass, java_version);
            backend.generateClassFile(streamOut);
            streamOut.close();
        } else if (outputMethod == 2) {
            // Success
            // Jasmin backend (outdated)
            OutputStream streamOut = new JasminOutputStream(new FileOutputStream(fileName));
            PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
            JasminClass jasminClass = new soot.jimple.JasminClass(sClass);
            jasminClass.print(writerOut);
            writerOut.flush();
            streamOut.close();
        } else {
            // Output jimple source
            // We have omitted the JasminOutputStream, and are calling the printTo method on Printer.
            fileName = SourceLocator.v().getFileNameFor(sClass, Options.output_format_jimple);
            OutputStream streamOut = new FileOutputStream(fileName);
            PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
            Printer.v().printTo(sClass, writerOut);
            writerOut.flush();
            streamOut.close();
        }
        System.out.println("Class file generate finished.");
        System.out.println("Go to SootBasic/sootOutput and use \"java HelloWorld\" to run.");
    }
}
