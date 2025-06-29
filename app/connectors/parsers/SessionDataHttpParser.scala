/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors.parsers

import models.APIErrorModel
import models.session.SessionData
import play.api.http.Status._
import play.api.libs.json.JsSuccess
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys.{INTERNAL_SERVER_ERROR_FROM_API, SERVICE_UNAVAILABLE_FROM_API, UNEXPECTED_RESPONSE_FROM_API}
import utils.PagerDutyHelper.pagerDutyLog

object SessionDataHttpParser extends APIParser {

  type SessionDataResponse = Either[APIErrorModel, Option[SessionData]]

  override val parserName: String = "SessionDataHttpParser"
  override val service: String = "income-tax-session-data"

  implicit object SessionDataResponseReads extends HttpReads[SessionDataResponse] {
    override def read(method: String, url: String, response: HttpResponse): SessionDataResponse = {
      response.status match {
        case OK =>
          response.json.validate[SessionData] match {
            case JsSuccess(parsedModel, _) => Right(Some(parsedModel))
            case _ => badSuccessJsonFromAPI
          }
        case NOT_FOUND | NO_CONTENT =>
          Right(None)
        case SERVICE_UNAVAILABLE =>
          pagerDutyLog(SERVICE_UNAVAILABLE_FROM_API, logMessage(response))
          handleAPIError(response)
        case INTERNAL_SERVER_ERROR =>
          pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_API, logMessage(response))
          handleAPIError(response)
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, logMessage(response))
          handleAPIError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }
}