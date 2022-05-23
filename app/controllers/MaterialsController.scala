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

package controllers

import actions.ActionsProvider
import config.{AppConfig, ErrorHandler}
import controllers.routes.{ContractorCYAController, MaterialsAmountController}
import forms.FormsProvider
import models.pages.MaterialsPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.MaterialsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.MaterialsView

import java.time.Month
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MaterialsController @Inject()(actionsProvider: ActionsProvider,
                                    formsProvider: FormsProvider,
                                    pageView: MaterialsView,
                                    materialsService: MaterialsService,
                                    errorHandler: ErrorHandler)
                                   (implicit cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int,
           month: String,
           contractor: String): Action[AnyContent] = actionsProvider.endOfYearWithSessionData(taxYear, month, contractor) { implicit request =>
    Ok(pageView(MaterialsPage(Month.valueOf(month.toUpperCase), request.cisUserData, formsProvider.materialsYesNoForm(request.user.isAgent))))
  }

  def submit(taxYear: Int,
             month: String,
             contractor: String): Action[AnyContent] = actionsProvider.endOfYearWithSessionData(taxYear, month, contractor).async { implicit request =>
    formsProvider.materialsYesNoForm(request.user.isAgent).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(pageView(MaterialsPage(Month.valueOf(month.toUpperCase), request.cisUserData, formWithErrors)))),
      yesNoValue => materialsService.saveQuestion(request.user, request.cisUserData, yesNoValue).map {
        case Left(_) => errorHandler.internalServerError()
        case Right(_) => if (yesNoValue) {
          Redirect(MaterialsAmountController.show(taxYear, month.toLowerCase, contractor))
        } else {
          Redirect(ContractorCYAController.show(taxYear,month.toLowerCase,contractor))
        }
      }
    )
  }
}
