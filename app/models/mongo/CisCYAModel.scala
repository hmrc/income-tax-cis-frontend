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
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import utils.DecryptableSyntax.DecryptableOps
import utils.DecryptorInstances.stringDecryptor
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.stringEncryptor
import utils.{EncryptedValue, SecureGCMCipher}

case class CisCYAModel(contractorName: String,
                       periodData: Seq[CYAPeriodData]) {

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedCisCYAModel = EncryptedCisCYAModel(
    contractorName = contractorName.encrypted,
    periodData = periodData.map(_.encrypted)
  )

  def isFinished: Boolean = periodData.nonEmpty
}

object CisCYAModel {
  implicit val format: OFormat[CisCYAModel] = Json.format[CisCYAModel]
}

case class EncryptedCisCYAModel(contractorName: EncryptedValue,
                                periodData: Seq[EncryptedCYAPeriodData]) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): CisCYAModel = CisCYAModel(
    contractorName = contractorName.decrypted[String],
    periodData = periodData.map(_.decrypted)
  )
}

object EncryptedCisCYAModel extends MongoJodaFormats {
  implicit val mongoJodaDateTimeFormats: Format[DateTime] = dateTimeFormat

  implicit val formats: Format[EncryptedCisCYAModel] = Json.format[EncryptedCisCYAModel]
}
