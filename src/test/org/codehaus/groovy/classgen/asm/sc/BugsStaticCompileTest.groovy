/*
 * Copyright 2003-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.classgen.asm.sc

import groovy.transform.stc.BugsSTCTest

/**
 * Unit tests for static type checking : bugs.
 *
 * @author Cedric Champeau
 */
@Mixin(StaticCompilationTestSupport)
class BugsStaticCompileTest extends BugsSTCTest {

    @Override
    protected void setUp() {
        super.setUp()
        extraSetup()
    }

    void testGroovy5498PropertyAccess() {
        assertScript '''
            class Test {

              List getListVar() {
                new ArrayList()
              }

              void someMethod() {
                 def t = new Object()
                 t = this

                 t.getListVar()     //No error here
                 t.listVar          //error is being reported here
                 assert t.listVar == t.getListVar()
              }
            }
            new Test().someMethod()
        '''
    }

    // GROOVY-5512
    void testCreateRangeInInnerClass() {
        def shell = new GroovyShell()
        shell.evaluate '''
            class Outer {
                static class Inner {
                    @groovy.transform.CompileStatic
                    int m() {
                        int x = 0
                        for (int i in 1..10) {x++}
                        x
                    }
                }
            }
            assert new Outer.Inner().m() == 10
        '''
    }

    // GROOVY-5526
    void testAssertEqualsShouldNotThrowVerifyError() {
        assertScript '''
            import static org.junit.Assert.*;
            import groovy.transform.CompileStatic;

            class CompilerBugs {

              public static void main(String[] args) {
                int expected = 0
                assertEquals(expected, args.length)
              }

            }
        '''
    }

    // GROOVY-5529
    void testStaticCompilationOfClosureWhenSingleMethodAnnotated() {
        new GroovyShell().evaluate '''import groovy.transform.ASTTest
        import static org.codehaus.groovy.control.CompilePhase.*

        interface Row {
            int getKey()
        }

        class RowImpl implements Row {
            int getKey() { 1 }
        }

        @groovy.transform.CompileStatic
        def test() {
            def rows = [new RowImpl(), new RowImpl(), new RowImpl()]

            rows.each { Row row ->
                println row.key
            }
        }

        test()
        '''
    }

    // GROOVY-5536
    void testShouldNotThrowVerifyErrorWithNullDereferenceInIf() {
        assertScript '''
                boolean getDescriptorForPlugin(File pluginDir) {
                    if (pluginDir?.exists()) { true } else { false }
                }
                assert getDescriptorForPlugin(null) == false
        '''
    }

    // GROOVY-
    void testPowerShouldNotThrowVerifyError() {
        assertScript '''int squarePlusOne(int num) {
                num ** num + 1
            }
            assert squarePlusOne(2) == 5
        '''
    }

    // GROOVY-5570
    void testShouldNotThrowVerifyErrorRegisterContainsWrongType() {
        assertScript '''
                void foo() {
                boolean idx = false
                def cl = { idx }
                }
            '''
        assertScript '''
                void foo() {
                int idx = 0
                def cl = { idx }
                }
            '''
    }

    // GROOVY-5572
    void testTernaryOperatorWithNull() {
        assertScript '''
            assert (true ? null : true) == null
        '''
    }

    // GROOVY-5564
    void testSkipStaticCompile() {
        new GroovyShell().evaluate '''import groovy.transform.CompileStatic
            import static groovy.transform.TypeCheckingMode.SKIP

            @CompileStatic
            class A {
                @CompileStatic(SKIP)
                String toString(Object o) { o }
            }

            def a = new A()
            assert a.toString('foo')=='foo'
            assert a.toString(1) == '1'
            '''
    }

    // GROOVY-5586
    void testCanonicalInInnerClass() {
        new GroovyShell().evaluate '''import groovy.transform.*
            @CompileStatic
            class CanonicalStaticTest extends GroovyTestCase {
              @Canonical class Thing {
                String stuff
              }

              Thing testCanonical() {
                new Thing()
              }
            }
            assert new CanonicalStaticTest().testCanonical().toString() == 'CanonicalStaticTest$Thing(null)'
        '''
    }

    // GROOVY-5607
    // duplicate of GROOVY-5573, see ArraysAndCollectionsSTCTest#testArrayNewInstance()
    void testStaticNewInstanceMethodClash() {
        assertScript '''
            class Sql {
                static Sql newInstance(String s1, String s2, String s3, String s4) {
                    new Sql()
                }
            }

            @groovy.transform.CompileStatic
            class Main {
                void test() {
                    @ASTTest(phase=INSTRUCTION_SELECTION, value= {
                        assert node.rightExpression.getNodeMetaData(INFERRED_TYPE).nameWithoutPackage == 'Sql'
                    })
                    def sql = Sql.newInstance("a", "b", "c", "d")
                }
            }

            new Main().test()
        '''
    }

    void testCompileStaticTwiceShouldNotBeAProblem() {
        new GroovyShell().evaluate '''import groovy.transform.CompileStatic
        @CompileStatic
        class Tool {
            @CompileStatic // annotated too, even if class is already annotated
            String relativePath(File relbase, File file) {
                def pathParts = []
                def currentFile = file
                while (currentFile != null && currentFile != relbase) {
                    pathParts += currentFile.name
                    currentFile = currentFile.parentFile
                }
                pathParts.reverse().join('/')
            }
        }
        File a = new File('foo')
        File b = new File(new File(a, 'bar'), 'baz')
        assert new Tool().relativePath(a,b) == 'bar/baz'
        '''
    }

    void testCompileStaticTwiceShouldNotBeAProblemUsingCustomizer() {
        assertScript '''import groovy.transform.CompileStatic
        @CompileStatic
        class Tool {
            @CompileStatic // annotated too, even if class is already annotated
            String relativePath(File relbase, File file) {
                def pathParts = []
                def currentFile = file
                while (currentFile != null && currentFile != relbase) {
                    pathParts += currentFile.name
                    currentFile = currentFile.parentFile
                }
                pathParts.reverse().join('/')
            }
        }
        File a = new File('foo')
        File b = new File(new File(a, 'bar'), 'baz')
        assert new Tool().relativePath(a,b) == 'bar/baz'
        '''
    }

    // GROOVY-5613
    void testNullSafeAssignment() {
        assertScript '''
        class A {
            int x = -1
        }
        A a = new A()
        @ASTTest(phase=INSTRUCTION_SELECTION, value={
            assert node.getNodeMetaData(INFERRED_TYPE) == int_TYPE
        })
        def x = a?.x
        '''
    }
    void testNullSafeAssignmentWithLong() {
        assertScript '''
        class A {
            long x = -1
        }
        A a = new A()
        @ASTTest(phase=INSTRUCTION_SELECTION, value={
            assert node.getNodeMetaData(INFERRED_TYPE) == long_TYPE
        })
        def x = a?.x
        '''
    }
    void testNullSafeAssignmentWithChar() {
        assertScript '''
        class A {
            char x = 'a'
        }
        A a = new A()
        @ASTTest(phase=INSTRUCTION_SELECTION, value={
            assert node.getNodeMetaData(INFERRED_TYPE) == char_TYPE
        })
        def x = a?.x
        assert x == 'a'
        '''
    }
    void testCallStaticallyImportedMethodWithNullSafeArgument() {
        assertScript '''import static java.lang.Math.abs
        class A {
            int x = -1
        }
        def a = new A()
        def x = a?.x
        assert abs(a?.x) == 1
        '''
    }

    void testClosureAsInterfaceArgument() {
        assertScript '''
                Closure c = { Integer x, Integer y -> x <=> y }
                def list = [ 3,1,5,2,4 ]
                assert list.sort(c) == [1,2,3,4,5]
            '''
    }

    void testInferredTypeForInteger() {
        assertScript '''
            @ASTTest(phase=INSTRUCTION_SELECTION, value={
                assert node.getNodeMetaData(INFERRED_TYPE) == Integer_TYPE
            })
            Integer x = 1
        '''
    }

    // GROOVY-5671
    void testPostfixIncPrimitiveInteger() {
        assertScript '''
            int x = 0
            x++
            x++
            assert x == 2
            assert x++ == 2
            assert x == 3
        '''
    }

    void testPostfixIncInteger() {
        assertScript '''
                Integer x = 0
                x++
                x++
                assert x == 2
                assert x++ == 2
                assert x == 3
            '''
    }

    void testPostfixDecInt() {
        assertScript '''
                int x = 0
                x--
                x--
                assert x == -2
                assert x-- == -2
                assert x == -3
            '''
    }

    void testPostfixDecInteger() {
        assertScript '''
                Integer x = 0
                x--
                x--
                assert x == -2
                assert x-- == -2
                assert x == -3
            '''
    }

    void testPrefixIncPrimitiveInteger() {
        assertScript '''
            int x = 0
            ++x
            ++x
            assert x == 2
            assert ++x == 3
            assert x == 3
        '''
    }

    void testPrefixIncInteger() {
        assertScript '''
                Integer x = 0
                ++x
                ++x
                assert x == 2
                assert ++x == 3
                assert x == 3
            '''
    }

    void testPrefixDecInt() {
        assertScript '''
                int x = 0
                --x
                --x
                assert --x == -3
                assert x == -3
            '''
    }

    void testPrefixDecInteger() {
        assertScript '''
                Integer x = 0
                --x
                --x
                assert --x == -3
                assert x == -3
            '''
    }

    void testShouldSkipSpreadOperator() {
        new GroovyShell().evaluate '''import groovy.transform.TypeCheckingMode
            import groovy.transform.CompileStatic

            @CompileStatic // top level must be @CS
            class Foo {
                @CompileStatic(TypeCheckingMode.SKIP)
                static void foo(fun, args) {
                    new Runnable() { // create an anonymous class which should *not* be visited
                        void run() {
                            fun(*args) // spread operator is disallowed with STC/SC, but SKIP should prevent from an error
                        }
                    }
                }
            }
            new Foo()
        '''
    }

    // GROOVY-5672
    void testTypeCheckedPlusCompileStatic() {
        new GroovyShell().evaluate '''import groovy.transform.CompileStatic
        import groovy.transform.TypeChecked

        @TypeChecked
        @CompileStatic
        class SampleClass {
            def a = "some string"
            def b = a.toString()
        }
        new SampleClass()
        '''
    }

    void testSubclassShouldNotThrowArrayIndexOutOfBoundsException() {
        assertScript '''
            // The error only shows up if the subclass is compiled *before* the superclass
            class Subclass extends Z {
                public Subclass(double x, double y, double z) {
                    super(x,y,z)
                }
            }
            class Z {
               double x, y, z

                public Z(double x, double y, double z) { this.x = x; this.y = y; this.z = z }

                public Z negative() { return new Z(-x, -y, -z) }
            }
            new Subclass(0,0,0)
        '''
    }
}

