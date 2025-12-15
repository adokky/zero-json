package dev.dokky.zerojson

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import kotlin.random.Random

@Serializable
@JvmInline
value class EmployeeId(val asInt: Int)

@Serializable
@JvmInline
value class RoleId(val asInt: Int)

@Serializable
@JvmInline
value class DepartmentId(val asInt: Int)

@Serializable
sealed interface Person {
    val name: String
}

@Serializable
data class Employee(
    val id: EmployeeId,
    override val name: String,
    val birthDate: String,
    val description: String?,
    val department: Department,
    val participation: Map<DepartmentId, RoleId>,
    val friends: List<EmployeeId>,
    val income: Int,
    val locations: List<Location>,
    val extra: Map<String, String> = emptyMap()
): Person

@Serializable
data class Location(val address: String, val latitude: Double, val longitude: Double)

@Serializable
data class Guest(
    override val name: String,
    val roleId: RoleId,
    val enum: SomeEnum,
    val birthDate: String,
) : Person

@Serializable
data class Department(
    val name: String,
    val address: String,
    val description: String? = null
)

@Serializable
data class Response<T>(
    val data: List<T>,
    val total: Int,
    val version: Int
)

enum class SomeEnum {
    entry1,
    another_entry2,
    UPPERCASE_ENTRY,
}

private fun randomEmployee() = Employee(
    EmployeeId(Random.nextInt(100000)),
    name = "First 姓 Фамилия ${Random.nextInt()}",
    description = when {
        Random.nextInt(3) == 0 -> null
        Random.nextInt(7) == 0 -> "The guanaco (Lama guanicoe) is a species of mammal in the family Camelidae, the camelids. Closely related to the llama, the guanaco is native to the steppes, scrublands and mountainous regions of South America, including Peru, Bolivia, Chile, Paraguay and Argentina. It is a diurnal animal, living in small herds consisting of either one male and several females with their young, or separate bachelor herds. It can run at speeds of up to 64 km/h (40 mph), important for avoiding predation. A herbivore, the guanaco grazes on grasses, shrubs, herbs, lichens, fungi, cacti, and flowers, while its natural predators include the puma and the culpeo (Andean fox). Some guanacos are found domesticated in zoos and private herds around the world, and its fiber is also harvested for use in luxury fabrics, being noted for its soft, warm feel."
        else -> "Длинное описание. 很多線. Long description. "
    },
    department = Department(
        "Dep Name ${Random.nextInt()}",
        "Address 45, 67",
        description = """Kotlin is a cross-platform, statically typed, general-purpose high-level programming language with type inference. Kotlin is designed to interoperate fully with Java, and the JVM version of Kotlin's standard library depends on the Java Class Library, but type inference allows its syntax to be more concise. Kotlin mainly targets the JVM, but also compiles to JavaScript (e.g. for frontend web applications using React) or native code via LLVM (e.g. for native iOS apps sharing business logic with Android apps)"""
            .takeIf { Random.nextInt(10) == 0 }

    ),
    participation = buildMap {
        repeat(Random.nextInt(3)) {
            put(DepartmentId(Random.nextInt(1000)), RoleId(Random.nextInt(1000)))
        }
    },
    friends = buildList {
        repeat(Random.nextInt(10)) {
            add(EmployeeId(Random.nextInt(10000)))
        }
    },
    extra = buildMap {
        repeat(Random.nextInt(10)) { i->
            put("id$i", "Some Value ${i*i*i} /dfp39czx828dda;l,v,358qc,=caserdxVOI*TJ(9HXc".repeat(Random.nextInt(2)))
        }
    },
    locations = (0..Random.nextInt(2)).map { Location("Some Address, 1 2", Random.nextDouble(300.0), Random.nextDouble(300.0)) },
    income = Random.nextInt(),
    birthDate = LocalDateTime.now().toString()
)

private fun randomGuest() = Guest(
    name = "Guest ${Random.nextInt()}",
    roleId = RoleId(Random.nextInt(1000)),
    enum = SomeEnum.entries.random(),
    birthDate = LocalDateTime.now().toString()
)

fun randomPerson(): Person = if (Random.nextInt(8) == 0) randomGuest() else randomEmployee()