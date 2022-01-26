/** https://github.com/soot-oss/soot/wiki/Adding-attributes-to-class-files-%28Advanced%29
 * Should be familiar with SootClass, SootField, SootMethod, Body, Unit
 * Know about Host, AbstractHost, Tag, Attribute classes
 *
 * Before running this, you should make sure foo.class is in SootBasic/sootOutput
 */
package com.xyhyouka.sootbasic;

import soot.PackManager;
import soot.Transform;
import soot.options.Options;

public class AddAttributesAdvanced {

    public static void main(String[] args) {

        String cp = System.getProperty("java.class.path");
        String fSeparator = System.getProperty("file.separator");
        String pSeparator = System.getProperty("path.separator");
        String projPath = System.getProperty("user.dir");

        cp = projPath + fSeparator + "sootOutput" + pSeparator + cp;
        Options.v().set_soot_classpath(cp);
        Options.v().set_output_dir("sootOutput" + fSeparator + "addAttributesAdvanced");

        /* adds the transformer. */
        PackManager.v().getPack("jtp").add(new
                        Transform("jtp.annotexample",
                        AnnExampleWrapper.v()));

        /* invokes Soot */
        soot.Main.main(new String[]{"foo"});
    }
}
