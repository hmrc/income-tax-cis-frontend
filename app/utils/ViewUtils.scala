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

package utils

import play.api.i18n.Messages
import play.api.mvc.Call
import uk.gov.hmrc.govukfrontend.views.Aliases
import uk.gov.hmrc.govukfrontend.views.Aliases.SelectItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, Month}
import java.util.Locale
import scala.util.Try

object ViewUtils {

  case class DataRowForView(fieldHeadings: String, fieldValues: Option[String],
                            changeLink: Option[Call] = None, hiddenText: Option[String] = None)

  def convertBoolToYesOrNo(cisField: Boolean)(implicit messages: Messages): String = {
    if (cisField) messages("common.yes") else messages("common.no")
  }

  def dateFormatter(date: String): Option[String] = {
    try {
      Some(LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.UK))
        .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)))
    }
    catch {
      case _: Exception => None
    }
  }

  def dateFormatter(date: LocalDate): String = {
    date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK))
  }

  def getAgentDynamicContent(msgKey: String, isAgent: Boolean): String = {
    s"$msgKey.${if (isAgent) "agent" else "individual"}"
  }

  def monthToTaxYearConverter(month: Month, taxYear: Int): Int = month match {
    case Month.JANUARY | Month.FEBRUARY | Month.MARCH | Month.APRIL => taxYear
    case _ => taxYear - 1
  }

  def translatedMonthAndTaxYear(month: Month, taxYear: Int)(implicit messages: Messages): String =
    messages("common." + month.toString.toLowerCase) + " " + monthToTaxYearConverter(month, taxYear)

  def summaryListRow(key: HtmlContent,
                     value: HtmlContent,
                     keyClasses: String = "govuk-!-width-one-third",
                     valueClasses: String = "govuk-!-width-one-third",
                     actionClasses: String = "govuk-!-width-one-third",
                     actions: Seq[(Call, String, Option[String])]): SummaryListRow = {
    SummaryListRow(
      key = Key(content = key, classes = keyClasses),
      value = Value(content = value, classes = valueClasses),
      actions = Some(Actions(
        items = actions.map { case (call, linkText, visuallyHiddenText) => ActionItem(
          href = call.url,
          content = ariaHiddenChangeLink(linkText),
          visuallyHiddenText = visuallyHiddenText
        )
        },
        classes = actionClasses
      ))
    )
  }

  def ariaHiddenChangeLink(linkText: String): HtmlContent = {
    HtmlContent(s"""<span aria-hidden="true">$linkText</span>""")
  }

  def ariaVisuallyHiddenText(text: String): HtmlContent = {
    HtmlContent(s"""<span class="govuk-visually-hidden">$text</span>""")
  }

  def bigDecimalCurrency(value: String, currencySymbol: String = "£"): String = {
    Try(BigDecimal(value))
      .map(amount => currencySymbol + f"$amount%1.2f".replace(".00", ""))
      .getOrElse(value)
      .replaceAll("\\B(?=(\\d{3})+(?!\\d))", ",")
  }

  def availableMonths(taxYear: Int, prefillMonth: Option[Month])(implicit messages: Messages): Seq[SelectItem] = {
    allMonthsSelectItems(taxYear, prefillMonth)
  }

  //  def availableMonths(submittedMonths: Seq[Month], taxYear: Int, prefillMonth: Option[Month])(implicit messages: Messages): Seq[SelectItem] = {
  //    allMonthsSelectItems(taxYear, prefillMonth)
  //      .filterNot(selectItem => submittedMonths.map(_.name().toLowerCase).contains(selectItem.value.getOrElse("")))
  //  }

  private def allMonthsSelectItems(taxYear: Int, prefillMonth: Option[Month])(implicit messages: Messages): Seq[SelectItem] = {
    val upToApril = Month.APRIL.getValue
    val monthsOrdered: Seq[Month] = Month.values().drop(upToApril) ++ Month.values().take(upToApril)

    monthsOrdered.map(month => mapToSelectItem(taxYear, messages, month, prefillMonth.contains(month)))
  }

  private def mapToSelectItem(taxYear: Int, messages: Messages, month: Month, selected: Boolean): Aliases.SelectItem = {
    val monthMsg = messages(s"common.${month.name().toLowerCase}")

    SelectItem(
      value = Some(month.name().toLowerCase),
      text = messages("common.taxMonth", monthMsg, monthToTaxYearConverter(month, taxYear).toString),
      selected = selected
    )
  }
}
