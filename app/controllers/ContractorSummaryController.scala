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

package controllers

import actions.ActionsProvider
import config.{AppConfig, ErrorHandler}
import controllers.routes.DeductionPeriodController
import models._
import models.pages.ContractorSummaryPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ContractorSummaryService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.ContractorSummaryViewValues

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ContractorSummaryController @Inject()(actionsProvider: ActionsProvider,
                                            pageView: ContractorSummaryViewValues,
                                            inYearUtil: InYearUtil,
                                            contractorSummaryService: ContractorSummaryService,
                                            errorHandler: ErrorHandler)
                                           (implicit mcc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, contractor: String): Action[AnyContent] = actionsProvider.userPriorDataFor(taxYear, contractor) { implicit request =>
    Ok(pageView(ContractorSummaryPage(taxYear, inYearUtil.inYear(taxYear), contractor, request.incomeTaxUserData)))
  }

  def showValues(taxYear: Int, contractor: String): Action[AnyContent] = actionsProvider.endOfYear(taxYear) { implicit request =>
    val anIncomeTaxUserData: IncomeTaxUserData = IncomeTaxUserData(cis = None)
    Ok(pageView(ContractorSummaryPage(taxYear, inYearUtil.inYear(taxYear), contractor, anIncomeTaxUserData)))
  }

  def addCisDeduction(taxYear: Int, contractor: String): Action[AnyContent] = actionsProvider.userPriorDataFor(taxYear, contractor).async { implicit request =>
    contractorSummaryService.saveCYAForNewCisDeduction(taxYear, contractor, request.incomeTaxUserData, request.user).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(_) => Redirect(DeductionPeriodController.show(taxYear, contractor))
    }
  }
}
