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

case class CisUserData(sessionId: String,
                       mtdItId: String,
                       nino: String,
                       taxYear: Int,
                       employerRef: String,
                       submissionId: Option[String],
                       isPriorSubmission: Boolean,
                       cis: Option[CisCYAModel],
                       lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC))

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
                                cis: Option[EncryptedCisCYAModel],
                                lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC))

object EncryptedCisUserData extends MongoJodaFormats {

  implicit val mongoJodaDateTimeFormats: Format[DateTime] = dateTimeFormat

  implicit val formats: Format[EncryptedCisUserData] = Json.format[EncryptedCisUserData]
}
