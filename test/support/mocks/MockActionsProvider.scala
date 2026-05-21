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

import actions.ActionsProvider
import models.mongo.CisUserData
import models.{AuthorisationRequest, IncomeTaxUserData, UserPriorDataRequest, UserSessionDataRequest}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import play.api.mvc._
import support.UnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData

import scala.concurrent.{ExecutionContext, Future}

trait MockActionsProvider extends MockAuthorisedAction with MockCISSessionService with MockErrorHandler { this: UnitTest =>

  protected val mockActionsProvider: ActionsProvider =
    org.mockito.Mockito.mock(classOf[ActionsProvider])

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
                                   cisUserData: CisUserData): Unit = {
    val actionBuilder: ActionBuilder[UserSessionDataRequest, AnyContent] = new ActionBuilder[UserSessionDataRequest, AnyContent] {
      override def parser: BodyParser[AnyContent] = BodyParser("anyContent")(_ => throw new NotImplementedError)

      override def invokeBlock[A](request: Request[A], block: UserSessionDataRequest[A] => Future[Result]): Future[Result] =
        block(UserSessionDataRequest(cisUserData, aUser, request))

      override protected def executionContext: ExecutionContext = ExecutionContext.Implicits.global
    }

    when(
      mockActionsProvider.endOfYearWithSessionData(eqTo(taxYear), eqTo(cisUserData.employerRef), any[Boolean]())
    ).thenReturn(actionBuilder)
  }

  def mockEndOfYearWithSessionData(taxYear: Int,
                                   month: String,
                                   employerRef: String): Unit = {
    val actionBuilder: ActionBuilder[UserSessionDataRequest, AnyContent] = new ActionBuilder[UserSessionDataRequest, AnyContent] {
      override def parser: BodyParser[AnyContent] = BodyParser("anyContent")(_ => throw new NotImplementedError)

      override def invokeBlock[A](request: Request[A], block: UserSessionDataRequest[A] => Future[Result]): Future[Result] =
        block(UserSessionDataRequest(aCisUserData.copy(employerRef = employerRef), aUser, request))

      override protected def executionContext: ExecutionContext = ExecutionContext.Implicits.global
    }

    when(
      mockActionsProvider.endOfYearWithSessionData(eqTo(taxYear), eqTo(month), eqTo(employerRef))
    ).thenReturn(actionBuilder)
  }

  def mockEndOfYearWithSessionDataWithCustomerDeductionPeriod(taxYear: Int,
                                                              cisUserData: CisUserData,
                                                              month: Option[String] = None): Unit = {
    val actionBuilder: ActionBuilder[UserSessionDataRequest, AnyContent] = new ActionBuilder[UserSessionDataRequest, AnyContent] {
      override def parser: BodyParser[AnyContent] = BodyParser("anyContent")(_ => throw new NotImplementedError)

      override def invokeBlock[A](request: Request[A], block: UserSessionDataRequest[A] => Future[Result]): Future[Result] =
        block(UserSessionDataRequest(cisUserData, aUser, request))

      override protected def executionContext: ExecutionContext = ExecutionContext.Implicits.global
    }

    when(
      mockActionsProvider.endOfYearWithSessionDataWithCustomerDeductionPeriod(eqTo(taxYear), eqTo(cisUserData.employerRef), any[Option[String]]())
    ).thenReturn(actionBuilder)
  }

  def mockEndOfYearWithSessionData(taxYear: Int,
                                   month: String,
                                   cisUserData: CisUserData): Unit = {
    val actionBuilder: ActionBuilder[UserSessionDataRequest, AnyContent] = new ActionBuilder[UserSessionDataRequest, AnyContent] {
      override def parser: BodyParser[AnyContent] = BodyParser("anyContent")(_ => throw new NotImplementedError)

      override def invokeBlock[A](request: Request[A], block: UserSessionDataRequest[A] => Future[Result]): Future[Result] =
        block(UserSessionDataRequest(cisUserData, aUser, request))

      override protected def executionContext: ExecutionContext = ExecutionContext.Implicits.global
    }

    when(
      mockActionsProvider.endOfYearWithSessionData(eqTo(taxYear), eqTo(month), eqTo(cisUserData.employerRef))
    ).thenReturn(actionBuilder)
  }

  def mockPriorCisDeductionsData(taxYear: Int,
                                 result: IncomeTaxUserData): Unit = {
    when(
      mockActionsProvider.priorCisDeductionsData(eqTo(taxYear))
    ).thenReturn(userPriorDataRequestActionBuilder(result))
  }

  def mockInYearWithPreviousDataFor(taxYear: Int,
                                    month: String,
                                    contractor: String,
                                    result: IncomeTaxUserData
                                   ): Unit = {
    when(
      mockActionsProvider.inYearWithPreviousDataFor(eqTo(taxYear), eqTo(month), eqTo(contractor))
    ).thenReturn(userPriorDataRequestActionBuilder(result))
  }

  def mockUserPriorDataFor(taxYear: Int,
                           contractor: String,
                           result: IncomeTaxUserData
                          ): Unit = {
    when(
      mockActionsProvider.userPriorDataFor(eqTo(taxYear), eqTo(contractor))
    ).thenReturn(userPriorDataRequestActionBuilder(result))
  }

  def mockUserPriorDataFor(taxYear: Int,
                           contractor: String,
                           month: String,
                           result: IncomeTaxUserData
                          ): Unit = {
    when(
      mockActionsProvider.userPriorDataFor(eqTo(taxYear), eqTo(contractor), eqTo(month))
    ).thenReturn(userPriorDataRequestActionBuilder(result))
  }

  def mockCheckCyaExistsAndReturnSessionData(taxYear: Int,
                                             contractor: String,
                                             month: String,
                                             result: CisUserData
                                            ): Unit = {
    when(
      mockActionsProvider.checkCyaExistsAndReturnSessionData(eqTo(taxYear), eqTo(contractor), eqTo(month))
    ).thenReturn(userSessionDataRequestActionBuilder(result))
  }

  def mockExclusivelyCustomerPriorDataForEOY(taxYear: Int,
                                             contractor: String,
                                             month: String,
                                             result: IncomeTaxUserData
                             ): Unit = {
    when(
      mockActionsProvider.exclusivelyCustomerPriorDataForEOY(eqTo(taxYear), eqTo(contractor), eqTo(month))
    ).thenReturn(userPriorDataRequestActionBuilder(result))
  }

  def mockNotInYear(taxYear: Int): Unit = {
    when(
      mockActionsProvider.endOfYear(eqTo(taxYear))
    ).thenReturn(authorisationRequestActionBuilder)
  }
}
