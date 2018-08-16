
package filters

import com.google.inject.Inject
import play.api.http.DefaultHttpFilters
import uk.gov.hmrc.play.bootstrap.filters.FrontendFilters

class FiltersWithWhitelist @Inject()(
                                      whitelistFilter: WhitelistFilter,
                                      sessionIdFilter: SessionIdFilter,
                                      frontendFilters: FrontendFilters
                                    ) extends DefaultHttpFilters(frontendFilters.filters :+ sessionIdFilter :+ whitelistFilter: _*)
