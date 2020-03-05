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

package services

import javax.inject.Inject

class PaginationService @Inject()() {
  def divide(numberOfSchemes: Int,
             pagination: Int): Int =
    if (pagination > 0)
      (BigDecimal(numberOfSchemes) / BigDecimal(pagination)).setScale(0, BigDecimal.RoundingMode.UP).toInt
    else
      0

  def pageNumberLinks(currentPage: Int,
                      numberOfSchemes: Int,
                      pagination: Int,
                      numberOfPages: Int): Seq[Int] = {
    if (numberOfSchemes < pagination)
      Seq.empty
    else if (currentPage < 4 && numberOfPages < 6)
      Seq.range(1, numberOfPages + 1)
    else if (currentPage < 4 && numberOfPages > 5)
      Seq.range(1, 6)
    else if (currentPage >= 4 && currentPage <= numberOfPages - 3)
      Seq.range(currentPage - 2, currentPage + 3)
    else
      Seq.range(numberOfPages - 4, numberOfPages + 1)
  }
}
