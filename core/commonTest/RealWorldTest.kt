package dev.dokky.zerojson

import dev.dokky.zerojson.framework.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlin.jvm.JvmInline
import kotlin.random.Random
import kotlin.test.Test

class RealWorldTest: RandomizedJsonTest() {
    @Serializable
    @JvmInline
    private value class EmployeeId(val asInt: Int)

    @Serializable
    @JvmInline
    private value class RoleId(val asInt: Int)

    @Serializable
    @JvmInline
    private value class DepartmentId(val asInt: Int)

    @Serializable
    private sealed interface Person {
        val name: String
    }

    @Serializable
    private data class Employee(
        val id: EmployeeId,
        override val name: String,
        val description: String?,
        val department: Department,
        val participation: Map<DepartmentId, RoleId>,
        val friends: List<EmployeeId>,
        @JsonInline val extra: Map<String, String> = emptyMap()
    ): Person

    @Serializable
    private data class Guest(override val name: String, val roleId: RoleId) : Person

    @Serializable
    private data class Department(val name: String, val address: String)

    @Serializable
    private data class Response<T>(
        val data: List<T>,
        val total: Int,
        val version: Int
    )

    private fun randomEmployee() = Employee(
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

    private fun randomGuest() = Guest("Guest ${Random.nextInt()}", RoleId(Random.nextInt(1000)))

    private fun randomPerson(): Person = if (Random.nextInt(8) == 0) randomGuest() else randomEmployee()

    @Test
    fun simple() {
        encodeDecode(
            value = Response(
                data = (1..100).map { randomPerson() },
                total = 2094,
                version = 35353
            ),
            compareWithKtx = false
        )
    }

    @Test
    fun randomized() {
        val data = Response(
            data = (1..5).map { randomPerson() },
            total = 2094,
            version = 35353
        )

        randomizedTest {
            domainObject(data)
            jsonElement = jsonObject {
                "data" array {
                    for (item in data.data) {
                        add(item.toJsonObject())
                    }
                }
                "total" eq data.total
                "version" eq data.version
            }
        }
    }

    @Test
    fun fixture() {
        assertDecodedEquals<Person>(
            """
            {
                "extra": "value",
                "id": 68271,
                "friends":
                [
                    5579,
                    236,
                    8660,
                    4709,
                    7903,
                    3700,
                    6900,
                    3444
                ],
                "department":
                {
                    "address": "Some District 45, 67",
                    "name": "Dep Name 1558854354"
                },
                "participation":
                {
                    "742": 773,
                    "553": 876
                },
                "description": null,
                "type": "dev.dokky.zerojson.RealWorldTest.Employee",
                "name": "FirstName SecondName ThirdName -53240547"
            }
            """.trimIndent(),
            Employee(
                EmployeeId(68271),
                name = "FirstName SecondName ThirdName -53240547",
                description = null,
                participation = mapOf(
                    DepartmentId(742) to RoleId(773),
                    DepartmentId(553) to RoleId(876),
                ),
                department = Department(name = "Dep Name 1558854354", address = "Some District 45, 67"),
                friends = listOf(5579, 236, 8660, 4709, 7903, 3700, 6900, 3444).map { EmployeeId(it) },
                extra = mapOf("extra" to "value")
            )
        )
    }

    private fun Person.toJsonObject(): JsonObject = jsonObject(allowRandomKeys = false) {
        "name" eq name

        when (this@toJsonObject) {
            is Employee -> fillJsonObject(this@toJsonObject)
            is Guest -> {
                discriminator<Guest>()
                "roleId" eq this@toJsonObject.roleId.asInt
            }
        }
    }

    private fun DslJsonObjectBuilder.fillJsonObject(item: Employee) {
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