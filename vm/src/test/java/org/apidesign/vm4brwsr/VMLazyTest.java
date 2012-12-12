/**
 * Back 2 Browser Bytecode Translator
 * Copyright (C) 2012 Jaroslav Tulach <jaroslav.tulach@apidesign.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://opensource.org/licenses/GPL-2.0.
 */
package org.apidesign.vm4brwsr;

import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.testng.annotations.BeforeClass;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/** Implements loading class by class.
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
public class VMLazyTest {

    
    private static CharSequence codeSeq;
    private static Invocable code;

    @BeforeClass
    public void compileTheCode() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("\nvar data = {};");
        sb.append("\nfunction test(clazz, method) {");
        sb.append("\n  if (!data.bck2brwsr) data.bck2brwsr = bck2brwsr(function(name) { return loader.get(name); });");
        sb.append("\n  var c = data.bck2brwsr.loadClass(clazz);");
        sb.append("\n  return c[method]();");
        sb.append("\n}");
        
        
       
        ScriptEngine[] arr = { null };
        code = StaticMethodTest.compileClass(sb, arr,
            "org/apidesign/vm4brwsr/VM"
        );
        arr[0].getContext().setAttribute("loader", new BytesLoader(), ScriptContext.ENGINE_SCOPE);
        codeSeq = sb;
    }
    
    @Test public void invokeStaticMethod() throws Exception {
        assertExec("Trying to get -1", "test", Double.valueOf(-1),
            StaticMethod.class.getName(), "minusOne__I"
        );
    }

    @Test public void loadDependantClass() throws Exception {
        assertExec("Expecting zero", "test", Double.valueOf(0),
            InstanceSub.class.getName(), "recallDbl__D"
        );
    }

    private static void assertExec(String msg, String methodName, Object expRes, Object... args) throws Exception {
        Object ret = null;
        try {
            ret = code.invokeFunction(methodName, args);
        } catch (ScriptException ex) {
            fail("Execution failed in\n" + StaticMethodTest.dumpJS(codeSeq), ex);
        } catch (NoSuchMethodException ex) {
            fail("Cannot find method in\n" + StaticMethodTest.dumpJS(codeSeq), ex);
        }
        if (ret == null && expRes == null) {
            return;
        }
        if (expRes.equals(ret)) {
            return;
        }
        assertEquals(ret, expRes, msg + "was: " + ret + "\n" + StaticMethodTest.dumpJS(codeSeq));
    }
}
