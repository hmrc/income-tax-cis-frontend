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

package models.mongo

import models.submission.PeriodData
import org.joda.time.DateTime
import play.api.libs.json._
import uk.gov.hmrc.crypto.EncryptedValue
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import utils.AesGcmAdCrypto
import utils.CypherSyntax.{DecryptableOps, EncryptableOps}

import java.time.Month

case class CYAPeriodData(deductionPeriod: Month,
                         grossAmountPaid: Option[BigDecimal] = None,
                         deductionAmount: Option[BigDecimal] = None,
                         costOfMaterialsQuestion: Option[Boolean] = None,
                         costOfMaterials: Option[BigDecimal] = None,
                         contractorSubmitted: Boolean,
                         originallySubmittedPeriod: Option[Month]) {

  def toSubmissionPeriodData(taxYear: Int): Option[PeriodData] = {
    deductionAmount.map { deductionAmount =>
      PeriodData(taxYear, deductionPeriod, grossAmountPaid, deductionAmount, costOfMaterials)
    }
  }

  def isAnUpdateFor(month: Month): Boolean =
    originallySubmittedPeriod.contains(month)

  def encrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedCYAPeriodData = EncryptedCYAPeriodData(
    deductionPeriod = deductionPeriod.encrypted,
    grossAmountPaid = grossAmountPaid.map(_.encrypted),
    deductionAmount = deductionAmount.map(_.encrypted),
    costOfMaterialsQuestion = costOfMaterialsQuestion.map(_.encrypted),
    costOfMaterials = costOfMaterials.map(_.encrypted),
    contractorSubmitted = contractorSubmitted.encrypted,
    originallySubmittedPeriod = originallySubmittedPeriod.map(_.encrypted)
  )

  lazy val isFinished: Boolean = {
    val costOfMaterialsFinished = costOfMaterialsQuestion match {
      case Some(true) => costOfMaterials.isDefined
      case Some(false) => true
      case None => false
    }

    grossAmountPaid.isDefined &&
      deductionAmount.isDefined &&
      costOfMaterialsFinished
  }
}

object CYAPeriodData {
  implicit val reads: Reads[Month] = JsPath.read[String].map(Month.valueOf)

  implicit val writes: Writes[Month] = Writes { month => JsString(month.toString) }

  implicit val format: OFormat[CYAPeriodData] = Json.format[CYAPeriodData]
}

case class EncryptedCYAPeriodData(deductionPeriod: EncryptedValue,
                                  grossAmountPaid: Option[EncryptedValue] = None,
                                  deductionAmount: Option[EncryptedValue] = None,
                                  costOfMaterialsQuestion: Option[EncryptedValue] = None,
                                  costOfMaterials: Option[EncryptedValue] = None,
                                  contractorSubmitted: EncryptedValue,
                                  originallySubmittedPeriod: Option[EncryptedValue] = None) {

  def decrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): CYAPeriodData = CYAPeriodData(
    deductionPeriod = deductionPeriod.decrypted[Month],
    grossAmountPaid = grossAmountPaid.map(_.decrypted[BigDecimal]),
    deductionAmount = deductionAmount.map(_.decrypted[BigDecimal]),
    costOfMaterialsQuestion = costOfMaterialsQuestion.map(_.decrypted[Boolean]),
    costOfMaterials = costOfMaterials.map(_.decrypted[BigDecimal]),
    contractorSubmitted = contractorSubmitted.decrypted[Boolean],
    originallySubmittedPeriod = originallySubmittedPeriod.map(_.decrypted[Month])
  )
}

object EncryptedCYAPeriodData extends MongoJodaFormats {
  implicit val mongoJodaDateTimeFormats: Format[DateTime] = dateTimeFormat
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]
  implicit val formats: Format[EncryptedCYAPeriodData] = Json.format[EncryptedCYAPeriodData]
}

