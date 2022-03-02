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

package models.mongo

import org.joda.time.DateTime
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import utils.DecryptableSyntax.DecryptableOps
import utils.DecryptorInstances.{bigDecimalDecryptor, booleanDecryptor, monthDecryptor}
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.{bigDecimalEncryptor, booleanEncryptor, monthEncryptor}
import utils.{EncryptedValue, SecureGCMCipher}

import java.time.Month

case class CYAPeriodData(deductionPeriod: Month,
                         grossAmountPaid: Option[BigDecimal] = None,
                         deductionAmount: Option[BigDecimal] = None,
                         costOfMaterialsQuestion: Option[Boolean] = None,
                         costOfMaterials: Option[BigDecimal] = None) {

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedCYAPeriodData = EncryptedCYAPeriodData(
    deductionPeriod = deductionPeriod.encrypted,
    grossAmountPaid = grossAmountPaid.map(_.encrypted),
    deductionAmount = deductionAmount.map(_.encrypted),
    costOfMaterialsQuestion = costOfMaterialsQuestion.map(_.encrypted),
    costOfMaterials = costOfMaterials.map(_.encrypted)
  )

  def isFinished: Boolean = {
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
                                  costOfMaterials: Option[EncryptedValue] = None) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): CYAPeriodData = CYAPeriodData(
    deductionPeriod = deductionPeriod.decrypted[Month],
    grossAmountPaid = grossAmountPaid.map(_.decrypted[BigDecimal]),
    deductionAmount = deductionAmount.map(_.decrypted[BigDecimal]),
    costOfMaterialsQuestion = costOfMaterialsQuestion.map(_.decrypted[Boolean]),
    costOfMaterials = costOfMaterials.map(_.decrypted[BigDecimal])
  )
}

object EncryptedCYAPeriodData extends MongoJodaFormats {
  implicit val mongoJodaDateTimeFormats: Format[DateTime] = dateTimeFormat

  implicit val formats: Format[EncryptedCYAPeriodData] = Json.format[EncryptedCYAPeriodData]
}

