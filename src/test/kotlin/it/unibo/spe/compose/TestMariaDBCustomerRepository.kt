package it.unibo.spe.compose

import it.unibo.spe.compose.db.MariaDB
import it.unibo.spe.compose.db.MariaDBConnectionFactory
import it.unibo.spe.compose.impl.SqlCustomerRepository
import java.io.File
import java.lang.IllegalArgumentException
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass

class TestMariaDBCustomerRepository {

    companion object {
        const val HOST = MariaDB.DEFAULT_HOST
        const val PORT = MariaDB.DEFAULT_PORT
        const val DATABASE = "db"
        const val USER = "user"
        const val PASSWORD = "password"
        const val TABLE = "customers"

        private lateinit var composeFile: File

        // @Suppress("UNUSED_PARAMETER")
        // private fun findSubstitution(
        //         text: List<String>,
        //         vararg pairssss: Array<out Pair<String, String>>
        // ): List<String> {
        //     return text.map { line ->
        //             for (p in pairssss) {
        //                 if (line.contains("__" + p.first + "__")) {
        //                     line.replace("__" + p.first + "__", p.second)
        //                 } else {
        //                     line
        //                 }
        //             }
        //     }
        // }

        private fun findSubstitution(
                text: List<String>,
                vararg pairssss: Array<out Pair<String, String>>
        ): List<String> {
            return text.map { line ->
                var modifiedLine = line
                pairssss.forEach { pairs ->
                    pairs.forEach { (first, second) ->
                        if (modifiedLine.contains("__${first}__")) {
                            modifiedLine = modifiedLine.replace("__${first}__", second)
                        }
                    }
                }
                modifiedLine
            }
        }

        @Suppress("UNUSED_PARAMETER")
        private fun createComposeFile(path: File, vararg assignments: Pair<String, String>): File {
            val file = File(object {}.javaClass.getResource("docker-compose.yml.template").file)
            val bufferedReader = file.bufferedReader()
            val text: List<String> = bufferedReader.readLines()
            val substitutedLines = this.findSubstitution(text, assignments)
            val docker = File("docker-compose.yml")
            docker.createNewFile()
            docker.writeText(substitutedLines.joinToString("\n"))
            return docker
        }

        /**
         * Calls `docker compose -f $composeFile $arguments and waits for its completion If async ==
         * false, waits for the command to terminate and asserts that the exit value is 0
         */
        private fun executeDockerCompose(vararg arguments: String): Process {
            val command: MutableList<String> =
                    mutableListOf<String>() // TODO("call docker compose ensuring it will use
            // $composeFile")
            command.addAll(arguments)
            return ProcessBuilder(command).inheritIO().start().also {
                // TODO("wait for the process to terminate")
                // TODO("assert that the exit value is 0 (i.e. make the test fail if the docker
                // compose command fails")
            }
        }

        @BeforeClass
        @JvmStatic
        fun setUpMariaDb() {
            composeFile =
                    createComposeFile(
                            path = File.createTempFile("docker-compose", ".yml"),
                            "VERSION" to "latest",
                            "ROOT_PASSWORD" to UUID.randomUUID().toString(),
                            "DB_NAME" to DATABASE,
                            "USER" to USER,
                            "PASSWORD" to PASSWORD,
                            "PORT" to "$PORT",
                    )

            TODO("start the stack in $composeFile")
        }

        @AfterClass
        @JvmStatic
        fun tearDownMariaDb() {
            TODO("stop the stack in $composeFile")
        }
    }

    private val connectionFactory = MariaDBConnectionFactory(DATABASE, USER, PASSWORD, HOST, PORT)
    private lateinit var repository: SqlCustomerRepository

    @Before
    fun createFreshTable() {
        repository = SqlCustomerRepository(connectionFactory, TABLE)
        repository.createTable(replaceIfPresent = true)
    }

    private val taxCode = TaxCode("CTTGNN92D07D468M")
    private val person = Customer.person(taxCode, "Giovanni", "Ciatto", LocalDate.of(1992, 4, 7))
    private val person2 =
            person.clone(birthDate = LocalDate.of(1992, 4, 8), id = TaxCode("CTTGNN92D08D468M"))
    private val vatNumber = VatNumber(12345678987L)
    private val company = Customer.company(vatNumber, "ACME", "Inc.", LocalDate.of(1920, 1, 1))

    @Test
    fun complexTestWhichShouldActuallyBeDecomposedInSmallerTests() {
        assertEquals(
                expected = emptyList(),
                actual = repository.findById(taxCode),
        )
        assertEquals(
                expected = emptyList(),
                actual = repository.findById(vatNumber),
        )
        repository.add(person)
        repository.add(company)
        assertEquals(
                expected = listOf(person),
                actual = repository.findById(taxCode),
        )
        assertEquals(
                expected = listOf(company),
                actual = repository.findById(vatNumber),
        )
        repository.remove(vatNumber)
        repository.update(taxCode, person2)
        assertFailsWith<IllegalArgumentException>("Invalid customer id:") {
            repository.remove(taxCode)
        }
        assertEquals(
                expected = emptyList(),
                actual = repository.findById(taxCode),
        )
        assertEquals(
                expected = emptyList(),
                actual = repository.findById(vatNumber),
        )
        assertEquals(
                expected = listOf(person2),
                actual = repository.findById(person2.id),
        )
        listOf("Giovanni", "Ciatto").forEach {
            assertEquals(
                    expected = listOf(person2),
                    actual = repository.findByName(it),
            )
        }
    }
}
