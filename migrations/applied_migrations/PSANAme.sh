#!/bin/bash

echo "Applying migration PSANAme"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /pSANAme                        controllers.PSANAmeController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /pSANAme                        controllers.PSANAmeController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changePSANAme                  controllers.PSANAmeController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changePSANAme                  controllers.PSANAmeController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "pSANAme.title = pSANAme" >> ../conf/messages.en
echo "pSANAme.heading = pSANAme" >> ../conf/messages.en
echo "pSANAme.checkYourAnswersLabel = pSANAme" >> ../conf/messages.en
echo "pSANAme.error.required = Enter pSANAme" >> ../conf/messages.en
echo "pSANAme.error.length = PSANAme must be 107 characters or less" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryPSANAmeUserAnswersEntry: Arbitrary[(PSANAmePage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[PSANAmePage.type]";\
    print "        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryPSANAmePage: Arbitrary[PSANAmePage.type] =";\
    print "    Arbitrary(PSANAmePage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to CacheMapGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(PSANAmePage.type, JsValue)] ::";\
    next }1' ../test/generators/CacheMapGenerator.scala > tmp && mv tmp ../test/generators/CacheMapGenerator.scala

echo "Adding helper method to CheckYourAnswersHelper"
awk '/class/ {\
     print;\
     print "";\
     print "  def pSANAme: Option[AnswerRow] = userAnswers.get(PSANAmePage) map {";\
     print "    x => AnswerRow(\"pSANAme.checkYourAnswersLabel\", s\"$x\", false, routes.PSANAmeController.onPageLoad(CheckMode).url)";\
     print "  }";\
     next }1' ../app/utils/CheckYourAnswersHelper.scala > tmp && mv tmp ../app/utils/CheckYourAnswersHelper.scala

echo "Migration PSANAme completed"
