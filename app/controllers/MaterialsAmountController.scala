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
import forms.FormsProvider
import models.UserSessionDataRequest
import models.pages.MaterialsAmountPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.MaterialsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.MaterialsAmountView

import java.time.Month
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MaterialsAmountController @Inject()(actionsProvider: ActionsProvider,
                                          formsProvider: FormsProvider,
                                          pageView: MaterialsAmountView,
                                          materialsService: MaterialsService,
                                          errorHandler: ErrorHandler)
                                         (implicit mcc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int,
           month: String,
           contractor: String): Action[AnyContent] = actionsProvider.endOfYearWithSessionData(taxYear, month, contractor) { implicit request =>
    if (!hasCostOfMaterials(request)) {
      Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    } else {
      val form = formsProvider.materialsAmountForm(request.user.isAgent)
      Ok(pageView(MaterialsAmountPage(Month.valueOf(month.toUpperCase), request.cisUserData, form)))
    }
  }

  def submit(taxYear: Int,
             month: String,
             contractor: String): Action[AnyContent] = actionsProvider.endOfYearWithSessionData(taxYear, month, contractor).async { implicit request =>
    if (!hasCostOfMaterials(request)) {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    } else {
      formsProvider.materialsAmountForm(request.user.isAgent).bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(pageView(MaterialsAmountPage(Month.valueOf(month.toUpperCase), request.cisUserData, formWithErrors)))),
        amount => materialsService.saveAmount(request.user, request.cisUserData, amount).map {
          case Left(_) => errorHandler.internalServerError()
          // TODO: Redirect to next page
          case Right(_) => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        }
      )
    }
  }

  // TODO: The following should be done in an action
  private def hasCostOfMaterials(request: UserSessionDataRequest[AnyContent]) = {
    request.cisUserData.cis.periodData
      .flatMap(_.costOfMaterialsQuestion)
      .contains(true)
  }
}
