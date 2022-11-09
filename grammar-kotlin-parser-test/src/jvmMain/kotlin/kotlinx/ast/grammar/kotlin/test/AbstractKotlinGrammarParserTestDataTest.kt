package kotlinx.ast.grammar.kotlin.test

import io.kotest.assertions.fail
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.ast.common.AstSource
import kotlinx.ast.common.ast.Ast
import kotlinx.ast.common.ast.AstNode
import kotlinx.ast.common.ast.AstWithAstInfo
import kotlinx.ast.common.printString
import kotlinx.ast.grammar.kotlin.common.KotlinGrammarParser
import kotlinx.ast.grammar.kotlin.common.summary
import kotlinx.ast.test.OverwriteTestData
import java.io.File

abstract class AbstractKotlinGrammarParserTestDataTest<Parser : KotlinGrammarParser<*, *>>(parser: Parser) : FunSpec({
    val tests = testData()

    if (tests.isEmpty()) {
        test("no test data found!") {
            fail("no test data found!")
        }
    }

    tests.forEach() { testData ->
        testData.apply {
            context(name) {
                val ast by lazy { parser.parseKotlinFile(AstSource.String(name, kotlinContent)) }

                suspend fun test(
                    testCase: String,
                    expected: String?,
                    expectedFile: File,
                    execute: () -> String
                ) {
                    test(testCase) {
                        val actual = execute()
                        if (expected == null) {
                            expectedFile.writeText(actual)
                            fail("expected data for test \"$name -- $testCase\" not found, data created, please restart the test!")
                        } else {
                            if (OverwriteTestData() && actual != expected) {
                                expectedFile.writeText(actual)
                                fail("expected data for test \"$name -- $testCase\" differs, data updated!")
                            } else {
                                actual shouldBe expected
                            }
                        }
                    }
                }

                test("raw ast", rawAstContent, rawAstFile) {
                    ast.printString()
                }

                test("summary ast", summaryContent, summaryFile) {
                    ast.summary(attachRawAst = false).map {
                        it.joinToString("", transform = Ast::printString)
                    }.get()
                }

                fun Ast.astInfo(indent: Int): List<String> {
                    val info = ((this as? AstWithAstInfo)?.info?.toString() ?: "").padEnd(34)
                    val self = "$info${"  ".repeat(indent)}$description"
                    return if (this is AstNode) {
                        listOf(self) + children.flatMap { child ->
                            child.astInfo(indent + 1)
                        }
                    } else {
                        listOf(self)
                    }
                }

                fun Ast.astInfo(): String {
                    return astInfo(indent = 0).joinToString("\n", postfix = "\n")
                }

                val infoHeader = "   ID Index        Position       Token\n"

                test("raw info", rawInfoContent, rawInfoFile) {
                    infoHeader + ast.astInfo()
                }

                test("summary info", summaryInfoContent, summaryInfoFile) {
                    infoHeader + ast.summary(attachRawAst = false).map {
                        it.joinToString("", transform = Ast::astInfo)
                    }.get()
                }
            }
        }
    }
})
