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

import com.google.inject.Inject
import connectors.scheme.ListOfSchemesConnector
import models.SchemeDetail
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class SchemeSearchService @Inject()(listSchemesConnector: ListOfSchemesConnector,
                                    fuzzyMatching: FuzzyMatchingService) {

  private val srnRegex = "^S[0-9]{10}$".r
  private val pstrRegex = "^[0-9]{8}[A-Za-z]{2}$".r


  def search(psaId: String, searchText: Option[String])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[SchemeDetail]] = {

    listSchemesConnector.getListOfSchemes(psaId).map { listOfSchemes =>

      val list = listOfSchemes.schemeDetail.getOrElse(Nil)

      searchText.fold[List[SchemeDetail]](list) {
        case srn if srnRegex.findFirstIn(srn).isDefined =>
          list.filter(_.referenceNumber == srn)
        case pstr if pstrRegex.findFirstIn(pstr).isDefined =>
          list.filter(_.pstr.exists(_ == pstr))
        case schemeName =>
          list.filter(scheme => fuzzyMatching.score(scheme.name, schemeName) > 0.85)

      }
    }
  }
}
