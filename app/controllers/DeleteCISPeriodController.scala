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
import controllers.routes.ContractorSummaryController
import models.HttpParserError
import models.pages.DeleteCISPeriodPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.DeleteCISPeriodService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.DeleteCISPeriodView

import java.time.Month
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DeleteCISPeriodController @Inject()(actionsProvider: ActionsProvider,
                                          view: DeleteCISPeriodView,
                                          errorHandler: ErrorHandler,
                                          deleteCISPeriodService: DeleteCISPeriodService)
                                         (implicit val mcc: MessagesControllerComponents, ec: ExecutionContext, appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int,
           month: String,
           contractor: String): Action[AnyContent] = actionsProvider.exclusivelyCustomerPriorDataForEOY(taxYear, contractor, month) { implicit request =>
    Ok(view(DeleteCISPeriodPage(taxYear, contractor, Month.valueOf(month.toUpperCase))))
  }

  def submit(taxYear: Int,
             contractor: String,
             month: String): Action[AnyContent] = actionsProvider.exclusivelyCustomerPriorDataForEOY(taxYear, contractor, month).async { implicit request =>
    deleteCISPeriodService.removeCisDeduction(taxYear, contractor, request.user, Month.valueOf(month.toUpperCase), request.incomeTaxUserData).map {
      case Left(HttpParserError(status)) => errorHandler.handleError(status)
      case Left(_) => errorHandler.internalServerError()
      case Right(_) => Redirect(ContractorSummaryController.show(taxYear, contractor))
    }
  }
}
