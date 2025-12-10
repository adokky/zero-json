package dev.dokky.zerojson

import kotlinx.serialization.Serializable
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
    val description: String?,
    val department: Department,
    val participation: Map<DepartmentId, RoleId>,
    val friends: List<EmployeeId>,
    val extra: Map<String, String> = emptyMap()
): Person

@Serializable
data class Guest(override val name: String, val roleId: RoleId) : Person

@Serializable
data class Department(val name: String, val address: String)

@Serializable
data class Response<T>(
    val data: List<T>,
    val total: Int,
    val version: Int
)

private fun randomEmployee() = Employee(
    EmployeeId(Random.nextInt(100000)),
    name = "First 姓 Фамилия ${Random.nextInt()}",
    description = if (Random.nextBoolean()) null else "Длинное описание. 很多線. Long description",
    department = Department("Dep Name ${Random.nextInt()}", "Address 45, 67"),
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
    extra = mapOf("extra" to "value")
)

private fun randomGuest() = Guest("Guest ${Random.nextInt()}", RoleId(Random.nextInt(1000)))

fun randomPerson(): Person = if (Random.nextInt(8) == 0) randomGuest() else randomEmployee()

