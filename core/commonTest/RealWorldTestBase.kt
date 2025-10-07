package dev.dokky.zerojson

import dev.dokky.zerojson.framework.DslJsonObjectBuilder
import dev.dokky.zerojson.framework.RandomizedJsonTest
import dev.dokky.zerojson.framework.jsonObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlin.jvm.JvmInline
import kotlin.random.Random

abstract class RealWorldTestBase: RandomizedJsonTest() {
    @Serializable
    @JvmInline
    protected value class EmployeeId(val asInt: Int)

    @Serializable
    @JvmInline
    protected value class RoleId(val asInt: Int)

    @Serializable
    @JvmInline
    protected value class DepartmentId(val asInt: Int)

    @Serializable
    protected sealed interface Person {
        val name: String
    }

    @Serializable
    protected data class Employee(
        val id: EmployeeId,
        override val name: String,
        val description: String?,
        val department: Department,
        val participation: Map<DepartmentId, RoleId>,
        val friends: List<EmployeeId>,
        @JsonInline val extra: Map<String, String> = emptyMap()
    ): Person

    @Serializable
    protected data class Guest(override val name: String, val roleId: RoleId) : Person

    @Serializable
    protected data class Department(val name: String, val address: String)

    @Serializable
    protected data class Response<T>(
        val data: List<T>,
        val total: Int,
        val version: Int
    )

    protected fun randomEmployee() = Employee(
        EmployeeId(Random.nextInt(100000)),
        name = "FirstName SecondName ThirdName ${Random.nextInt()}",
        description = if (Random.nextBoolean()) null else "Detailed description of employee",
        department = Department("Dep Name ${Random.nextInt()}", "Some District 45, 67"),
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

    protected fun randomGuest() = Guest("Guest ${Random.nextInt()}", RoleId(Random.nextInt(1000)))

    protected fun randomPerson(): Person = if (Random.nextInt(8) == 0) randomGuest() else randomEmployee()

    protected fun Person.toJsonObject(): JsonObject = jsonObject(allowRandomKeys = false) {
        "name" eq name

        when (this@toJsonObject) {
            is Employee -> fillJsonObject(this@toJsonObject)
            is Guest -> {
                discriminator<Guest>()
                "roleId" eq this@toJsonObject.roleId.asInt
            }
        }
    }

    protected fun DslJsonObjectBuilder.fillJsonObject(item: Employee) {
        discriminator<Employee>()
        "id" eq item.id.asInt
        "description" eq item.description
        "department" {
            "name" eq item.department.name
            "address" eq item.department.address
        }
        "participation" noRandomKeys {
            for ((k, v) in item.participation) {
                k.asInt.toString() eq v.asInt
            }
        }
        "friends" array {
            for (f in item.friends) {
                add(f.asInt)
            }
        }
        "extra" eq "value"
    }
}