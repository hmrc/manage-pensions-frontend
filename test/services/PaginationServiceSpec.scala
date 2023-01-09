/*
 * Copyright 2023 HM Revenue & Customs
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

package services

import base.SpecBase

class PaginationServiceSpec extends SpecBase {
  val paginationService: PaginationService = new PaginationService
  "PaginationService" must {
    "return 0 when pagination of 0 passed to divide" in {
      paginationService.divide(numberOfSchemes = 10, pagination = 0) mustBe 0
    }

    "round values up when returning not whole values from divide" in {
      paginationService.divide(numberOfSchemes = 6, pagination = 5) mustBe 2
    }

    "return an empty seq from pageNumberLinks when numberOfSchemes < pagination" in {
      paginationService.pageNumberLinks(
        currentPage = 1,
        numberOfSchemes = 5,
        pagination = 10,
        numberOfPages = 1
      ) mustBe Seq.empty
    }

    "return a Seq from 1 - numberOfPages when currentPage < 4 && numberOfPages < 6" in {
      paginationService.pageNumberLinks(
        currentPage = 1,
        numberOfSchemes = 6,
        pagination = 2,
        numberOfPages = 3
      ) mustBe Seq(1, 2, 3)

      paginationService.pageNumberLinks(
        currentPage = 2,
        numberOfSchemes = 6,
        pagination = 2,
        numberOfPages = 3
      ) mustBe Seq(1, 2, 3)

      paginationService.pageNumberLinks(
        currentPage = 3,
        numberOfSchemes = 6,
        pagination = 2,
        numberOfPages = 3
      ) mustBe Seq(1, 2, 3)

    }

    "return a Seq from 1 - 5 when currentPage < 4 && numberOfPages > 6" in {
      paginationService.pageNumberLinks(
        currentPage = 1,
        numberOfSchemes = 20,
        pagination = 2,
        numberOfPages = 10
      ) mustBe Seq(1, 2, 3, 4, 5)

      paginationService.pageNumberLinks(
        currentPage = 2,
        numberOfSchemes = 20,
        pagination = 2,
        numberOfPages = 10
      ) mustBe Seq(1, 2, 3, 4, 5)

      paginationService.pageNumberLinks(
        currentPage = 3,
        numberOfSchemes = 20,
        pagination = 2,
        numberOfPages = 10
      ) mustBe Seq(1, 2, 3, 4, 5)
    }

    "return a Seq from currentPage - 2 to currentPage + 2 when currentPage >= 4 && currentPage <= numberOfPages - 3" in {
      paginationService.pageNumberLinks(
        currentPage = 4,
        numberOfSchemes = 20,
        pagination = 2,
        numberOfPages = 10
      ) mustBe Seq(2, 3, 4, 5, 6)

      paginationService.pageNumberLinks(
        currentPage = 5,
        numberOfSchemes = 20,
        pagination = 2,
        numberOfPages = 10
      ) mustBe Seq(3, 4, 5, 6, 7)

      paginationService.pageNumberLinks(
        currentPage = 6,
        numberOfSchemes = 20,
        pagination = 2,
        numberOfPages = 10
      ) mustBe Seq(4, 5, 6, 7, 8)

      paginationService.pageNumberLinks(
        currentPage = 7,
        numberOfSchemes = 20,
        pagination = 2,
        numberOfPages = 10
      ) mustBe Seq(5, 6, 7, 8, 9)
    }

    "return a Seq from numberOfPages - 4, numberOfPages when when currentPage >= 4 && currentPage within 3 of numberOfPages" in {
      paginationService.pageNumberLinks(
        currentPage = 8,
        numberOfSchemes = 20,
        pagination = 2,
        numberOfPages = 10
      ) mustBe Seq(6, 7, 8, 9, 10)

      paginationService.pageNumberLinks(
        currentPage = 9,
        numberOfSchemes = 20,
        pagination = 2,
        numberOfPages = 10
      ) mustBe Seq(6, 7, 8, 9, 10)

      paginationService.pageNumberLinks(
        currentPage = 10,
        numberOfSchemes = 20,
        pagination = 2,
        numberOfPages = 10
      ) mustBe Seq(6, 7, 8, 9, 10)
    }
  }
}
