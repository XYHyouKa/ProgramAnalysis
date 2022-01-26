package com.xyhyouka.sootbasic;

import soot.Body;
import soot.BodyTransformer;
import soot.SootMethod;
import soot.tagkit.GenericAttribute;
import soot.tagkit.Tag;

import java.util.Map;

/**
 * It simply adds a string "Hello soot!" as an attribute to every method.
 * The attribute has the name "Example".
 */
public class AnnExampleWrapper extends BodyTransformer
{
    private static AnnExampleWrapper instance = new AnnExampleWrapper();

    private AnnExampleWrapper() {};

    public static AnnExampleWrapper v()
    {
        return instance;
    }

    public void internalTransform(Body body, String phaseName, Map<String, String> options)
    {
        SootMethod method = body.getMethod();
        String attr = new String("Hello soot!");

        Tag example = new GenericAttribute("Example", attr.getBytes());
        method.addTag(example);
    }
}
