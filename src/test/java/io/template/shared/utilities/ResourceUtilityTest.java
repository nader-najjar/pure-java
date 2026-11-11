package io.template.shared.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceUtilityTest {

    @Test
    void readsClasspathResourceAndPreservesWhitespace() {
        assertEquals(
                "First line\n\n  Second line\n",
                ResourceUtility.readString("/resource-utility/plain.txt")
        );
    }

    @Test
    void readsUtf8Resource() {
        assertEquals(
                "café, مرحبا, 日本語, 🌍\n",
                ResourceUtility.readString("/resource-utility/unicode.txt")
        );
    }

    @Test
    void readsPackageRelativeResource() {
        assertEquals("Relative resource\n", ResourceUtility.readString("relative.txt"));
    }

    @Test
    void readsEmptyResource() {
        assertEquals("", ResourceUtility.readString("/resource-utility/empty.txt"));
    }

    @Test
    void reportsMissingResourcePath() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ResourceUtility.readString("/resource-utility/missing.txt")
        );

        assertEquals("Missing resource: /resource-utility/missing.txt", exception.getMessage());
    }

    @Test
    void wrapsReadFailureAndClosesStream() throws IOException, ReflectiveOperationException {
        InputStream content = mock(InputStream.class);
        IOException failure = new IOException("Read failed");
        when(content.readAllBytes()).thenThrow(failure);

        UncheckedIOException exception = readFailure(content);

        assertEquals("Could not read resource: /broken.txt", exception.getMessage());
        assertSame(failure, exception.getCause());
        verify(content).close();
    }

    @Test
    void wrapsCloseFailure() throws IOException, ReflectiveOperationException {
        InputStream content = mock(InputStream.class);
        IOException failure = new IOException("Close failed");
        when(content.readAllBytes()).thenReturn(new byte[0]);
        doThrow(failure).when(content).close();

        UncheckedIOException exception = readFailure(content);

        assertEquals("Could not read resource: /broken.txt", exception.getMessage());
        assertSame(failure, exception.getCause());
    }

    @Test
    void preservesReadFailureWhenClosingAlsoFails() throws IOException, ReflectiveOperationException {
        InputStream content = mock(InputStream.class);
        IOException readFailure = new IOException("Read failed");
        IOException closeFailure = new IOException("Close failed");
        when(content.readAllBytes()).thenThrow(readFailure);
        doThrow(closeFailure).when(content).close();

        UncheckedIOException exception = readFailure(content);

        assertSame(readFailure, exception.getCause());
        assertEquals(1, readFailure.getSuppressed().length);
        assertSame(closeFailure, readFailure.getSuppressed()[0]);
    }

    private static UncheckedIOException readFailure(InputStream content)
            throws IOException, ReflectiveOperationException {
        URL location = ResourceUtility.class.getProtectionDomain().getCodeSource().getLocation();
        // Load the real utility with a controlled resource stream to exercise I/O failures.
        try (URLClassLoader loader = new URLClassLoader(
                new URL[] {location},
                ClassLoader.getPlatformClassLoader()
        ) {
            @Override
            public InputStream getResourceAsStream(String name) {
                assertEquals("broken.txt", name);
                return content;
            }
        }) {
            Class<?> utility = loader.loadClass(ResourceUtility.class.getName());
            Method readString = utility.getMethod("readString", String.class);
            InvocationTargetException exception = assertThrows(
                    InvocationTargetException.class,
                    () -> readString.invoke(null, "/broken.txt")
            );

            return assertInstanceOf(UncheckedIOException.class, exception.getCause());
        }
    }
}
