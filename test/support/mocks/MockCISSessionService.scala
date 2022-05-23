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

import java.time.Month

import models.mongo.{CisCYAModel, CisUserData, DatabaseError}
import models.{HttpParserError, IncomeTaxUserData, ServiceError, User}
import org.scalamock.handlers.{CallHandler3, CallHandler4, CallHandler6}
import org.scalamock.scalatest.MockFactory
import play.api.mvc.Request
import services.CISSessionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockCISSessionService extends MockFactory {

  protected val mockCISSessionService: CISSessionService = mock[CISSessionService]

  def mockGetPriorData(taxYear: Int,
                       user: User,
                       result: Either[HttpParserError, IncomeTaxUserData]
                      ): CallHandler3[User, Int, HeaderCarrier, Future[Either[HttpParserError, IncomeTaxUserData]]] = {
    (mockCISSessionService.getPriorData(_: User, _: Int)(_: HeaderCarrier))
      .expects(user, taxYear, *)
      .returns(Future.successful(result))
  }

  def mockClear(taxYear: Int, employerRef: String,
                result: Either[ServiceError,Unit]): CallHandler3[User, String, Int, Future[Either[ServiceError, Unit]]] = {
    (mockCISSessionService.clear( _: User, _: String, _: Int))
      .expects(*, employerRef, taxYear)
      .returning(Future.successful(result))
  }

  def mockGetSessionData(taxYear: Int,
                         user: User,
                         employerRef: String,
                         result: Either[DatabaseError, Option[CisUserData]]
                        ): CallHandler4[Int, String, User, Option[String], Future[Either[DatabaseError, Option[CisUserData]]]] = {
    (mockCISSessionService.getSessionData(_: Int, _: String, _: User, _: Option[String]))
      .expects(taxYear, employerRef, user, *)
      .returns(Future.successful(result))
  }

  def mockCreateOrUpdateCISUserData(taxYear: Int,
                                    user: User,
                                    employerRef: String,
                                    submissionId: Option[String],
                                    isPriorSubmission: Boolean,
                                    cisCYAModel: CisCYAModel,
                                    result: Either[DatabaseError, CisUserData]
                                   ): CallHandler6[User, Int, String, Option[String], Boolean, CisCYAModel, Future[Either[DatabaseError, CisUserData]]] = {
    (mockCISSessionService.createOrUpdateCISUserData(_: User, _: Int, _: String, _: Option[String], _: Boolean, _: CisCYAModel))
      .expects(user, taxYear, employerRef, submissionId, isPriorSubmission, cisCYAModel)
      .returns(Future.successful(result))
  }

  def mockCheckCyaAndReturnData(taxYear: Int,
                                employerRef: String,
                                month: Month,
                                result: Either[ServiceError, Option[CisUserData]]
                               ): CallHandler6[Int, String, User, Month, Option[String], HeaderCarrier, Future[Either[ServiceError, Option[CisUserData]]]] = {
    (mockCISSessionService.checkCyaAndReturnData(_: Int, _: String, _: User, _: Month, _: Option[String])(_: HeaderCarrier))
      .expects(taxYear, employerRef, *, month, *, *)
      .returns(Future.successful(result))
  }
}
