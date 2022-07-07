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

import audit.AuditService
import config.{AppConfig, ErrorHandler}
import models._
import play.api.mvc._
import services.CISSessionService
import utils.InYearUtil

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ActionsProvider @Inject()(authAction: AuthorisedAction,
                                cisSessionService: CISSessionService,
                                auditService: AuditService,
                                errorHandler: ErrorHandler,
                                inYearUtil: InYearUtil,
                                appConfig: AppConfig
                               )(implicit ec: ExecutionContext) {

  def endOfYear(taxYear: Int): ActionBuilder[AuthorisationRequest, AnyContent] =
    authAction
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(EndOfYearFilterAction(taxYear, inYearUtil, appConfig))

  def tailoringEnabledFilterWithEndOfYear(taxYear: Int): ActionBuilder[AuthorisationRequest, AnyContent] =
    authAction
      .andThen(TailoringEnabledFilterAction(taxYear, appConfig))

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
    authAction
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(UserPriorDataRefinerAction(taxYear, cisSessionService, errorHandler))
  }

  def exclusivelyCustomerPriorDataForEOY(taxYear: Int, contractor: String, month: String): ActionBuilder[UserPriorDataRequest, AnyContent] = {
    authAction
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(EndOfYearFilterAction(taxYear, inYearUtil, appConfig))
      .andThen(UserPriorDataRefinerAction(taxYear, cisSessionService, errorHandler))
      .andThen(HasEoyDeductionsForEmployerRefAndMonthFilterAction(taxYear, contractor, month, errorHandler, appConfig, needsToBeExclusivelyCustomerData = true))
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
      .andThen(InYearViewCisPeriodAuditAction(taxYear, contractor, month, auditService))
  }

  def endOfYearWithSessionData(taxYear: Int, contractor: String, redirectIfPrior: Boolean): ActionBuilder[UserSessionDataRequest, AnyContent] =
    authAction
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(EndOfYearFilterAction(taxYear, inYearUtil, appConfig))
      .andThen(CisUserDataRefinerAction(taxYear, contractor, cisSessionService, errorHandler, appConfig))
      .andThen(CisUserDataFilterAction(taxYear, contractor, appConfig, needsPeriodData = true, redirectIfPrior))

  def endOfYearWithSessionData(taxYear: Int, month: String, contractor: String): ActionBuilder[UserSessionDataRequest, AnyContent] =
    authAction
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(MonthFilterAction(month, errorHandler))
      .andThen(EndOfYearFilterAction(taxYear, inYearUtil, appConfig))
      .andThen(CisUserDataRefinerAction(taxYear, contractor, cisSessionService, errorHandler, appConfig))
      .andThen(CisUserDataFilterAction(taxYear, contractor, appConfig, needsPeriodData = true, redirectIfPrior = false))

  def endOfYearWithSessionDataWithCustomerDeductionPeriod(taxYear: Int,
                                                          contractor: String,
                                                          month: Option[String] = None): ActionBuilder[UserSessionDataRequest, AnyContent] =
    month.map(monthValue => authAction.andThen(MonthFilterAction(monthValue, errorHandler))).getOrElse(authAction)
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(EndOfYearFilterAction(taxYear, inYearUtil, appConfig))
      .andThen(CisUserDataRefinerAction(taxYear, contractor, cisSessionService, errorHandler, appConfig))
      .andThen(CisUserDataFilterAction(taxYear, contractor, appConfig, needsPeriodData = false, redirectIfPrior = false))
      .andThen(CustomerDeductionPeriodFilterAction(taxYear, appConfig))

  def checkCyaExistsAndReturnSessionData(taxYear: Int, contractor: String, month: String): ActionBuilder[UserSessionDataRequest, AnyContent] = {
    authAction
      .andThen(TaxYearAction.taxYearAction(taxYear)(appConfig))
      .andThen(OptionalCisCyaRefinerAction(taxYear, contractor, month, cisSessionService, errorHandler, appConfig))
      .andThen(EndOfYearViewCisPeriodAuditAction(taxYear, auditService))
  }

  private def getHasPeriodDataFilterActionFor(taxYear: Int, contractor: String): ActionFilter[UserPriorDataRequest] = {
    if (inYearUtil.inYear(taxYear)) {
      HasInYearPeriodDataWithEmployerRefFilterAction(taxYear, contractor, appConfig)
    } else {
      HasEoyDeductionsForEmployerRefFilterAction(taxYear, contractor, appConfig)
    }
  }

  private def getHasMonthDataFilterActionFor(taxYear: Int, contractor: String, month: String): ActionFilter[UserPriorDataRequest] = {
    if (inYearUtil.inYear(taxYear)) {
      HasInYearDeductionsForEmployerRefAndMonthFilterAction(taxYear, contractor, month, errorHandler, appConfig)
    } else {
      HasEoyDeductionsForEmployerRefAndMonthFilterAction(taxYear, contractor, month, errorHandler, appConfig)
    }
  }
}
