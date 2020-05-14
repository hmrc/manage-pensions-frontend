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
import utils.FuzzyMatching

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class SchemeSearchService @Inject()(listSchemesConnector: ListOfSchemesConnector, fuzzyMatching: FuzzyMatching) {
  private val srnRegex = "^S[0-9]{10}$".r
  private val pstrRegex = "^[0-9]{8}[A-Za-z]{2}$".r

  private val filterSchemesBySrnOrPstr
  : (String, List[SchemeDetail]) => List[SchemeDetail] =
    (searchText, list) => {
      searchText match {
        case srn if srnRegex.findFirstIn(searchText).isDefined =>
          list.filter(_.referenceNumber == searchText)
        case pstr if pstrRegex.findFirstIn(searchText).isDefined =>
          list.filter(_.pstr.exists(_ == searchText))
        case _ =>
          list.flatMap { schemeDetail =>
            if (fuzzyMatching.doFuzzyMatching(searchText.toLowerCase(), schemeDetail.name.toLowerCase())) Some(schemeDetail) else None
          }
      }
    }

  def search( psaId: String, searchText: Option[String])(implicit hc: HeaderCarrier, ec: ExecutionContext):Future[List[SchemeDetail]] = {
    listSchemesConnector.getListOfSchemes(psaId)
      .map{ listOfSchemes =>
        val filterSearchResults =
          searchText.fold[List[SchemeDetail] => List[SchemeDetail]](identity)(
            st => filterSchemesBySrnOrPstr(st, _: List[SchemeDetail])
          )

        filterSearchResults(listOfSchemes.schemeDetail.getOrElse(List.empty[SchemeDetail]))
      }
  }:Future[List[SchemeDetail]]
}
