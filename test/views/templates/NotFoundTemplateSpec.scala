/*
 * Copyright 2024 HM Revenue & Customs
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

package views.templates

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.HtmlFormat
import utils.ViewTest
import views.html.templates.NotFoundTemplate

class NotFoundTemplateSpec extends ViewTest {

  object Selectors {

    val h1Selector = "#main-content > div > div > header > h1"
    val p1Selector = "#main-content > div > div > div.govuk-body > p:nth-child(1)"
    val p2Selector = "#main-content > div > div > div.govuk-body > p:nth-child(2)"
    val p3Selector = "#main-content > div > div > div.govuk-body > p:nth-child(3)"
    val linkSelector = "#govuk-self-assessment-link"
  }

  object expectedResultsEN {
    val h1Expected = "Page not found"
    val p1Expected = "If you typed the web address, check it is correct."
    val p2Expected = "If you used ‘copy and paste’ to enter the web address, check you copied the full address."
    val p3Expected: String = "If the web address is correct or you selected a link or button, you can use Self Assessment: " +
      "general enquiries (opens in new tab) to speak to someone about your income tax."
    val p3ExpectedLink = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"
    val p3ExpectedLinkText = "Self Assessment: general enquiries (opens in new tab)"
  }

  object expectedResultsCY {
    val h1Expected = "Heb ddod o hyd i’r dudalen"
    val p1Expected = "Os gwnaethoch deipio’r cyfeiriad gwe, gwiriwch ei fod yn gywir."
    val p2Expected = "Os gwnaethoch ddefnyddio ‘copïo a gludo’ er mwyn nodi’r cyfeiriad gwe, gwiriwch eich bod wedi copïo’r cyfeiriad llawn."
    val p3Expected: String = "Os yw’r cyfeiriad gwe yn gywir neu os ydych wedi dewis cysylltiad neu fotwm, gallwch ddefnyddio Hunanasesiad: ymholiadau cyffredinol (yn agor tab newydd) " +
      "i siarad â rhywun am eich treth incwm."
    val p3ExpectedLink = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"
    val p3ExpectedLinkText = "Hunanasesiad: ymholiadau cyffredinol (yn agor tab newydd)"
  }

  private val notFoundTemplate: NotFoundTemplate = app.injector.instanceOf[NotFoundTemplate]

  "NotFoundTemplate in English" should {
    import expectedResultsEN._

    "render the page correctly" which {
      lazy val view: HtmlFormat.Appendable = notFoundTemplate()(fakeRequest, messages, mockAppConfig)
      implicit lazy val document: Document = Jsoup.parse(view.body)

      titleCheck(h1Expected, isWelsh = false)
      welshToggleCheck("English")
      h1Check(h1Expected, "xl")

      textOnPageCheck(p1Expected, Selectors.p1Selector)
      textOnPageCheck(p2Expected, Selectors.p2Selector)
      textOnPageCheck(p3Expected, Selectors.p3Selector)
      linkCheck(p3ExpectedLinkText, Selectors.linkSelector, p3ExpectedLink)

    }
  }

  "NotFoundTemplate in Welsh" should {
    import expectedResultsCY._
    "render the page correctly" which {

      lazy val view: HtmlFormat.Appendable = notFoundTemplate()(fakeRequest, welshMessages, mockAppConfig)
      implicit lazy val document: Document = Jsoup.parse(view.body)

      titleCheck(h1Expected, isWelsh = true)
      welshToggleCheck("Welsh")
      h1Check(h1Expected, "xl")

      textOnPageCheck(p1Expected, Selectors.p1Selector)
      textOnPageCheck(p2Expected, Selectors.p2Selector)
      textOnPageCheck(p3Expected, Selectors.p3Selector)
      linkCheck(p3ExpectedLinkText, Selectors.linkSelector, p3ExpectedLink)

    }
  }

}
