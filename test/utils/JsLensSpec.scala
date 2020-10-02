/*
 * Copyright 2020 HM Revenue & Customs
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

package utils

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.MustMatchers
import org.scalatest.OptionValues
import org.scalatest.WordSpec
import play.api.libs.json._

class JsLensSpec extends WordSpec with MustMatchers with ScalaCheckPropertyChecks with OptionValues {

  val jsLeafGen: Gen[JsValue] = {
    Gen.frequency(
      10 -> Gen.alphaNumStr.map(JsString),
      10 -> Gen.chooseNum(1, 9999).map(JsNumber(_)),
      3 -> Gen.oneOf(true, false).map(JsBoolean)
    )
  }

  "#atKey (get)" must {
    "return a lens" which {

      "gets an inner value at `key` when it is available" in {

        forAll(Gen.alphaNumStr, jsLeafGen) {
          (key, value) =>

            val json = Json.obj(
              key -> value
            )

            val lens = JsLens.atKey(key)
            lens.get(json).asOpt.value mustEqual value
        }
      }

      "gets an error when the key is undefined" in {

        forAll(Gen.alphaNumStr) {
          key =>
            val json = Json.obj()
            val lens = JsLens.atKey(key)
            lens.get(json).isError mustBe true
        }
      }

      "gets an error when the `JsValue` is a non object" in {

        val gen = Gen.frequency(
          10 -> jsLeafGen,
          2 -> Gen.const(JsNull)
        )

        forAll(Gen.alphaNumStr, gen) {
          (key, json) =>

            val lens = JsLens.atKey(key)
            lens.get(json).isError mustBe true
        }
      }
    }
  }

  "#atKey (put)" must {
    "return a lens" which {

      "replaces an existing key on an object" in {

        forAll(Gen.alphaNumStr, jsLeafGen, jsLeafGen) {
          (key, oldValue, newValue) =>

            val json = Json.obj(
              key -> oldValue
            )

            val lens = JsLens.atKey(key)
            lens.set(newValue, json).asOpt.value mustEqual Json.obj(key -> newValue)
        }
      }

      "assigns a new key on an object" in {

        forAll(Gen.alphaNumStr, jsLeafGen) {
          (key, value) =>
            val json = Json.obj()
            val lens = JsLens.atKey(key)
            lens.set(value, json).asOpt.value mustEqual Json.obj(key -> value)
        }
      }

      "assigns a new key to a new object when the outer object is null" in {

        forAll(Gen.alphaNumStr, jsLeafGen) {
          (key, value) =>
            val json = JsNull
            val lens = JsLens.atKey(key)
            lens.set(value, json).asOpt.value mustEqual Json.obj(key -> value)
        }
      }

      "fails to add a key to a non-object" in {

        forAll(Gen.alphaNumStr, jsLeafGen, jsLeafGen) {
          (key, json, newValue) =>
            val lens = JsLens.atKey(key)
            lens.set(newValue, json).isError mustBe true
        }
      }
    }
  }

  "#atKey (remove)" must {
    "return a lens" which {

      "removes an existing key on an object" in {

        forAll(Gen.alphaNumStr, jsLeafGen) {
          (key, value) =>
            val lens = JsLens.atKey(key)
            val json = Json.obj(key -> value)
            lens.remove(json).asOpt.value mustEqual Json.obj()
        }
      }

      "not alter an object if the key doesn't exist" in {

        forAll(Gen.alphaNumStr) {
          key =>
            val lens = JsLens.atKey(key)
            lens.remove(Json.obj()).asOpt.value mustEqual Json.obj()
        }
      }

      "not alter JsNull" in {

        forAll(Gen.alphaNumStr) {
          key =>
            val lens = JsLens.atKey(key)
            lens.remove(JsNull).asOpt.value mustEqual JsNull
        }
      }

      "fail when used on a non-object" in {

        forAll(Gen.alphaNumStr, jsLeafGen) {
          (key, value) =>
            val lens = JsLens.atKey(key)
            lens.remove(value).isError mustBe true
        }
      }
    }
  }

  "#atIndex (get)" must {
    "return a lens" which {

      "gets an element from an array when it exists at the given index" in {

        val gen: Gen[(Int, JsArray)] = for {
          idx <- Gen.chooseNum(0, 50)
          arr <- Gen.listOfN(idx + 1, jsLeafGen).map(JsArray(_))
        } yield (idx, arr)

        forAll(gen) {
          case (idx, arr) =>
            val lens = JsLens.atIndex(idx)
            lens.get(arr).asOpt.value mustEqual arr(idx)
        }
      }

      "gets an error when the index doesn't exist in the array" in {

        forAll(Gen.chooseNum(0, 50)) {
          idx =>
            val lens = JsLens.atIndex(idx)
            lens.get(Json.arr()).isError mustEqual true
        }
      }

      "gets an error when the JsValue is not an array" in {

        forAll(Gen.chooseNum(0, 50), jsLeafGen) {
          (idx, json) =>
            val lens = JsLens.atIndex(idx)
            lens.get(json).isError mustEqual true
        }
      }
    }
  }

  "#atIndex (put)" must {
    "return a lens" which {

      "replaces the value at the given index" in {

        val gen: Gen[(Int, JsArray)] = for {
          idx <- Gen.chooseNum(0, 50)
          arr <- Gen.listOfN(idx + 1, jsLeafGen).map(JsArray(_))
        } yield (idx, arr)

        forAll(gen, jsLeafGen) {
          case ((idx, arr), newValue) =>
            val lens = JsLens.atIndex(idx)
            lens.set(newValue, arr).asOpt.value(idx) mustEqual newValue
        }
      }

      "adds the value to an empty array" in {

        forAll(jsLeafGen) {
          value =>
            val arr = Json.arr()
            val lens = JsLens.atIndex(0)
            lens.set(value, arr).asOpt.value mustEqual JsArray(Seq(value))
        }
      }

      "adds the value to an array when the index is one more than the defined indices" in {

        val gen: Gen[(Int, JsArray)] = for {
          idx <- Gen.chooseNum(0, 50)
          arr <- Gen.listOfN(idx + 1, jsLeafGen).map(JsArray(_))
        } yield (idx, arr)

        forAll(gen, jsLeafGen) {
          case ((idx, arr), newValue) =>
            val lens = JsLens.atIndex(idx + 1)
            lens.set(newValue, arr).asOpt.value mustEqual JsArray(arr.value :+ newValue)
        }
      }

      "creates a new array when the JsValue is null" in {

        forAll(jsLeafGen) {
          value =>
            val lens = JsLens.atIndex(0)
            lens.set(value, Json.arr()).asOpt.value mustEqual JsArray(Seq(value))
        }
      }

      "fail to add an index to a non-array" in {

        forAll(Gen.chooseNum(0, 50), jsLeafGen, jsLeafGen) {
          (idx, oldValue, newValue) =>
            val lens = JsLens.atIndex(idx)
            lens.set(newValue, oldValue).isError mustEqual true
        }
      }

      "fail to add an index greater than the size of the array" in {

        val gen: Gen[(Int, JsArray)] = for {
          idx <- Gen.chooseNum(0, 50)
          arr <- Gen.listOfN(idx + 1, jsLeafGen).map(JsArray(_))
        } yield (idx, arr)

        forAll(gen, jsLeafGen) {
          case ((idx, arr), newValue) =>
            val lens = JsLens.atIndex(idx + 2)
            lens.set(newValue, arr).isError mustEqual true
        }
      }

      "fail to add an index less than 0" in {

        forAll(Gen.chooseNum(-50, -1)) {
          idx =>
            an[IllegalArgumentException] mustBe thrownBy {
              JsLens.atIndex(idx)
            }
        }
      }

      "fail to add an index greater than 0 to an empty array" in {

        forAll(Gen.chooseNum(1, 50), jsLeafGen) {
          (idx, value) =>
            val lens = JsLens.atIndex(idx)
            lens.set(value, JsNull).isError mustEqual true
        }
      }
    }
  }

  "#atIndex (remove)" must {
    "return a lens" which {

      "removes an index which exists" in {

        val gen: Gen[(Int, JsArray)] = for {
          idx <- Gen.chooseNum(0, 50)
          arr <- Gen.listOfN(idx + 1, jsLeafGen).map(JsArray(_))
        } yield (idx, arr)

        forAll(gen) {
          case (idx, arr) =>
            val lens = JsLens.atIndex(idx)
            val expected = JsArray(arr.value.patch(idx, Seq.empty, 1))
            lens.remove(arr).asOpt.value mustEqual expected
        }
      }

      "does not modify an array where the index does not exist" in {

        val gen: Gen[(Int, JsArray)] = for {
          idx <- Gen.chooseNum(0, 50)
          arr <- Gen.listOfN(idx - 1, jsLeafGen).map(JsArray(_))
        } yield (idx, arr)

        forAll(gen) {
          case (idx, arr) =>
            val lens = JsLens.atIndex(idx)
            lens.remove(arr).asOpt.value mustEqual arr
        }
      }

      "does not modify JsNull" in {

        forAll(Gen.chooseNum(0, 50)) {
          idx =>
            val lens = JsLens.atIndex(idx)
            lens.remove(JsNull).asOpt.value mustEqual JsNull
        }
      }

      "fails when used on a non-array" in {

        forAll(Gen.chooseNum(0, 50), jsLeafGen) {
          (idx, value) =>
            val lens = JsLens.atIndex(idx)
            lens.remove(value).isError mustBe true
        }
      }
    }

    ".andThen" must {

      "apply lenses in order (get)" in {

        val json = Json.obj(
          "establishers" -> Json.arr(
            Json.obj(
              "name" -> "foo",
              "directors" -> Json.arr(
                Json.obj(
                  "name" -> "dir quack"
                ),
                Json.obj(
                  "name" -> "dir zap"
                )
              )
            ),
            Json.obj(
              "name" -> "bar",
              "directors" -> Json.arr(
                Json.obj(
                  "name" -> "dir foo"
                ),
                Json.obj(
                  "name" -> "dir bar"
                )
              )
            )
          )
        )
        val lens = JsLens.fromPath(__ \ "establishers" \ 0 \ "directors" \ 0 \ "name")

        lens.get(json).asOpt.value mustEqual JsString("dir quack")
      }

      "apply lenses in order (put)" in {

        val json = Json.obj(
          "establishers" -> Json.arr(
            Json.obj(
              "name" -> "foo",
              "directors" -> Json.arr(
                Json.obj(
                  "name" -> "dir quack"
                ),
                Json.obj(
                  "name" -> "dir zap"
                )
              )
            ),
            Json.obj(
              "name" -> "bar",
              "directors" -> Json.arr(
                Json.obj(
                  "name" -> "dir foo"
                ),
                Json.obj(
                  "name" -> "dir bar"
                )
              )
            )
          )
        )
        val expected = Json.obj(
          "establishers" -> Json.arr(
            Json.obj(
              "name" -> "foo",
              "directors" -> Json.arr(
                Json.obj(
                  "name" -> "foo"
                ),
                Json.obj(
                  "name" -> "dir zap"
                )
              )
            ),
            Json.obj(
              "name" -> "bar",
              "directors" -> Json.arr(
                Json.obj(
                  "name" -> "dir foo"
                ),
                Json.obj(
                  "name" -> "dir bar"
                )
              )
            )
          )
        )
        val lens = JsLens.fromPath(__ \ "establishers" \ 0 \ "directors" \ 0 \ "name")

        lens.set(JsString("foo"), json).asOpt.value mustEqual expected
      }

      "apply lenses in order (remove index)" in {

        val json = Json.obj(
          "establishers" -> Json.arr(
            Json.obj(
              "name" -> "foo",
              "directors" -> Json.arr(
                Json.obj(
                  "name" -> "dir quack"
                ),
                Json.obj(
                  "name" -> "dir zap"
                )
              )
            ),
            Json.obj(
              "name" -> "bar",
              "directors" -> Json.arr(
                Json.obj(
                  "name" -> "dir foo"
                ),
                Json.obj(
                  "name" -> "dir bar"
                )
              )
            )
          )
        )
        val expected = Json.obj(
          "establishers" -> Json.arr(
            Json.obj(
              "name" -> "foo",
              "directors" -> Json.arr(
                Json.obj(
                  "name" -> "dir zap"
                )
              )
            ),
            Json.obj(
              "name" -> "bar",
              "directors" -> Json.arr(
                Json.obj(
                  "name" -> "dir foo"
                ),
                Json.obj(
                  "name" -> "dir bar"
                )
              )
            )
          )
        )
        val lens = JsLens.fromPath(__ \ "establishers" \ 0 \ "directors" \ 0)

        lens.remove(json).asOpt.value mustEqual expected
      }

      "apply lenses in order (remove key)" in {

        val json = Json.obj(
          "establishers" -> Json.arr(
            Json.obj(
              "name" -> "foo",
              "directors" -> Json.arr(
                Json.obj(
                  "name" -> "dir quack"
                ),
                Json.obj(
                  "name" -> "dir zap"
                )
              )
            ),
            Json.obj(
              "name" -> "bar",
              "directors" -> Json.arr(
                Json.obj(
                  "name" -> "dir foo"
                ),
                Json.obj(
                  "name" -> "dir bar"
                )
              )
            )
          )
        )
        val expected = Json.obj(
          "establishers" -> Json.arr(
            Json.obj(
              "name" -> "foo",
              "directors" -> Json.arr(
                Json.obj(
                ),
                Json.obj(
                  "name" -> "dir zap"
                )
              )
            ),
            Json.obj(
              "name" -> "bar",
              "directors" -> Json.arr(
                Json.obj(
                  "name" -> "dir foo"
                ),
                Json.obj(
                  "name" -> "dir bar"
                )
              )
            )
          )
        )
        val lens = JsLens.fromPath(__ \ "establishers" \ 0 \ "directors" \ 0 \ "name")

        lens.remove(json).asOpt.value mustEqual expected
      }

      "fail to insert into a new array when the index is greater than 0" in {

        val json = Json.obj()
        val lens = JsLens.atKey("abc") andThen JsLens.atIndex(1)

        lens.set(JsString("foo"), json).isError mustEqual true
      }

      "retrieve multiple" in {

        val json = Json.obj(
          "establishers" -> Json.arr(
            Json.obj(
              "name" -> "foo"
            ),
            Json.obj(
              "name" -> "bar"
            )
          )
        )

        val lens = JsLens.fromPath(__ \ "establishers" \\ "name")

        lens.getAll(json).asOpt.value mustEqual Seq(JsString("foo"), JsString("bar"))
      }

      "fail to set multiple" in {

        val json = Json.obj(
          "establishers" -> Json.arr(
            Json.obj(
              "name" -> "foo"
            ),
            Json.obj(
              "name" -> "bar"
            )
          )
        )

        val lens = JsLens.fromPath(__ \ "establishers" \\ "name")

        lens.set(JsString("foobar"), json).isError mustEqual true
      }
    }
  }
}

