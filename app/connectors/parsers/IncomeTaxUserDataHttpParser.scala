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

package connectors.parsers

import models.{APIErrorModel, IncomeTaxUserData}
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys.{INTERNAL_SERVER_ERROR_FROM_API, SERVICE_UNAVAILABLE_FROM_API, UNEXPECTED_RESPONSE_FROM_API}
import utils.PagerDutyHelper.{logger, pagerDutyLog}

object IncomeTaxUserDataHttpParser extends APIParser {
  type IncomeTaxUserDataResponse = Either[APIErrorModel, IncomeTaxUserData]

  override val parserName: String = "IncomeTaxUserDataHttpParser"
  override val service: String = "income-tax-submission"

  implicit object IncomeTaxUserDataHttpReads extends HttpReads[IncomeTaxUserDataResponse] {
    override def read(method: String, url: String, response: HttpResponse): IncomeTaxUserDataResponse = {
      response.status match {
        case OK =>
          logger.info("TEMP_LOGGING: case OK")
          try {
            response.json.validate[IncomeTaxUserData]
          } catch {
            case error@_ => println(s"TEMP_LOGGING: Exception error = ${error}")
          }
          val value = response.json.validate[IncomeTaxUserData]
          logger.info("TEMP_LOGGING: Json validated")
          value.fold[IncomeTaxUserDataResponse](
            _ => badSuccessJsonFromAPI,
            parsedModel => Right(parsedModel)
          )
        case NO_CONTENT =>
          logger.info("TEMP_LOGGING: NO_CONTENT scenario")
          Right(IncomeTaxUserData())
        case INTERNAL_SERVER_ERROR =>
          logger.info("TEMP_LOGGING: INTERNAL_SERVER_ERROR scenario")
          pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_API, logMessage(response))
          handleAPIError(response)
        case SERVICE_UNAVAILABLE =>
          logger.info("TEMP_LOGGING: SERVICE_UNAVAILABLE scenario")
          pagerDutyLog(SERVICE_UNAVAILABLE_FROM_API, logMessage(response))
          handleAPIError(response)
        case _ =>
          logger.info("TEMP_LOGGING: _ scenario")
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, logMessage(response))
          handleAPIError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }
}
