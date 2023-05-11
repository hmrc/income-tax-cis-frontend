/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors

import config.AppConfig
import connectors.parsers.CISHttpParser.CISResponse
import models.submission.CISSubmission
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CISConnector @Inject()(val http: HttpClient, val config: AppConfig)(implicit ec: ExecutionContext) {

  def submit(nino: String, taxYear: Int, submission: CISSubmission)(implicit hc: HeaderCarrier): Future[CISResponse] = {
    import connectors.parsers.CISHttpParser.{CISHttpReads, CISResponse}
    val cisUrl: String = config.incomeTaxCISBEUrl + s"/income-tax/nino/$nino/sources?taxYear=$taxYear"
    http.POST[CISSubmission, CISResponse](cisUrl, submission)
  }

  def delete(nino: String, taxYear: Int, submissionId: String)(implicit hc: HeaderCarrier): Future[CISResponse] = {
    import connectors.parsers.DeleteCISHttpParser.DeleteCISHttpReads

    val cisUrl: String = config.incomeTaxCISBEUrl + s"/income-tax/nino/$nino/sources/$submissionId?taxYear=$taxYear"
    http.DELETE[CISResponse](cisUrl)
  }
}
