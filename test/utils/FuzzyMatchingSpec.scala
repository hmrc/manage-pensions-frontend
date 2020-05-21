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

import org.scalatest.{FreeSpec, MustMatchers}

class FuzzyMatchingSpec extends FreeSpec with MustMatchers {

  val fuzzyMatching = new FuzzyMatching

  "doFuzzyMatching" - {
    "must return false" - {
      "when search string is of 2 characters or less" in {
        val result = fuzzyMatching.doFuzzyMatching(searchString = "to", inputString = "this need to work")
        result mustBe false
      }

      Seq("nead", "nod").foreach { searchString =>
        s"when search string $searchString is of 3 or 4 characters and its not a 100% match" in {
          val result = fuzzyMatching.doFuzzyMatching(searchString, inputString = "this need not work")
          result mustBe false
        }
      }

      Seq("singal", "leets", "multipal", "impats").foreach { searchString =>
        s"when search string $searchString is 5 characters or more and its not an 80% match" in {
          val result = fuzzyMatching.doFuzzyMatching(searchString, inputString = "this can work at least a single time for multiple inputs")
          result mustBe false
        }
      }

      Seq("singal time", "multipal inputs").foreach { searchString =>
        s"when search string $searchString is of two words and only one of the word is matching" in {
          val result = fuzzyMatching.doFuzzyMatching(searchString, inputString = "this can work at least a single time for multiple inputs")
          result mustBe false
        }
      }
    }

    "must return true" - {

      Seq("need", "not").foreach { searchString =>
        s"when search string $searchString is of 3 or 4 characters and its a 100% match" in {
          val result = fuzzyMatching.doFuzzyMatching(searchString, inputString = "this need not work")
          result mustBe true
        }
      }

      Seq("SinGla", "leest", "Multeple", "Inpats").foreach { searchString =>
        s"when search string $searchString is 5 characters or more and its more than 80% match" in {
          val result = fuzzyMatching.doFuzzyMatching(searchString, inputString = "this can work at least a single time for multiple inputs")
          result mustBe true
        }
      }

      Seq("singla time", "multipl input", "can work at leest").foreach { searchString =>
        s"when search string $searchString is of two or more words and all of the words is matching" in {
          val result = fuzzyMatching.doFuzzyMatching(searchString, inputString = "this can work at least a single time for multiple inputs")
          result mustBe true
        }
      }
    }
  }

}
