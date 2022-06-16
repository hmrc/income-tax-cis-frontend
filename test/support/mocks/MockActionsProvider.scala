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

import actions.ActionsProvider
import models.mongo.CisUserData
import models.{AuthorisationRequest, IncomeTaxUserData, UserPriorDataRequest, UserSessionDataRequest}
import org.scalamock.handlers.{CallHandler1, CallHandler2, CallHandler3}
import org.scalamock.scalatest.MockFactory
import play.api.mvc._
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData

import scala.concurrent.{ExecutionContext, Future}

trait MockActionsProvider extends MockFactory
  with MockAuthorisedAction
  with MockCISSessionService
  with MockErrorHandler {

  protected val mockActionsProvider: ActionsProvider = mock[ActionsProvider]

  private def userPriorDataRequestActionBuilder(incomeTaxUserData: IncomeTaxUserData): ActionBuilder[UserPriorDataRequest, AnyContent] =
    new ActionBuilder[UserPriorDataRequest, AnyContent] {
      override def parser: BodyParser[AnyContent] = BodyParser("anyContent")(_ => throw new NotImplementedError)

      override def invokeBlock[A](request: Request[A], block: UserPriorDataRequest[A] => Future[Result]): Future[Result] =
        block(UserPriorDataRequest(incomeTaxUserData, aUser, request))

      override protected def executionContext: ExecutionContext = ExecutionContext.Implicits.global
    }

  private def userSessionDataRequestActionBuilder(userData: CisUserData): ActionBuilder[UserSessionDataRequest, AnyContent] =
    new ActionBuilder[UserSessionDataRequest, AnyContent] {
      override def parser: BodyParser[AnyContent] = BodyParser("anyContent")(_ => throw new NotImplementedError)

      override def invokeBlock[A](request: Request[A], block: UserSessionDataRequest[A] => Future[Result]): Future[Result] =
        block(UserSessionDataRequest(userData, aUser, request))

      override protected def executionContext: ExecutionContext = ExecutionContext.Implicits.global
    }

  private def authorisationRequestActionBuilder: ActionBuilder[AuthorisationRequest, AnyContent] =
    new ActionBuilder[AuthorisationRequest, AnyContent] {
      override def parser: BodyParser[AnyContent] = BodyParser("anyContent")(_ => throw new NotImplementedError)

      override def invokeBlock[A](request: Request[A], block: AuthorisationRequest[A] => Future[Result]): Future[Result] =
        block(AuthorisationRequest(aUser, request))

      override protected def executionContext: ExecutionContext = ExecutionContext.Implicits.global
    }

  def mockEndOfYearWithSessionData(taxYear: Int,
                                   cisUserData: CisUserData): CallHandler3[Int, String, Boolean, ActionBuilder[UserSessionDataRequest, AnyContent]] = {
    val actionBuilder: ActionBuilder[UserSessionDataRequest, AnyContent] = new ActionBuilder[UserSessionDataRequest, AnyContent] {
      override def parser: BodyParser[AnyContent] = BodyParser("anyContent")(_ => throw new NotImplementedError)

      override def invokeBlock[A](request: Request[A], block: UserSessionDataRequest[A] => Future[Result]): Future[Result] =
        block(UserSessionDataRequest(cisUserData, aUser, request))

      override protected def executionContext: ExecutionContext = ExecutionContext.Implicits.global
    }

    (mockActionsProvider.endOfYearWithSessionData(_: Int, _: String, _: Boolean))
      .expects(taxYear, cisUserData.employerRef, *)
      .returns(value = actionBuilder)
  }

  def mockEndOfYearWithSessionData(taxYear: Int,
                                   month: String,
                                   employerRef: String): CallHandler3[Int, String, String, ActionBuilder[UserSessionDataRequest, AnyContent]] = {
    val actionBuilder: ActionBuilder[UserSessionDataRequest, AnyContent] = new ActionBuilder[UserSessionDataRequest, AnyContent] {
      override def parser: BodyParser[AnyContent] = BodyParser("anyContent")(_ => throw new NotImplementedError)

      override def invokeBlock[A](request: Request[A], block: UserSessionDataRequest[A] => Future[Result]): Future[Result] =
        block(UserSessionDataRequest(aCisUserData.copy(employerRef = employerRef), aUser, request))

      override protected def executionContext: ExecutionContext = ExecutionContext.Implicits.global
    }

    (mockActionsProvider.endOfYearWithSessionData(_: Int, _: String, _: String))
      .expects(taxYear, month, employerRef)
      .returns(value = actionBuilder)
  }

  def mockEndOfYearWithSessionDataWithCustomerDeductionPeriod(taxYear: Int,
                                                              cisUserData: CisUserData,
                                                              month: Option[String] = None): CallHandler3[Int, String, Option[String], ActionBuilder[UserSessionDataRequest, AnyContent]] = {
    val actionBuilder: ActionBuilder[UserSessionDataRequest, AnyContent] = new ActionBuilder[UserSessionDataRequest, AnyContent] {
      override def parser: BodyParser[AnyContent] = BodyParser("anyContent")(_ => throw new NotImplementedError)

      override def invokeBlock[A](request: Request[A], block: UserSessionDataRequest[A] => Future[Result]): Future[Result] =
        block(UserSessionDataRequest(cisUserData, aUser, request))

      override protected def executionContext: ExecutionContext = ExecutionContext.Implicits.global
    }

    (mockActionsProvider.endOfYearWithSessionDataWithCustomerDeductionPeriod(_: Int, _: String, _: Option[String]))
      .expects(taxYear, cisUserData.employerRef, month)
      .returns(value = actionBuilder)
  }

  def mockEndOfYearWithSessionData(taxYear: Int,
                                   month: String,
                                   cisUserData: CisUserData): CallHandler3[Int, String, String, ActionBuilder[UserSessionDataRequest, AnyContent]] = {
    val actionBuilder: ActionBuilder[UserSessionDataRequest, AnyContent] = new ActionBuilder[UserSessionDataRequest, AnyContent] {
      override def parser: BodyParser[AnyContent] = BodyParser("anyContent")(_ => throw new NotImplementedError)

      override def invokeBlock[A](request: Request[A], block: UserSessionDataRequest[A] => Future[Result]): Future[Result] =
        block(UserSessionDataRequest(cisUserData, aUser, request))

      override protected def executionContext: ExecutionContext = ExecutionContext.Implicits.global
    }

    (mockActionsProvider.endOfYearWithSessionData(_: Int, _: String, _: String))
      .expects(taxYear, month, cisUserData.employerRef)
      .returns(value = actionBuilder)
  }

  def mockPriorCisDeductionsData(taxYear: Int,
                                 result: IncomeTaxUserData): CallHandler1[Int, ActionBuilder[UserPriorDataRequest, AnyContent]] = {
    (mockActionsProvider.priorCisDeductionsData(_: Int))
      .expects(taxYear)
      .returns(value = userPriorDataRequestActionBuilder(result))
  }

  def mockInYearWithPreviousDataFor(taxYear: Int,
                                    month: String,
                                    contractor: String,
                                    result: IncomeTaxUserData
                                   ): CallHandler3[Int, String, String, ActionBuilder[UserPriorDataRequest, AnyContent]] = {
    (mockActionsProvider.inYearWithPreviousDataFor(_: Int, _: String, _: String))
      .expects(taxYear, month, contractor)
      .returns(value = userPriorDataRequestActionBuilder(result))
  }

  def mockUserPriorDataFor(taxYear: Int,
                           contractor: String,
                           result: IncomeTaxUserData
                          ): CallHandler2[Int, String, ActionBuilder[UserPriorDataRequest, AnyContent]] = {
    (mockActionsProvider.userPriorDataFor(_: Int, _: String))
      .expects(taxYear, contractor)
      .returns(value = userPriorDataRequestActionBuilder(result))
  }

  def mockUserPriorDataFor(taxYear: Int,
                           contractor: String,
                           month: String,
                           result: IncomeTaxUserData
                          ): CallHandler3[Int, String, String, ActionBuilder[UserPriorDataRequest, AnyContent]] = {
    (mockActionsProvider.userPriorDataFor(_: Int, _: String, _: String))
      .expects(taxYear, contractor, month)
      .returns(value = userPriorDataRequestActionBuilder(result))
  }

  def mockCheckCyaExistsAndReturnSessionData(taxYear: Int,
                                             contractor: String,
                                             month: String,
                                             result: CisUserData
                                            ): CallHandler3[Int, String, String, ActionBuilder[UserSessionDataRequest, AnyContent]] = {
    (mockActionsProvider.checkCyaExistsAndReturnSessionData(_: Int, _: String, _: String))
      .expects(taxYear, contractor, month)
      .returns(value = userSessionDataRequestActionBuilder(result))
  }

  def mockExclusivelyCustomerPriorDataForEOY(taxYear: Int,
                                             contractor: String,
                                             month: String,
                                             result: IncomeTaxUserData
                             ): CallHandler3[Int, String, String, ActionBuilder[UserPriorDataRequest, AnyContent]] = {
    (mockActionsProvider.exclusivelyCustomerPriorDataForEOY(_: Int, _: String, _: String))
      .expects(taxYear, contractor, month)
      .returns(value = userPriorDataRequestActionBuilder(result))
  }

  def mockNotInYear(taxYear: Int): CallHandler1[Int, ActionBuilder[AuthorisationRequest, AnyContent]] = {
    (mockActionsProvider.endOfYear(_: Int))
      .expects(taxYear)
      .returns(value = authorisationRequestActionBuilder)
  }
}
