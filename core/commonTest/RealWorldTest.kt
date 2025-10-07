package dev.dokky.zerojson

import dev.dokky.zerojson.framework.domainObject
import dev.dokky.zerojson.framework.jsonObject
import dev.dokky.zerojson.framework.randomizedTest
import kotlin.test.Test

class RealWorldTest: RealWorldTestBase() {
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
}