
# income-tax-cis-frontend

This is where users can review and make changes to the CIS section of their income tax return.

## Running the service locally

you will need to have the following:
- Installed [MondoDB](https://docs.mongodb.com/manual/installation/)
- Installed/configured [service-manager](https://github.com/hmrc/service-manager)

The service manager profile for this service is:

    sm2 --start INCOME_TAX_CIS_FRONTEND
Run the following command to start the remaining services locally:

    sudo mongod (If not already running)
    sm2 --start INCOME_TAX_SUBMISSION_ALL -r

To run the service locally:

    sudo mongod (If not already running)
    sm2 --start INCOME_TAX_SUBMISSION_ALL -r
    sm2 --stop INCOME_TAX_CIS_FRONTEND
    ./run.sh **OR** sbt -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes run

This service runs on port: `localhost:9338`

To test the branch you're working on locally. You will need to run `sm2 --stop INCOME_TAX_CIS_FRONTEND` followed by
`./run.sh`

### Running Tests

- Run Unit Tests:  `sbt test`
- Run Integration Tests: `sbt it/test`
- Run Unit and Integration Tests: `sbt test it/test`
- Run Unit and Integration Tests with coverage report: `./check.sh`<br/>
  which runs `sbt clean coverage test it/test coverageReport dependencyUpdates`


### Feature Switches

| Feature      | Environments Enabled In |
|--------------|-------------------------|
| Encryption   | None                    |
| Welsh Toggle | QA, Staging             |
| taxYearError | Production              |

### CIS Sources (Contractor and Customer Data)
CIS data can come from different sources: Contractor and Customer. Contractor data is CIS data that HMRC have for the user within the tax year, prior to any updates made by the user. The CIS data displayed in-year is Contractor data.

Customer data is provided by the user. At the end of the tax year, users can view any existing CIS data and make changes (create, update and delete).

Examples can be found here in the [income-tax-submission-stub](https://github.com/hmrc/income-tax-submission-stub/blob/main/app/models/CISUsers.scala)

## Ninos with stub data for CIS

### In-Year
| Nino      | CIS data                              | Source     |
|-----------|---------------------------------------|------------|
| AC150000B | CIS User with multiple CIS deductions | Contractor |
| AA123459A | CIS User with multiple CIS deductions | Contractor |

### End of Year
| Nino      | CIS data                              | Source               |
|-----------|---------------------------------------|----------------------|
| AC150000B | CIS User with multiple CIS deductions | Contractor, Customer |


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").