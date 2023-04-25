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
import forms.FormsProvider
import models.pages.DeductionsFromPaymentsPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.TailoringService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.DeductionsFromPaymentsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeductionsFromPaymentsController @Inject()(actionsProvider: ActionsProvider,
                                                 formsProvider: FormsProvider,
                                                 pageView: DeductionsFromPaymentsView,
                                                 tailoringService: TailoringService,
                                                 errorHandler: ErrorHandler)
                                                (implicit cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.tailoringEnabledFilter(taxYear).async { implicit request =>
    tailoringService.getExcludedJourneys(taxYear, request.user.mtditid, request.user.mtditid).map {
      case Left(error) => errorHandler.handleError(error.status)
      case Right(result) =>
        Ok(pageView(DeductionsFromPaymentsPage(taxYear,
          formsProvider.deductionsFromPaymentsForm(request.user.isAgent).fill(!result.journeys.map(_.journey).contains("cis")))))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.tailoringEnabledFilter(taxYear).async { implicit request =>
    formsProvider.deductionsFromPaymentsForm(request.user.isAgent).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(pageView(DeductionsFromPaymentsPage(taxYear, formWithErrors)))),
      yesNoAnswer =>
        if (yesNoAnswer) {
          tailoringService.clearExcludedJourney(taxYear, request.user.nino, request.user.mtditid).map {
            case Left(_) => errorHandler.internalServerError()
            case Right(_) => Redirect(controllers.routes.DeductionsSummaryController.show(taxYear))
          }
        } else {
          tailoringService.getExcludedJourneys(taxYear, request.user.mtditid, request.user.mtditid).map {
            case Left(error) => errorHandler.handleError(error.status)
            case Right(result) =>
              if (result.journeys.map(_.journey).contains("cis")) {
                Redirect(controllers.routes.DeductionsSummaryController.show(taxYear))
              }
              else {
                Redirect(controllers.routes.TailorCisWarningController.show(taxYear))
              }
          }
        }
    )
  }
}
