/**
 * Back 2 Browser Bytecode Translator
 * Copyright (C) 2012-2017 Jaroslav Tulach <jaroslav.tulach@apidesign.org>
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
package org.apidesign.truffle;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.BeforeClass;
import org.junit.Test;

public class Bck2BrwsrLanguageTest {

    private static PolyglotEngine engine;
    private static ByteArrayOutputStream out;

    public Bck2BrwsrLanguageTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        out = new ByteArrayOutputStream();
        engine = PolyglotEngine.newBuilder().setErr(out).setOut(out).build();
    }

    @AfterClass
    public static void tearDownClass() {
        engine.dispose();
    }

    @Test
    public void testHelloWorld() throws Exception {
        File jar = createHelloJar();

        Source src = Source.newBuilder(jar.toURI().toURL()).mimeType("application/x-jar").build();
        engine.eval(src);

        PolyglotEngine.Value nonExistingClass;
        try {
            nonExistingClass = engine.findGlobalSymbol(Hello.class.getCanonicalName() + '2');
        } catch (Exception ex) {
            nonExistingClass = null;
        }
        assertNull(nonExistingClass);

        PolyglotEngine.Value in = engine.findGlobalSymbol(Hello.class.getCanonicalName());
        assertNotNull(in);
        Invoke invoke = in.as(Invoke.class);

        out.reset();
        invoke.invoke("sayHello");
        assertEquals("Hello from Java!", out.toString("UTF-8").trim());
    }

    private File createHelloJar() throws IOException {
        URL u = Bck2BrwsrLanguageTest.class.getResource("Hello.class");
        assertNotNull("Hello.class found", u);

        File jar = File.createTempFile("hello", ".jar");
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().putValue("Bundle-SymbolicName", "test");
        mf.getMainAttributes().putValue("Export-Package", Hello.class.getPackage().getName());
        jar.deleteOnExit();
        JarOutputStream os = new JarOutputStream(new FileOutputStream(jar), mf);
        os.putNextEntry(new JarEntry(Hello.class.getCanonicalName().replace('.', '/') + ".class"));
        InputStream is = u.openStream();
        byte[] arr = new byte[4096];
        for (;;) {
            int len = is.read(arr);
            if (len == -1) {
                break;
            }
            os.write(arr, 0, len);
        }
        is.close();
        os.closeEntry();
        os.close();
        return jar;
    }

    private static interface Invoke {
        void invoke() throws Exception;
        void invoke(String name, String... args) throws Exception;
    }
}
