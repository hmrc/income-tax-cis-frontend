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

import actions.{ActionsProvider, AuthorisedAction}
import com.google.inject.Inject
import config.{AppConfig, ErrorHandler}
import forms.ContractorDetailsForm.contractorDetailsForm
import models.pages.ContractorDetailsViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ContractorDetailsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{InYearUtil, SessionHelper}
import views.html.ContractorDetailsView

import java.nio.charset.StandardCharsets.UTF_8
import java.net.{URLDecoder, URLEncoder}
import scala.concurrent.{ExecutionContext, Future}

class ContractorDetailsController @Inject()(
                                           implicit val cc: MessagesControllerComponents,
                                           actionsProvider: ActionsProvider,
                                           contractorDetailsView: ContractorDetailsView,
                                           contractorDetailsService: ContractorDetailsService,
                                           errorHandler: ErrorHandler,
                                           implicit val ec: ExecutionContext,
                                           implicit val appConfig: AppConfig
                                           ) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, contractor: Option[String]): Action[AnyContent] = actionsProvider.notInYear(taxYear).async { implicit request =>
      val form: Form[ContractorDetailsViewModel] = contractorDetailsForm(request.user.isAgent)
      contractor match {
        case Some(contractorValue) => {
          val decodedErn = URLDecoder.decode(contractorValue, UTF_8.toString)
          contractorDetailsService.checkAccessContractorDetailsPage(taxYear,request.user, decodedErn).map {
            case Right(Some(cisUserData)) =>
              val fillModel = ContractorDetailsViewModel(cisUserData.cis.contractorName.getOrElse(""), cisUserData.employerRef)
              Ok(contractorDetailsView(taxYear, form.fill(fillModel), Some(cisUserData.employerRef)))
            case Right(None) => Ok(contractorDetailsView(taxYear, form))
            case Left(value) => errorHandler.internalServerError()
          }
        }
        case None => Future(Ok(contractorDetailsView(taxYear, form)))
      }
  }

  def submit(taxYear: Int, contractor: Option[String]): Action[AnyContent] = actionsProvider.notInYear(taxYear).async { implicit request =>
      val form: Form[ContractorDetailsViewModel] = contractorDetailsForm(request.user.isAgent)
      form.bindFromRequest().fold(
        formWithErrors => Future(BadRequest(contractorDetailsView(taxYear, formWithErrors, contractor))),
        submittedForm =>
          contractorDetailsService.createOrUpdateContractorDetails(submittedForm, taxYear, request.user, contractor).flatMap {
            case Left(error) => Future(errorHandler.internalServerError())
            case Right(value) => val ern = URLEncoder.encode(submittedForm.employerReferenceNumber, UTF_8.toString)
              Future(Redirect(controllers.routes.DeductionPeriodController.show(taxYear, ern)))
          }
      )
  }

}
