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
import play.api.mvc._
import services.CISSessionService
import utils.InYearUtil

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ActionsProvider @Inject()(authAction: AuthorisedAction,
                                cisSessionService: CISSessionService,
                                errorHandler: ErrorHandler,
                                inYearUtil: InYearUtil,
                                appConfig: AppConfig
                               )(implicit ec: ExecutionContext) {

  def inYear(taxYear: Int): ActionBuilder[AuthorisationRequest, AnyContent] =
    authAction
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(InYearFilterAction(taxYear, inYearUtil, appConfig))

  def notInYear(taxYear: Int): ActionBuilder[AuthorisationRequest, AnyContent] =
    authAction
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(EndOfYearFilterAction(taxYear, inYearUtil, appConfig))

  def inYearWithPreviousDataFor(taxYear: Int, contractor: String): ActionBuilder[UserPriorDataRequest, AnyContent] =
    authAction
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(InYearFilterAction(taxYear, inYearUtil, appConfig))
      .andThen(UserPriorDataRefinerAction(taxYear, cisSessionService, errorHandler))
      .andThen(HasInYearPeriodDataWithEmployerRefFilterAction(taxYear, contractor, appConfig))

  def inYearWithPreviousDataFor(taxYear: Int, month: String, contractor: String): ActionBuilder[UserPriorDataRequest, AnyContent] =
    authAction
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(InYearFilterAction(taxYear, inYearUtil, appConfig))
      .andThen(UserPriorDataRefinerAction(taxYear, cisSessionService, errorHandler))
      .andThen(HasInYearDeductionsForEmployerRefAndMonthFilterAction(taxYear, contractor, month, errorHandler, appConfig))

  def priorCisDeductionsData(taxYear: Int): ActionBuilder[UserPriorDataRequest, AnyContent] = {
    if (inYearUtil.inYear(taxYear)) {
      priorDataWithInYearCisDeductions(taxYear)
    } else {
      priorDataWithEndOfYearCisDeductions(taxYear)
    }
  }

  def userPriorDataFor(taxYear: Int, contractor: String): ActionBuilder[UserPriorDataRequest, AnyContent] = {
    authAction
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(UserPriorDataRefinerAction(taxYear, cisSessionService, errorHandler))
      .andThen(getHasPeriodDataFilterActionFor(taxYear, contractor))
  }

  def userPriorDataFor(taxYear: Int, contractor: String, month: String): ActionBuilder[UserPriorDataRequest, AnyContent] = {
    authAction
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(UserPriorDataRefinerAction(taxYear, cisSessionService, errorHandler))
      .andThen(getHasMonthDataFilterActionFor(taxYear, contractor, month))
  }

  def endOfYearWithSessionData(taxYear: Int, contractor: String, needsPeriodData: Boolean = true): ActionBuilder[UserSessionDataRequest, AnyContent] =
    authAction
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(EndOfYearFilterAction(taxYear, inYearUtil, appConfig))
      .andThen(CisUserDataRefinerAction(taxYear, contractor, cisSessionService, errorHandler, appConfig, needsPeriodData = needsPeriodData))

  def endOfYearWithSessionData(taxYear: Int, month: String, contractor: String): ActionBuilder[UserSessionDataRequest, AnyContent] =
    authAction
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(MonthFilterAction(month, errorHandler))
      .andThen(EndOfYearFilterAction(taxYear, inYearUtil, appConfig))
      .andThen(CisUserDataRefinerAction(taxYear, contractor, cisSessionService, errorHandler, appConfig))

  def checkCyaExistsAndReturnSessionData(taxYear: Int, contractor: String, month: String): ActionBuilder[UserSessionDataRequest, AnyContent] = {
    authAction
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(OptionalCisCyaRefinerAction(taxYear, contractor, month, cisSessionService, errorHandler, appConfig))
  }

  private def priorDataWithInYearCisDeductions(taxYear: Int): ActionBuilder[UserPriorDataRequest, AnyContent] =
    authAction
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(InYearFilterAction(taxYear, inYearUtil, appConfig))
      .andThen(UserPriorDataRefinerAction(taxYear, cisSessionService, errorHandler))
      .andThen(HasInYearCisDeductionsFilterAction(taxYear, appConfig))

  private def priorDataWithEndOfYearCisDeductions(taxYear: Int): ActionBuilder[UserPriorDataRequest, AnyContent] =
    authAction
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(EndOfYearFilterAction(taxYear, inYearUtil, appConfig))
      .andThen(UserPriorDataRefinerAction(taxYear, cisSessionService, errorHandler))

  private def getHasPeriodDataFilterActionFor(taxYear: Int, contractor: String): ActionFilter[UserPriorDataRequest] = {
    if (inYearUtil.inYear(taxYear)) {
      HasInYearPeriodDataWithEmployerRefFilterAction(taxYear, contractor, appConfig)
    } else {
      HasEOYDataWithEmployerRefFilterAction(taxYear, contractor, appConfig)
    }
  }

  private def getHasMonthDataFilterActionFor(taxYear: Int, contractor: String, month: String): ActionFilter[UserPriorDataRequest] = {
    if (inYearUtil.inYear(taxYear)) {
      HasInYearDeductionsForEmployerRefAndMonthFilterAction(taxYear, contractor, month, errorHandler, appConfig)
    } else {
      HasEndOfYearDeductionsForEmployerRefAndMonthFilterAction(taxYear, contractor, month, errorHandler, appConfig)
    }
  }
}