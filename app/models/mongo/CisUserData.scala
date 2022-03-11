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

import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import utils.SecureGCMCipher

case class CisUserData(sessionId: String,
                       mtdItId: String,
                       nino: String,
                       taxYear: Int,
                       employerRef: String,
                       submissionId: Option[String],
                       isPriorSubmission: Boolean,
                       cis: CisCYAModel,
                       lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC)) {

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedCisUserData = EncryptedCisUserData(
    sessionId = sessionId,
    mtdItId = mtdItId,
    nino = nino,
    taxYear = taxYear,
    employerRef = employerRef,
    submissionId = submissionId,
    isPriorSubmission = isPriorSubmission,
    cis = cis.encrypted(),
    lastUpdated = lastUpdated
  )
}

object CisUserData extends MongoJodaFormats {

  implicit val mongoJodaDateTimeFormats: Format[DateTime] = dateTimeFormat

  implicit val formats: Format[CisUserData] = Json.format[CisUserData]
}

case class EncryptedCisUserData(sessionId: String,
                                mtdItId: String,
                                nino: String,
                                taxYear: Int,
                                employerRef: String,
                                submissionId: Option[String],
                                isPriorSubmission: Boolean,
                                cis: EncryptedCisCYAModel,
                                lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC)) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): CisUserData = CisUserData(
    sessionId = sessionId,
    mtdItId = mtdItId,
    nino = nino,
    taxYear = taxYear,
    employerRef = employerRef,
    submissionId = submissionId,
    isPriorSubmission = isPriorSubmission,
    cis = cis.decrypted(),
    lastUpdated = lastUpdated
  )
}

object EncryptedCisUserData extends MongoJodaFormats {
  implicit val mongoJodaDateTimeFormats: Format[DateTime] = dateTimeFormat

  implicit val formats: Format[EncryptedCisUserData] = Json.format[EncryptedCisUserData]
}
