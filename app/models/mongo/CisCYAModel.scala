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

import org.joda.time.DateTime
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import utils.AesGcmAdCrypto
import utils.CypherSyntax.{DecryptableOps, EncryptableOps}

import java.time.Month

case class CisCYAModel(contractorName: Option[String] = None,
                       periodData: Option[CYAPeriodData] = None,
                       priorPeriodData: Seq[CYAPeriodData] = Seq.empty) {

  lazy val periodDataUpdated: Boolean =
    periodData.forall(!priorPeriodData.contains(_))

  lazy val isFinished: Boolean = periodData.exists(_.isFinished)

  lazy val cyaPeriodData: Seq[CYAPeriodData] = periodData
    .map(periodData => periodData +: priorPeriodData.filterNot(_.originallySubmittedPeriod == periodData.originallySubmittedPeriod))
    .getOrElse(priorPeriodData)

  def isNewSubmissionFor(month: Month): Boolean =
    !priorPeriodData.map(_.deductionPeriod).contains(month)

  def isAnUpdateFor(month: Month): Boolean =
    periodData.exists(_.isAnUpdateFor(month))

  def encrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedCisCYAModel = EncryptedCisCYAModel(
    contractorName = contractorName.map(_.encrypted),
    periodData = periodData.map(_.encrypted),
    priorPeriodData = priorPeriodData.map(_.encrypted)
  )
}

object CisCYAModel {
  implicit val format: OFormat[CisCYAModel] = Json.format[CisCYAModel]
}

case class EncryptedCisCYAModel(contractorName: Option[EncryptedValue] = None,
                                periodData: Option[EncryptedCYAPeriodData] = None,
                                priorPeriodData: Seq[EncryptedCYAPeriodData] = Seq()) {

  def decrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): CisCYAModel = CisCYAModel(
    contractorName = contractorName.map(_.decrypted[String]),
    periodData = periodData.map(_.decrypted),
    priorPeriodData = priorPeriodData.map(_.decrypted)
  )
}

object EncryptedCisCYAModel extends MongoJodaFormats {
  implicit val mongoJodaDateTimeFormats: Format[DateTime] = dateTimeFormat
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]
  implicit val formats: Format[EncryptedCisCYAModel] = Json.format[EncryptedCisCYAModel]
}
