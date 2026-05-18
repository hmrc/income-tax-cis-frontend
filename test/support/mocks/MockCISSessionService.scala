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

package support.mocks

import models.mongo.{CisCYAModel, CisUserData, DatabaseError}
import models.{HttpParserError, IncomeTaxUserData, ServiceError, User}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatest.TestSuite
import services.CISSessionService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Month
import scala.concurrent.Future

trait MockCISSessionService { this: TestSuite =>


  protected val mockCISSessionService: CISSessionService =
    org.mockito.Mockito.mock(classOf[CISSessionService])

  def mockGetPriorData(taxYear: Int,
                       user: User,
                       result: Either[HttpParserError, IncomeTaxUserData]
                      ): Unit = {
    when(
      mockCISSessionService.getPriorData(
        eqTo(user),
        eqTo(taxYear)
      )(any[HeaderCarrier]())
    ).thenReturn(Future.successful(result))
  }

  def mockRefreshAndClear(taxYear: Int, employerRef: String,
                          result: Either[ServiceError, Unit]): Unit = {
    when(
      mockCISSessionService.refreshAndClear(
        any[User](),
        eqTo(employerRef),
        eqTo(taxYear),
        any[Boolean]()
      )(any[HeaderCarrier]())
    ).thenReturn(Future.successful(result))
  }

  def mockGetSessionData(taxYear: Int,
                         user: User,
                         employerRef: String,
                         result: Either[DatabaseError, Option[CisUserData]]
                        ): Unit = {
    when(
      mockCISSessionService.getSessionData(
        eqTo(taxYear),
        eqTo(employerRef),
        eqTo(user)
      )
    ).thenReturn(Future.successful(result))
  }

  def mockCreateOrUpdateCISUserData(taxYear: Int,
                                    user: User,
                                    employerRef: String,
                                    submissionId: Option[String],
                                    isPriorSubmission: Boolean,
                                    cisCYAModel: CisCYAModel,
                                    result: Either[DatabaseError, CisUserData]
                                   ): Unit = {
    when(
      mockCISSessionService.createOrUpdateCISUserData(
        eqTo(user),
        eqTo(taxYear),
        eqTo(employerRef),
        eqTo(submissionId),
        eqTo(isPriorSubmission),
        eqTo(cisCYAModel)
      )
    ).thenReturn(Future.successful(result))
  }

  def mockCheckCyaAndReturnData(taxYear: Int,
                                employerRef: String,
                                month: Month,
                                result: Either[ServiceError, Option[CisUserData]]
                               ): Unit = {
    when(
      mockCISSessionService.checkCyaAndReturnData(
        eqTo(taxYear),
        eqTo(employerRef),
        any[User](),
        eqTo(month)
      )(any[HeaderCarrier]())
    ).thenReturn(Future.successful(result))
  }
}
