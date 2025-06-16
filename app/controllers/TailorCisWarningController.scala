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

import actions.{ActionsProvider, AuthorisedAction}
import config.{AppConfig, ErrorHandler}
import models.IncomeTaxUserData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.TailoringService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import controllers.routes.DeductionsSummaryController
import views.html.TailorCisWarningView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TailorCisWarningController @Inject()(cc: MessagesControllerComponents,
                                           authAction: AuthorisedAction,
                                           inYearAction: InYearUtil,
                                           pageView: TailorCisWarningView,
                                           tailoringService: TailoringService,
                                           actionsProvider: ActionsProvider,
                                           errorHandler: ErrorHandler)
                                          (implicit val appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    if (!inYearAction.inYear(taxYear)) {
      Future.successful(Ok(pageView(taxYear)))
    } else {
      Future.successful(Redirect(DeductionsSummaryController.show(taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.priorCisDeductionsData(taxYear).async { implicit request =>
    if (!inYearAction.inYear(taxYear)) {
      request.incomeTaxUserData match {
        case IncomeTaxUserData(None) =>
          tailoringService.postExcludedJourney(taxYear, request.user.nino, request.user.mtditid).map {
            case Left(error) => errorHandler.handleError(error.status)
            case Right(_) => Redirect(DeductionsSummaryController.show(taxYear))
          }
        case IncomeTaxUserData(Some(_)) =>
          tailoringService.removeCISData(taxYear, request.user, request.incomeTaxUserData).flatMap {
            case Left(error) => Future.successful(errorHandler.internalServerError())
            case Right(_) =>
              tailoringService.postExcludedJourney(taxYear, request.user.nino, request.user.mtditid).map {
                case Left(error) => errorHandler.handleError(error.status)
                case Right(_) => Redirect(DeductionsSummaryController.show(taxYear))
              }
          }
      }
    } else {
      Future.successful(Redirect(DeductionsSummaryController.show(taxYear)))
    }
  }
}
