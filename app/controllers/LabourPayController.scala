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
import config.{AppConfig, ErrorHandler}
import controllers.routes.{ContractorCYAController, DeductionAmountController}
import forms.FormsProvider
import models.mongo.CisUserData
import models.pages.LabourPayPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.LabourPayService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.LabourPayView

import java.time.Month
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LabourPayController @Inject()(actionsProvider: ActionsProvider,
                                    formsProvider: FormsProvider,
                                    pageView: LabourPayView,
                                    labourPayService: LabourPayService,
                                    errorHandler: ErrorHandler)
                                   (implicit cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int,
           month: String,
           contractor: String): Action[AnyContent] = actionsProvider.endOfYearWithSessionData(taxYear, month, contractor) { implicit request =>
    val form = formsProvider.labourPayAmountForm(request.user.isAgent)
    Ok(pageView(LabourPayPage(Month.valueOf(month.toUpperCase), request.cisUserData, form)))
  }

  def submit(taxYear: Int,
             month: String,
             contractor: String): Action[AnyContent] = actionsProvider.endOfYearWithSessionData(taxYear, month, contractor).async { implicit request =>
    formsProvider.labourPayAmountForm(request.user.isAgent).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(pageView(LabourPayPage(Month.valueOf(month.toUpperCase), request.cisUserData, formWithErrors)))),
      amount => labourPayService.saveLabourPay(request.user, request.cisUserData, amount).map {
        case Left(_) => errorHandler.internalServerError()
        case Right(cisUserData) => Redirect(getRedirectCall(taxYear, month, contractor, cisUserData))
      }
    )
  }

  private def getRedirectCall(taxYear: Int, month: String, contractor: String, cisUserData: CisUserData) = {
    if (cisUserData.isFinished) ContractorCYAController.show(taxYear, month, contractor) else DeductionAmountController.show(taxYear, month, contractor)
  }
}
