package com.xyhyouka.sootbasic;

import soot.AbstractJasminClass;
import soot.ArrayType;
import soot.Local;
import soot.Modifier;
import soot.PackManager;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Transform;
import soot.Type;
import soot.Unit;
import soot.VoidType;
import soot.baf.Baf;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.StringConstant;
import soot.options.Options;
import soot.tagkit.GenericAttribute;
import soot.tagkit.Tag;
import soot.tagkit.TagAggregator;
import soot.util.Chain;
import soot.util.JasminOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * attribute_info {
 *     u2 attribute_name_index;
 *     u4 attribute_length;
 *     u1 info[attribute_length];
 * }
 *
 * Extends CreateHelloWorld.java and do three things:
 * - add attributes
 * - tag Units
 * - tag Aggregators
 */

public class AddAttributesBasic {

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

        // using baf without this will cause error
        Scene.v().loadNecessaryClasses();
        //Scene.v().setSootClassPath(System.getProperty("java.class.path"));

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
            Unit tmpUnit;
            Chain<Unit> units;

            // Add Local java.lang.String l0
            arg = Jimple.v().newLocal("l0", ArrayType.v(RefType.v("java.lang.String"), 1));
            jBody.getLocals().add(arg);
            // Add Local java.io.PrintStream tmpRef
            tmpRef = Jimple.v().newLocal("tmpRef", RefType.v("java.io.PrintStream"));
            jBody.getLocals().add(tmpRef);

            /**
             * Tag Units begin
             */
            // Add Unit "l0 = @parameter0" and tag
            units = jBody.getUnits();
            tmpUnit = Jimple.v().newIdentityStmt(arg,
                    Jimple.v().newParameterRef(ArrayType.v
                            (RefType.v("java.lang.String"), 1), 0));
            tmpUnit.addTag(new MyTag(1));
            units.add(tmpUnit);
            // Add Unit "tmpRef = java.lang.System.out"
            units.add(Jimple.v().newAssignStmt(tmpRef, Jimple.v().newStaticFieldRef(
                    Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())));

            // Insert "tmpRef.println("Hello world!")"
            SootMethod toCall = Scene.v().getMethod
                    ("<java.io.PrintStream: void println(java.lang.String)>");
            tmpUnit = Jimple.v().newInvokeStmt
                    (Jimple.v().newVirtualInvokeExpr
                            (tmpRef, toCall.makeRef(), StringConstant.v("Hello world!")));
            tmpUnit.addTag(new MyTag(2));
            units.add(tmpUnit);
            /**
             * Tag Units end
             */

            // Insert "return"
            units.add(Jimple.v().newReturnVoidStmt());
        }

        /**
         * add attributes part begin
         */
        // create and add the class attribute, with data "foo"
        GenericAttribute classAttr = new GenericAttribute(
                "ca.mcgill.sable.MyClassAttr",
                "foo".getBytes());
        sClass.addTag(classAttr);

        // Create and add the method attribute with no data
        GenericAttribute mAttr = new GenericAttribute(
                "ca.mcgill.sable.MyMethodAttr",
                "".getBytes());
        sMethod.addTag(mAttr);
        /**
         * add attributes part end
         */

        /** TagAggregator transform method expects a Baf Body
         * We must first convert the Body to Baf
         */
        MyTagAggregator mta = new MyTagAggregator();
        // convert the body to Baf
        sMethod.setActiveBody(Baf.v().newBody((JimpleBody) sMethod.getActiveBody()));
        // aggregate the tags and produce a CodeAttribute
        mta.transform(sMethod.getActiveBody());
        PackManager.v().getPack("tag").add(new Transform("tag.mta",
                new MyTagAggregator()));

        // Write to class file (modified to fit Baf body
        String fSeparator = System.getProperty("file.separator");
        Options.v().set_output_dir("sootOutput" + fSeparator + "addAttributesBasic");
        String fileName = SourceLocator.v().getFileNameFor(sClass, Options.output_format_class);
        OutputStream streamOut = new JasminOutputStream(new FileOutputStream(fileName));
        PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
        AbstractJasminClass jasminClass = new soot.baf.JasminClass(sClass);
        jasminClass.print(writerOut);
        writerOut.flush();
        streamOut.close();
        System.out.println("Class file generate finished.");
        System.out.println("Go to SootBasic/addAttributesBasic/sootOutput and use \"javap -verbose HelloWorld\" to run.");
        System.out.println("The attributes added will be found.");
    }
}

class MyTag implements Tag {

    int value;

    public MyTag(int value) {
        this.value = value;
    }

    public String getName() {
        return "ca.mcgill.sable.MyTag";
    }

    // output the value as a 4-byte array
    public byte[] getValue() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeInt(value);
            dos.flush();
        } catch(IOException e) {
            System.err.println(e);
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }
}

/**
 * In order to convert the Tags on statements into a code attribute that can be written out in the class file,
 * we must define a TagAggregator. A TagAggregator is a Soot BodyTransformer that accepts a Body with
 * tagged instructions, and produces a Body with an equivalent code attribute. We could use the GenericAttribute class
 * to represent the attribute structure written in the class file; in this section, we will be using
 * Soot's CodeAttribute class, which is a default implementation of a bytecode offset to value table.
 */
class MyTagAggregator extends TagAggregator {

    public String aggregatedName() {
        return "ca.mcgill.sable.MyTag";
    }

    public boolean wantTag(Tag t) {
        return (t instanceof MyTag);
    }

    public void considerTag(Tag t, Unit u, LinkedList<Tag> tags, LinkedList<Unit> units) {
        tags.add(t);
        units.add(u);
    }
}
