/*
 * Copyright 2018 HM Revenue & Customs
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

package views.behaviours

import config.FrontendAppConfig
import models.register.Entity
import views.ViewSpecBase

trait EntityListBehaviours {
  this: ViewSpecBase =>

  // scalastyle:off method.length
  def entityList(emptyView: View, nonEmptyView: View, items: Seq[Entity[_]], appConfig: FrontendAppConfig): Unit = {
    "behave like a list of items" must {
      "not show the list if there are no items" in {
        val doc = asDocument(emptyView())
        doc.select("ul#items").size() mustBe 0
      }

      "show the list when there are one or more items" in {
        val doc = asDocument(nonEmptyView())
        doc.select("ul#items").size() mustBe 1
      }

      "display the correct number of items in the list" in {
        val doc = asDocument(nonEmptyView())
        doc.select("#items > li").size() mustBe items.size
      }

      "display the correct details for each person" in {
        val doc = asDocument(nonEmptyView())
        items.foreach { item =>
          val name = doc.select(s"#person-${item.index}")
          name.size mustBe 1
          name.first.text mustBe item.name
        }
      }

      "display the status for each person" in {
        val doc = asDocument(nonEmptyView())
        items.foreach { item =>
          val link = doc.select(s"#person-${item.index}-status")
          val expectedResult = if (item.isCompleted) "COMPLETE" else "INCOMPLETE"

          link.size mustBe 1
          link.first.text mustBe expectedResult
        }
      }

      "disable the submit button if any of the items is incomplete" in {
        val doc = asDocument(nonEmptyView())
        doc.getElementById("submit").hasAttr("disabled") mustBe true
      }

      "display the delete link for each person" in {
        val doc = asDocument(nonEmptyView())
        items.foreach { item =>
          val link = doc.select(s"#person-${item.index}-delete")
          link.size mustBe 1
          link.first.text mustBe messages("site.delete")
          link.first.attr("href") mustBe item.deleteLink
        }
      }

      "display the edit link for each person" in {
        val doc = asDocument(nonEmptyView())
        items.foreach { item =>
          val link = doc.select(s"#person-${item.index}-edit")
          link.size mustBe 1
          link.first.text mustBe messages("site.edit")
          link.first.attr("href") mustBe item.editLink
        }
      }
    }
  }

  // scalastyle:on method.length

}
