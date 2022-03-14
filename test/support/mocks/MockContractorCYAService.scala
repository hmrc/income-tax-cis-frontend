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

package support.mocks

import models.pages.ContractorCYAPage
import models.{ServiceErrors, User}
import org.scalamock.handlers.CallHandler5
import org.scalamock.scalatest.MockFactory
import services.ContractorCYAService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Month
import scala.concurrent.Future

trait MockContractorCYAService extends MockFactory {

  protected val mockContractorCYAService: ContractorCYAService = mock[ContractorCYAService]

  def mockPageModelFor(taxYear: Int,
                       month: Month,
                       refNumber: String,
                       user: User,
                       result: Either[ServiceErrors, ContractorCYAPage]
                      ): CallHandler5[Int, Month, String, User, HeaderCarrier, Future[Either[ServiceErrors, ContractorCYAPage]]] = {
    (mockContractorCYAService.pageModelFor(_: Int, _: Month, _: String, _: User)(_: HeaderCarrier))
      .expects(taxYear, month, refNumber, user, *)
      .returns(Future.successful(result))
  }
}
