/*
 * Copyright 2022 HM Revenue & Customs
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

package views

import base.SpecBase
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.scalatest.Assertion
import org.scalatest.matchers.MatchResult
import org.scalatest.matchers.Matcher
import play.twirl.api.Html
import play.twirl.api.HtmlFormat

trait ViewSpecBase extends SpecBase {

  type View = () => HtmlFormat.Appendable

  def haveLink(url: String): Matcher[Elements] = Matcher[Elements] {
    elements =>
      val href = elements.attr("href")
      MatchResult(
        href == url,
        s"href $href is not equal to the url $url",
        s"href $href is equal to the url $url"
      )
  }

  def haveLinkWithUrlAndContent(linkId: String, url: String, expectedContent: String): Matcher[Document] = Matcher[Document]{
    document =>
    val link = document.select(s"a[id=$linkId]")
      val actualContent = link.text()

    val href = link.attr("href")
    MatchResult(
      href == url && expectedContent == actualContent,
      s"link id $linkId with link text $actualContent and href $href is not rendered on the page",
      s"link id $linkId with link text $actualContent and href $href is rendered on the page"
    )
  }

  def haveDynamicText(messageKey: String, args: Any*): Matcher[Document] = Matcher[Document] {
    document =>
      val text = messages(messageKey, args: _*)
      MatchResult(
        document.toString.contains(text),
        s"text $text is not rendered on the page",
        s"text $text is rendered on the page"
      )
  }

  def haveLabelAndValue(forElement: String, expectedLabel: String, expectedValue: String): Matcher[Document] = Matcher[Document] {
    document =>
      val labels = document.getElementsByAttributeValue("for", forElement)
      val label = labels.first.text
      val value = document.getElementById(forElement).attr("value")
      MatchResult(
        label == expectedLabel && value == expectedValue,
        s"text box with label: $label and value : $value is not correct",
        s"text box has correct label: $label and correct value: $value"
      )
  }

  def haveErrorOnSummary(id: String, expectedErrorMessage: String): Matcher[Document] = Matcher[Document] {
    document =>
      val href = document.select(s"a[href='#$id']").text()
      MatchResult(
        document.select("#error-summary-heading").size() != 0 && href == expectedErrorMessage,
        s"Error $expectedErrorMessage for field with id $id is not displayed on error summary",
        s"Error $expectedErrorMessage for field with id $id is displayed on error summary"
      )
  }

  def haveCheckBox(id: String, value: String): Matcher[Document] = Matcher[Document] {
    document =>
      val checkbox = document.select(s"input[id=$id][type=checkbox][value=$value]")

      MatchResult(
        checkbox.size == 1,
        s"Checkbox with Id $id and value $value not rendered on page",
        s"Checkbox with Id $id and value $value rendered on page"
      )
  }

  def haveLinkOnClick(action: String, linkId: String): Matcher[Document] = Matcher[Document] {
    document =>
      val link = document.select(s"a[id=$linkId]")
      val onClick = link.attr("onClick")
      MatchResult(
        onClick == action,
        s"link $linkId onClick $onClick is not equal to $action",
        s"link $linkId onClick $onClick is equal to $action"
      )
  }

  def asDocument(html: Html): Document = Jsoup.parse(html.toString())

  def assertEqualsMessage(doc: Document, cssSelector: String, expectedMessageKey: String): Assertion =
    assertEqualsValue(doc, cssSelector, messages(expectedMessageKey))

  def assertEqualsValueOwnText(doc: Document, cssSelector: String, expectedValue: String): Assertion = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    //<p> HTML elements are rendered out with a carriage return on some pages, so discount for comparison
    assert(elements.first().ownText().replace("\n", "") == expectedValue)
  }

  def assertEqualsValue(doc: Document, cssSelector: String, expectedValue: String): Assertion = {
    val elements = doc.select(cssSelector)

    if (elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    //<p> HTML elements are rendered out with a carriage return on some pages, so discount for comparison
    assert(elements.first().html().replace("\n", "") == expectedValue)
  }

  def assertPageTitleEqualsMessage(doc: Document, expectedMessage: String): Assertion = {
    val headers = doc.getElementsByTag("h1")
    headers.size mustBe 1
    headers.first.text.replaceAll("\u00a0", " ") mustBe expectedMessage.replaceAll("&nbsp;", " ")
  }

  def assertContainsText(doc: Document, text: String): Assertion = assert(doc.toString.contains(text), "\n\ntext " + text + " was not rendered on the page.\n")

  def assertContainsMessages(doc: Document, expectedMessageKeys: String*): Unit = {
    for (key <- expectedMessageKeys) assertContainsText(doc, messages(key))
  }

  def assertRenderedById(doc: Document, id: String): Assertion = {
    assert(doc.getElementById(id) != null, "\n\nElement " + id + " was not rendered on the page.\n")
  }

  def assertNotRenderedById(doc: Document, id: String): Assertion = {
    assert(doc.getElementById(id) == null, "\n\nElement " + id + " was rendered on the page.\n")
  }

  def assertRenderedByIdWithText(doc: Document, id: String, text: String): Assertion = {
    val element = doc.getElementById(id)
    assert(element != null, "\n\nElement " + id + " was not rendered on the page.\n")
    assert(element.text().equals(text), s"\n\nElement $id had text '${element.text()}' not '$text'.\n")
  }

  def assertRenderedByCssSelector(doc: Document, cssSelector: String): Assertion = {
    assert(!doc.select(cssSelector).isEmpty, "Element " + cssSelector + " was not rendered on the page.")
  }

  def assertNotRenderedByCssSelector(doc: Document, cssSelector: String): Assertion = {
    assert(doc.select(cssSelector).isEmpty, "\n\nElement " + cssSelector + " was rendered on the page.\n")
  }

  def assertContainsLabel(doc: Document, forElement: String, expectedText: String, expectedHintText: Option[String] = None): Any = {
    val labels = doc.getElementsByAttributeValue("for", forElement)
    assert(labels.size == 1, s"\n\nLabel for $forElement was not rendered on the page.")
    val label = labels.select("span")
    assert(label.first.text() == expectedText, s"\n\nLabel for $forElement was not $expectedText")

    if (expectedHintText.isDefined) {
      assert(labels.first.getElementsByClass("form-hint").first.text == expectedHintText.get,
        s"\n\nLabel for $forElement did not contain hint text $expectedHintText")
    }
  }

  def assertElementHasClass(doc: Document, id: String, expectedClass: String): Assertion = {
    assert(doc.getElementById(id).hasClass(expectedClass), s"\n\nElement $id does not have class $expectedClass")
  }

  def assertContainsRadioButton(doc: Document, id: String, name: String, value: String, isChecked: Boolean): Assertion = {
    assertRenderedById(doc, id)
    val radio = doc.getElementById(id)

    assert(radio.attr("name") == name, s"\n\nElement $id does not have name $name")
    assert(radio.attr("value") == value, s"\n\nElement $id does not have value $value")
    if (isChecked) {
      assert(radio.attr("checked") == "checked", s"\n\nElement $id is not checked")
    } else {
      assert(!radio.hasAttr("checked") && radio.attr("checked") != "checked", s"\n\nElement $id is checked")
    }
  }

  def assertContainsSelectOption(doc: Document, id: String, label: String, value: String, isChecked: Boolean): Assertion = {
    assertRenderedById(doc, id)
    val select = doc.getElementById(id)

    assert(select.text == label, s"\n\nElement $id does not have label $label")
    assert(select.attr("value") == value, s"\n\nElement $id does not have value $value")
    if (isChecked) {
      assert(select.hasAttr("selected"), s"\n\nElement $id is not selected")
    } else {
      assert(!select.hasAttr("selected"), s"\n\nElement $id is selected")
    }
  }

  def assertLink(doc: Document, linkId: String, url: String): Assertion = {
    val link = doc.select(s"a[id=$linkId]")
    assert(link.size() == 1, s"\n\nLink $linkId is not displayed")
    val href = link.attr("href")
    assert(href == url, s"\n\nLink $linkId has href $href no $url")
  }

  def haveLink(url: String, linkId: String): Matcher[View] = Matcher[View] {
    view =>
      val link = Jsoup.parse(view().toString()).select(s"a[id=$linkId]")
      val href = link.attr("href")
      MatchResult(
        href == url,
        s"link $linkId href $href is not equal to the url $url",
        s"link $linkId href $href is equal to the url $url"
      )
  }

  def haveElementWithText(id: String, text: String): Matcher[View] = Matcher[View] {
    view =>
      val element = Jsoup.parse(view().toString()).getElementById(id)
      MatchResult(
        element != null && element.text() == text,
        s"element $id does not have text $text. Text is ${element.text()}",
        s"element $id has text $text"
      )
  }

  def haveElementWithClass(id: String, className: String): Matcher[View] = Matcher[View] {
    view =>
      val element = Jsoup.parse(view().toString()).getElementById(id)
      MatchResult(
        element != null && element.hasClass(className),
        s"element $id does not have class $className. Class is ${element.classNames.toString}",
        s"element $id has class $className"
      )
  }

  def haveClassWithSize(className: String, size: Int, id: String = ""): Matcher[View] = Matcher[View] {
    view =>
      val list = if (id.isEmpty) {
        Jsoup.parse(view().toString()).getElementsByClass(className)
      } else {
        val idEls = Jsoup.parse(view().toString()).getElementById(id)
        idEls.getElementsByClass(className)
      }

      MatchResult(
        list != null && list.size() == size,
        s"Not the correct amount of elements with class $className. Size is ${list.size()}, expected $size",
        s"There are $size Elements with class of $className"
      )
  }

  def notHaveElementWithId(id: String): Matcher[View] = Matcher[View] {
    view =>
      val element = Jsoup.parse(view().toString()).getElementById(id)

      MatchResult(
        element == null,
        s"element$id was rendered but should not have been",
        s"element $id was not rendered"
      )
  }

}
