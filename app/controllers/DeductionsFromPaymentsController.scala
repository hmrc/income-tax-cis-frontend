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
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.DeductionsFromPaymentsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeductionsFromPaymentsController @Inject()(actionsProvider: ActionsProvider,
                                                 formsProvider: FormsProvider,
                                                 pageView: DeductionsFromPaymentsView,
                                                 errorHandler: ErrorHandler)
                                                (implicit cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.tailoringEnabledFilter(taxYear) { implicit request =>
    Ok(pageView(DeductionsFromPaymentsPage(taxYear, formsProvider.deductionsFromPaymentsForm(request.user.isAgent))))
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.tailoringEnabledFilter(taxYear).async { implicit request =>
    // TODO: change the redirects once the CYA data has been refactored
    // the redirects should work as follows:
    //  1. If user answers 'Yes' and is coming from 'Change' Link and CYA model is complete then redirect to CYA controller.
    //  2. If user answers 'Yes' and is not coming from 'Change' Link and CYA model is not complete then redirect to deductions summary controller.
    //  3. If user answers 'No' then redirect to CYA controller with a fully zeroed out model.
    formsProvider.deductionsFromPaymentsForm(request.user.isAgent).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(pageView(DeductionsFromPaymentsPage(taxYear, formWithErrors)))),
      yesNoAnswer =>
        if (yesNoAnswer) {
          Future(Redirect(controllers.routes.DeductionsSummaryController.show(taxYear)))
        } else {
          Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        }
    )
  }
}
