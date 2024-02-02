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

package controllers

import actions.ActionsProvider
import common.SessionValues
import config.{AppConfig, ErrorHandler}
import controllers.routes.{ContractorCYAController, ContractorSummaryController}
import models.pages.ContractorCYAPage._
import models.{HttpParserError, InvalidOrUnfinishedSubmission}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ContractorCYAService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.ContractorCYAView

import java.time.Month
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ContractorCYAController @Inject()(actionsProvider: ActionsProvider,
                                        pageView: ContractorCYAView,
                                        inYearUtil: InYearUtil,
                                        contractorCYAService: ContractorCYAService,
                                        errorHandler: ErrorHandler)
                                       (implicit mcc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int,
           month: String,
           contractor: String): Action[AnyContent] =
    if (inYearUtil.inYear(taxYear)) inYear(taxYear, month, contractor) else endOfYear(taxYear, month, contractor)

  def submit(taxYear: Int, month: String, contractor: String): Action[AnyContent] =
    actionsProvider.checkCyaExistsAndReturnSessionData(taxYear, contractor, month).async { implicit request =>
      if (request.cisUserData.cis.periodDataUpdated) {
        contractorCYAService.submitCisDeductionCYA(taxYear, contractor, request.user, request.cisUserData).map {
          case Left(HttpParserError(status)) => errorHandler.handleError(status)
          case Left(InvalidOrUnfinishedSubmission) => Redirect(ContractorCYAController.show(taxYear, month, contractor))
          case Left(_) => errorHandler.internalServerError()
          case Right(_) => Redirect(ContractorSummaryController.show(taxYear, contractor)).removingFromSession(SessionValues.TEMP_EMPLOYER_REF)
        }
      } else {
        Future.successful(Redirect(ContractorSummaryController.show(taxYear, contractor)).removingFromSession(SessionValues.TEMP_EMPLOYER_REF))
      }
    }

  private def inYear(taxYear: Int,
                     month: String,
                     contractor: String): Action[AnyContent] = actionsProvider.userPriorDataFor(taxYear, contractor, month) { implicit request =>
    val data = request.incomeTaxUserData.inYearCisDeductionsWith(contractor).get
    val pageModel = inYearMapToPageModel(taxYear, data, Month.valueOf(month.toUpperCase), request.user.isAgent)

    Ok(pageView(pageModel))
  }

  private def endOfYear(taxYear: Int,
                        month: String,
                        contractor: String): Action[AnyContent] =
    actionsProvider.checkCyaExistsAndReturnSessionData(taxYear, contractor, month) { implicit request =>
      val pageModel = eoyMapToPageModel(taxYear, request.cisUserData, request.user.isAgent)
      Ok(pageView(pageModel))
    }
}
