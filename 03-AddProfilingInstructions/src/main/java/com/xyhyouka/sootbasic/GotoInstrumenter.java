/** https://github.com/soot-oss/soot/wiki/Adding-profiling-instructions-to-applications
 *
 * 1.Add a static field gotoCount to the main class.
 * 2.Insert instructions incrementing gotoCount before each goto instruction in each method.
 * 3.Insert gotoCount print-out instructions before each return statement in 'main' method.
 * 4.Insert gotoCount print-out statements before each System.exit() invocation in each method.
 */
package com.xyhyouka.sootbasic;

import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.LongType;
import soot.Modifier;
import soot.PackManager;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.GotoStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.LongConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.options.Options;
import soot.util.Chain;

import java.util.Iterator;
import java.util.Map;

public class GotoInstrumenter extends BodyTransformer {

    private static boolean addedFieldToMainClassAndLoadedPrintStream;

    private static GotoInstrumenter instance = new GotoInstrumenter();

    static {
        addedFieldToMainClassAndLoadedPrintStream = false;
    }

    private GotoInstrumenter() {}

    public static GotoInstrumenter v() {
        return instance;
    }

    /*
     * internalTransform goes through a method body and inserts counter
     * instructions before an GOTO instruction
     */
    protected void internalTransform(Body body, String phaseName, Map<String, String> options) {

        if (!Scene.v().getMainClass().declaresMethod("void main(java.lang.String[])"))
            throw new RuntimeException("couldn't find main() in mainClass");

        // SootClass javaIoPrintStream;
        boolean isMainMethod = body.getMethod().getSubSignature().equals("void main(java.lang.String[])");
        SootMethod method = body.getMethod();
        Chain<Unit> units = body.getUnits();
        SootField gotoCounter;

        System.out.println("Instrumenting method: " + method.getSignature());

        if (addedFieldToMainClassAndLoadedPrintStream) {
            gotoCounter = Scene.v().getMainClass().getFieldByName("gotoCount");
        } else {
            // Add gotoCounter field
            gotoCounter = new SootField("gotoCount", LongType.v(), Modifier.STATIC);
            Scene.v().getMainClass().addField(gotoCounter);

            // Just in case, resolve the PrintStream SootClass.
            Scene.v().loadClassAndSupport("java.io.PrintStream");
            // javaIoPrintStream = Scene.v().getSootClass("java.io.PrintStream");

            addedFieldToMainClassAndLoadedPrintStream = true;
        }






        Local tmpCount = Jimple.v().newLocal("tmpCount", LongType.v());
        body.getLocals().add(tmpCount);


        // systemOut = java.lang.System.out;
        Local systemOut = Jimple.v().newLocal("systemOut", RefType.v("java.io.PrintStream"));
        body.getLocals().add(systemOut);

        Iterator stmtIt = units.snapshotIterator();
        while (stmtIt.hasNext()) {
            Stmt s = (Stmt) stmtIt.next();
            if (s instanceof GotoStmt) {
                /* Insert profiling instructions before s. */
                // gotoCount = gotoCount + 1;
                units.insertBefore(Jimple.v().newAssignStmt(tmpCount, Jimple.v().newStaticFieldRef(gotoCounter.makeRef())), s);
                units.insertBefore(Jimple.v().newAssignStmt(tmpCount, Jimple.v().newAddExpr(tmpCount, LongConstant.v(1L))), s);
                units.insertBefore(Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(gotoCounter.makeRef()), tmpCount), s);
            } else if (s instanceof InvokeStmt) {
                /* Check if it is a System.exit() statement.
                 * If it is, insert print-out statement before s.
                 */
                InvokeExpr iExpr = (InvokeExpr) ((InvokeStmt) s).getInvokeExpr();
                if (iExpr instanceof StaticInvokeExpr) {
                    SootMethod target = ((StaticInvokeExpr) iExpr).getMethod();
                    if (target.getSignature().equals("<java.lang.System: void exit(int)>")) {
                        /* insert printing statements here */
                        units.insertBefore(Jimple.v().newAssignStmt(systemOut, Jimple.v().newStaticFieldRef(Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())), s);
                        SootMethod toPrintln = Scene.v().getMethod("<java.io.PrintStream: void println(long)>");
                        // systemOut.println(gotoCount);
                        units.insertBefore(Jimple.v().newAssignStmt(tmpCount, Jimple.v().newStaticFieldRef(gotoCounter.makeRef())), s);
                        units.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(systemOut, toPrintln.makeRef(), tmpCount)), s);
                    }
                }
            } else if (isMainMethod && (s instanceof ReturnStmt || s instanceof ReturnVoidStmt)) {
                /* In the main method, before the return statement, insert
                 * print-out statements.
                 */
                units.insertBefore(Jimple.v().newAssignStmt(systemOut, Jimple.v().newStaticFieldRef(Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())), s);
                SootMethod toPrintln = Scene.v().getMethod("<java.io.PrintStream: void println(long)>");
                // systemOut.println(gotoCount);
                units.insertBefore(Jimple.v().newAssignStmt(tmpCount, Jimple.v().newStaticFieldRef(gotoCounter.makeRef())), s);
                units.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(systemOut, toPrintln.makeRef(), tmpCount)), s);
            }
        }
    }

    public static void main(String[] args) {

        String cp = System.getProperty("java.class.path");
        String fSeparator = System.getProperty("file.separator");
        String pSeparator = System.getProperty("path.separator");
        String projPath = System.getProperty("user.dir");

        cp = projPath + fSeparator + "sootOutput" + pSeparator + cp;
        Options.v().set_soot_classpath(cp);
        Options.v().set_output_dir("sootOutput" + fSeparator + "GotoInstrumenter");

        /* adds the transformer. */
        PackManager.v().getPack("jtp").add(new
                        Transform("jtp.GotoInstrumenter",
                        GotoInstrumenter.v()));

        /* invokes Soot */
        soot.Main.main(new String[]{"-w", "SimpleGoto"});
    }
}
