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

package actions

import config.{AppConfig, ErrorHandler}
import models._
import play.api.mvc.Results.Redirect
import play.api.mvc._
import services.CISSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import utils.{InYearUtil, UrlUtils}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ActionsProvider @Inject()(val authAction: AuthorisedAction,
                                cisSessionService: CISSessionService,
                                errorHandler: ErrorHandler,
                                inYearUtil: InYearUtil,
                                appConfig: AppConfig
                               )(implicit ec: ExecutionContext) extends FrontendHeaderCarrierProvider {

  def inYear(taxYear: Int): ActionBuilder[AuthorisationRequest, AnyContent] = authAction.andThen(inYearActionBuilder(taxYear))

  def notInYear(taxYear: Int): ActionBuilder[AuthorisationRequest, AnyContent] = authAction.andThen(notInYearActionBuilder(taxYear))

  def inYearWithPreviousDataFor(taxYear: Int, month: String, contractor: String): ActionBuilder[UserPriorDataRequest, AnyContent] =
    authAction
      .andThen(inYearActionBuilder(taxYear))
      .andThen(incomeTaxUserDataAction(taxYear))
      .andThen(new HasInYearDeductionsForEmployerRefAndMonthActionFilter(taxYear, UrlUtils.decode(contractor), month, errorHandler, appConfig))

  def inYearWithPreviousDataFor(taxYear: Int, contractor: String): ActionBuilder[UserPriorDataRequest, AnyContent] =
    authAction
      .andThen(inYearActionBuilder(taxYear))
      .andThen(incomeTaxUserDataAction(taxYear))
      .andThen(new HasInYearPeriodDataWithEmployerRefActionFilter(taxYear, UrlUtils.decode(contractor), appConfig))

  def priorDataWithInYearCisDeductions(taxYear: Int): ActionBuilder[UserPriorDataRequest, AnyContent] =
    authAction
      .andThen(inYearActionBuilder(taxYear))
      .andThen(incomeTaxUserDataAction(taxYear))
      .andThen(new HasInYearCisDeductionsActionFilter(taxYear, appConfig))

  def notInYearWithSessionData(taxYear: Int, contractor: String): ActionBuilder[UserSessionDataRequest, AnyContent] =
    authAction
      .andThen(notInYearActionBuilder(taxYear))
      .andThen(new CisUserDataActionRefiner(taxYear, UrlUtils.decode(contractor), cisSessionService, errorHandler, appConfig))

  private def notInYearActionBuilder(taxYear: Int): ActionFilter[AuthorisationRequest] = new ActionFilter[AuthorisationRequest] {
    override protected def executionContext: ExecutionContext = ec

    override protected def filter[A](request: AuthorisationRequest[A]): Future[Option[Result]] = Future.successful {
      if (inYearUtil.inYear(taxYear)) {
        Some(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      } else {
        None
      }
    }
  }

  private def inYearActionBuilder(taxYear: Int): ActionFilter[AuthorisationRequest] = new ActionFilter[AuthorisationRequest] {
    override protected def executionContext: ExecutionContext = ec

    override protected def filter[A](request: AuthorisationRequest[A]): Future[Option[Result]] = Future.successful {
      if (!inYearUtil.inYear(taxYear)) {
        Some(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      } else {
        None
      }
    }
  }

  // TODO: (Hristo) Make private once DeductionsSummaryController is refactored to deal with in year and eoy
  def incomeTaxUserDataAction(taxYear: Int): ActionRefiner[AuthorisationRequest, UserPriorDataRequest] =
    new ActionRefiner[AuthorisationRequest, UserPriorDataRequest] {
      override protected def executionContext: ExecutionContext = ec

      override protected def refine[A](input: AuthorisationRequest[A]): Future[Either[Result, UserPriorDataRequest[A]]] = {
        cisSessionService.getPriorData(input.user, taxYear)(hc(input.request)).map {
          case Left(error) => Left(errorHandler.handleError(error.status)(input.request))
          case Right(incomeTaxUserData) => Right(UserPriorDataRequest(incomeTaxUserData, input.user, input.request))
        }
      }
    }
}