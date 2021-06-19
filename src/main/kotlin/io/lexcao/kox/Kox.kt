package io.lexcao.kox

import io.lexcao.kox.common.Token
import io.lexcao.kox.component.Interpreter
import io.lexcao.kox.component.Parser
import io.lexcao.kox.component.Resolver
import io.lexcao.kox.component.Scanner
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

object Kox {
    private var hadError = false
    private var hadRuntimeError = false

    fun run(args: Array<String>) {
        when {
            args.size > 1 -> {
                println("Usage: kox [script]")
                exitProcess(64)
            }
            args.size == 1 -> {
                runFile(args[0])
            }
            else -> {
                runPrompt()
            }
        }
    }

    // REPL
    private fun runPrompt() {
        while (true) {
            print("> ")
            val line = readLine() ?: break
            if (line.isBlank()) continue
            run(line)
            hadError = false
            hadRuntimeError = false
        }
    }

    // Run file
    private fun runFile(fileName: String) {
        val bytes = Files.readAllBytes(Paths.get(fileName))
        run(String(bytes))

        if (hadError) exitProcess(65)
        if (hadRuntimeError) exitProcess(70)
    }

    fun run(source: String) {
        val scanner = Scanner(source)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens)
        val statements = parser.parse()

        if (hadError) return

        val interpreter = Interpreter()
        val resolver = Resolver(interpreter)
        resolver.resolve(statements)

        if (hadError) return

        interpreter.interpret(statements)
    }

    fun error(token: Token, message: String) {
        error(token.line, token.lexeme, message)
    }

    fun error(line: Int, message: String, where: String = "") {
        System.err.println("[line: $line] Error $where: $message")
        hadError = true
    }

    fun runtimeError(e: Interpreter.RuntimeError) {
        System.err.println(e.message + " [line ${e.token.line}]")
        hadRuntimeError = true
    }

    fun clearErrors() {
        hadError = false
        hadRuntimeError = false
    }
}
