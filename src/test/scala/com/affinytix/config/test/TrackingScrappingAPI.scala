package com.affinytix.config.test

import com.affinytix.config.common.PK
import com.affinytix.config.test.TrackingScrappingAPI.QUAId
import org.slf4j.LoggerFactory

/**
  * Created by sam on 6/29/17.
  */
object TrackingScrappingAPI {

  private final val logger = LoggerFactory.getLogger(this.getClass)

  import com.affinytix.config._
  import slick.dbio.DBIO

  import scala.concurrent.Await
  import scala.concurrent.duration._

  val schema = new TrackingScrappingSchema(slick.jdbc.H2Profile)

  import schema._
  import schema.profile.api._

  val db: Database = Database.forConfig("configtest")

  def exec[T](action: DBIO[T]): T = Await.result(db.run(action), 2 seconds)

  def create = exec(schema.create)

  def drop = exec(schema.drop)


  def insertEventTypes(eventTypes: Seq[EventType]) =
    exec(schema.insertEventTypes ++= eventTypes)

  def countEventTypes = exec(eventTypes.length.result)

  def selectAllEventTypes = exec(eventTypes.result)

  def insertPageTypes(pageTypes: Seq[PageType]) =
    exec(schema.insertPageTypes ++= pageTypes)

  def countPageTypes = exec(pageTypes.length.result)

  def selectAllPageTypes = exec(pageTypes.result)

  def insertUserActions(userActions: Seq[UserAction]) =
    exec(schema.insertUserActions ++= userActions)

  def insertUserActionsWithCancel(userActionsAndParentLabel: Seq[(UserAction, Option[String])]) = {


    val noParent = userActionsAndParentLabel.filter(_._2.isEmpty)
    val actionNoParent = schema.insertUserActions ++= noParent.map(_._1)

    val withParent = userActionsAndParentLabel.filter(_._2.isDefined)

    val seqI = withParent.map {
      t =>
        val uac = t._1
        val idParent = userActions.filter(_.label === t._2).map(_.userActionId)

        val uat = for {
          id <- idParent
        } yield (uac.name, uac.label, uac.minDuration, uac.cancelable, Rep.Some(id),
          uac.onBusinessAction, uac.onSeveralPages, uac.minRepeatableDelay, uac.id)

        userActions.map { ua =>
          (ua.name, ua.label, ua.minDuration, ua.cancelable, ua.cancelActionId,
            ua.onBusinessAction, ua.onSeveralPages, ua.minRepeatableDelay, ua.userActionId)
        }.forceInsertQuery(uat)
    }

    exec(actionNoParent >> DBIO.sequence(seqI))
  }

  def countUserActions = exec(userActions.length.result)

  def selectAllUserActions = exec(userActions.result)

  import DataGenerator._

  def insertTechnicalEvent(techEvtsRaw: Seq[TechEvtAndDepRaw]) = {
    val res = techEvtsRaw map {
      t =>
        val evtTypeIdQ = selectEventTypeId(t._1._2)
        val pageTypeIdQ = selectPageTypeId(t._1._3)
        val userActionIdQ = t._1._4.map(selectUserActionId(_)).
          getOrElse(Query(Rep.None[PK[UserActionTable]]))
        val techEvtIdQ = t._2.map(selectTechnicalEventId(_)).
          getOrElse(Query(Rep.None[PK[TechnicalEventTable]]))

        val toIns = for {
          evtTypeId <- evtTypeIdQ
          pageTypeId <- pageTypeIdQ
          userActionId <- userActionIdQ
          techEvtId <- techEvtIdQ
        } yield (t._1._1, evtTypeId, pageTypeId, userActionId, techEvtId)

        technicalEvents.map(
          i => (i.name, i.eventTypeId, i.pageTypeId, i.userActionId, i.dependentId)
        ).forceInsertQuery(toIns) // compiling + RuntimeError
      //        ).forceInsertQuery(Query( // compiling + inserting
      //          "toto",
      //          PK[EventTypeTable](1),
      //          PK[PageTypeTable](1),
      //          Rep.None[PK[UserActionTable]],
      //          Rep.None[PK[TechnicalEventTable]]))
    }

    exec(DBIO.sequence(res))
  }

  def countTechnicalEvents = exec(technicalEvents.length.result)

  def selectAllTechnicalEvents = exec(technicalEvents.result)

  def selectUserActionId(label: String) = userActions.
    filter(_.label.toUpperCase === label.toUpperCase)
    .map(x => Rep.Some(x.userActionId))

  def selectEventTypeId(name: String) = eventTypes.
    filter(_.name.toUpperCase === name.toUpperCase)
    .map(_.eventTypeId)

  def selectPageTypeId(name: String) = pageTypes.
    filter(_.name.toUpperCase === name.toUpperCase)
    .map(_.pageTypeId)

  def selectTechnicalEventId(techEvtRaw: TechEvtRaw) = {
    val baseQ =
      technicalEvents join
        eventTypes on (_.eventTypeId === _.eventTypeId) join
        pageTypes on (_._1.pageTypeId === _.pageTypeId) filter {
        t =>
          t._1._1.name === techEvtRaw._1 &&
            t._1._2.name === techEvtRaw._2 &&
            t._2.name === techEvtRaw._3
      }

    techEvtRaw._4.map {
      ual =>
        baseQ.join(userActions).
          on(_._1._1.userActionId === _.userActionId).
          filter(x => x._2.label === ual).
          map {
            t => Rep.Some(t._1._1._1.technicalEventId)
          }
    }.getOrElse(
      baseQ.map(t => Rep.Some(t._1._1.technicalEventId))
    )

  }

  def populateEventType = Seq(
    EventType(0, 0, "click", 0),
    EventType(0, 0, "mouseover", 0),
    EventType(0, 0, "mouseout", 0),
    EventType(0, 0, "Exitsite", 1),
    EventType(0, 0, "load", 0),
    EventType(0, 0, "beforeunload", 0),
    EventType(0, 0, "startuseractivity", 1),
    EventType(0, 0, "stopuseractivity", 1),
    EventType(0, 0, "blur", 0),
    EventType(0, 0, "focus", 0)
  )

  def populatePageType = Seq(
    PageType("any"),
    PageType("offer page"),
    PageType("category page"),
    PageType("cart page"),
    PageType("home page"),
    PageType("command validation step page"),
    PageType("search page"),
    PageType("thematique"),
    PageType("authentification page"),
    PageType("account creation page"),
    PageType("account page"),
    PageType("history commands page"),
    PageType("command account step page"),
    PageType("command delivery step page"),
    PageType("command payment step page"),
    PageType("universe page"),
    PageType("sub category page"),
    PageType("sub sub category page"),
    PageType("shipping option step page"),
    PageType("Pack page"),
    PageType("news page"),
    PageType("brand page"),
    PageType("sales page")
  )

  def populateUserAction = Seq(
    (UserAction(None, """click any element""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_product_hoverImage"""), """offer img overfly""", Some(1000), 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_product_addInBasket"""), """add to cart""", None, 1, None, 0, 0, Some(1000)), Some("""remove from cart""")),
    (UserAction(Some("""user_action_website_exitAttempt"""), """attempt exit site""", None, 0, None, 0, 0, Some(500)), None),
    (UserAction(None, """offer interest""", None, 0, None, 0, 0, None), None),
    (UserAction(None, """remove from cart""", None, 0, None, 0, 0, Some(1000)), None),
    (UserAction(None, """wepingo close popup""", None, 0, None, 1, 0, Some(1000)), None),
    (UserAction(None, """wepingo validate popup""", None, 0, None, 1, 0, Some(1000)), None),
    (UserAction(None, """wepingo popup overfly""", Some(200), 0, None, 1, 0, None), None),
    (UserAction(Some("""user_action_product_accessPage"""), """access offer page""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_view_cart"""), """access cart page""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_activeOnPage"""), """user is active on page""", Some(100), 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_category_accessPage"""), """access category page""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_home_accessPage"""), """access home page""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_product_redisplayPage"""), """redisplay offer page""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_command_validation"""), """command validation""", None, 0, None, 0, 0, Some(1000)), None),
    (UserAction(Some("""user_action_send_question"""), """wepingo send question""", None, 0, None, 1, 0, Some(1000)), None),
    (UserAction(Some("""user_action_any_accessPage"""), """access any page""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_search_accessPage"""), """access search page""", None, 0, None, 0, 0, None), None),
    (UserAction(None, """wepingo insert zone click""", None, 0, None, 1, 0, Some(1000)), None),
    (UserAction(None, """wepingo insert zone overfly""", Some(200), 0, None, 1, 0, None), None),
    (UserAction(None, """wepingo insert zone click on link""", None, 0, None, 1, 0, Some(1000)), None),
    (UserAction(Some("""user_action_thematique_accessPage"""), """access thematique page""", None, 0, None, 0, 0, None), None),
    (UserAction(None, """choose location""", None, 0, None, 0, 0, Some(2000)), None),
    (UserAction(None, """ask location""", None, 0, None, 0, 0, Some(1000)), None),
    (UserAction(None, """logo return home""", None, 0, None, 0, 0, Some(1000)), None),
    (UserAction(None, """cart validation""", None, 0, None, 0, 0, Some(1000)), None),
    (UserAction(None, """continue shopping""", None, 0, None, 0, 0, Some(1000)), None),
    (UserAction(None, """click discount""", None, 0, None, 0, 0, Some(1000)), None),
    (UserAction(None, """offer overfly""", Some(1000), 0, None, 0, 0, None), None),
    (UserAction(None, """go group site""", None, 0, None, 0, 0, Some(1000)), None),
    (UserAction(None, """ask info services""", None, 0, None, 0, 0, Some(1000)), None),
    (UserAction(None, """ask info commitments""", None, 0, None, 0, 0, Some(1000)), None),
    (UserAction(None, """ask info assistance""", None, 0, None, 0, 0, Some(1000)), None),
    (UserAction(None, """ask info payment""", None, 0, None, 0, 0, Some(1000)), None),
    (UserAction(None, """ask info delivery mode""", None, 0, None, 0, 0, Some(1000)), None),
    (UserAction(None, """ask contact""", None, 0, None, 0, 0, Some(1000)), None),
    (UserAction(None, """use breadcrumb""", None, 0, None, 0, 0, Some(1000)), None),
    (UserAction(None, """consult category menu""", Some(1000), 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_compare_button_click"""), """wepingo compare button click""", None, 0, None, 1, 0, Some(100)), None),
    (UserAction(None, """use search bar""", None, 0, None, 0, 0, None), None),
    (UserAction(None, """access authentification page""", None, 0, None, 0, 0, None), None),
    (UserAction(None, """authenticate""", None, 0, None, 0, 0, Some(300)), None),
    (UserAction(None, """want create account""", None, 0, None, 0, 0, Some(300)), None),
    (UserAction(None, """access account creation page""", None, 0, None, 0, 0, None), None),
    (UserAction(None, """valid account creation""", None, 0, None, 0, 0, Some(300)), None),
    (UserAction(None, """access account page""", None, 0, None, 0, 0, None), None),
    (UserAction(None, """edit acccount""", None, 0, None, 0, 0, Some(300)), None),
    (UserAction(None, """access history commands page""", None, 0, None, 0, 0, None), None),
    (UserAction(None, """access command account step page""", None, 0, None, 0, 0, None), None),
    (UserAction(None, """access command delivery step page""", None, 0, None, 0, 0, None), None),
    (UserAction(None, """modify product quantity""", None, 0, None, 0, 0, Some(300)), None),
    (UserAction(None, """modify command account""", None, 0, None, 0, 0, Some(300)), None),
    (UserAction(None, """modify command delivery""", None, 0, None, 0, 0, Some(300)), None),
    (UserAction(None, """valid delivery""", None, 0, None, 0, 0, Some(300)), None),
    (UserAction(None, """access command payment step page""", None, 0, None, 0, 0, None), None),
    (UserAction(None, """pay command""", None, 0, None, 0, 0, Some(300)), None),
    (UserAction(None, """access command validation step page""", None, 0, None, 0, 0, None), None),
    (UserAction(None, """return command cart step page""", None, 0, None, 0, 0, Some(300)), None),
    (UserAction(None, """return command account step page""", None, 0, None, 0, 0, Some(300)), None),
    (UserAction(None, """return command delivery step page""", None, 0, None, 0, 0, Some(300)), None),
    (UserAction(None, """return command payment step page""", None, 0, None, 0, 0, Some(300)), None),
    (UserAction(None, """use input account form""", None, 0, None, 0, 0, None), None),
    (UserAction(None, """deconnexion""", None, 0, None, 0, 0, Some(300)), None),
    (UserAction(None, """change captcha""", None, 0, None, 0, 0, Some(100)), None),
    (UserAction(None, """forgot password""", None, 0, None, 0, 0, Some(300)), None),
    (UserAction(None, """modify password""", None, 0, None, 0, 0, Some(300)), None),
    (UserAction(None, """see shop site hours""", None, 0, None, 0, 0, Some(300)), None),
    (UserAction(None, """see terms of sales""", None, 0, None, 0, 0, Some(300)), None),
    (UserAction(None, """see more about payment""", None, 0, None, 0, 0, Some(300)), None),
    (UserAction(None, """change product delivery mode""", None, 0, None, 0, 0, None), None),
    (UserAction(None, """wepingo compare add""", None, 0, None, 1, 0, Some(100)), None),
    (UserAction(None, """wepingo compare remove""", None, 0, None, 1, 0, Some(100)), None),
    (UserAction(None, """wepingo compare open modal""", None, 0, None, 1, 0, Some(300)), None),
    (UserAction(None, """wepingo compare close modal""", None, 0, None, 1, 0, Some(300)), None),
    (UserAction(None, """wepingo compare open bar""", None, 0, None, 1, 0, Some(100)), None),
    (UserAction(None, """wepingo compare close bar""", None, 0, None, 1, 0, Some(100)), None),
    (UserAction(None, """wepingo compare empty list""", None, 0, None, 1, 0, Some(300)), None),
    (UserAction(None, """wepingo compare add to cart""", None, 0, None, 1, 0, Some(300)), None),
    (UserAction(None, """wepingo compare click on product""", None, 0, None, 1, 0, Some(300)), None),
    (UserAction(None, """wepingo compare change category""", None, 0, None, 1, 0, None), None),
    (UserAction(None, """wepingo compare product overfly""", Some(1000), 0, None, 1, 0, None), None),
    (UserAction(None, """wepingo compare specifications overfly""", Some(1000), 0, None, 1, 0, None), None),
    (UserAction(None, """wepingo compare use search product""", None, 0, None, 1, 0, None), None),
    (UserAction(Some("""user_action_universe_accessPage"""), """access universe page""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_sub_category_accessPage"""), """access sub category page""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_click_sorting_choice"""), """click sorting choice""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_sorting_range_price"""), """click sorting range price""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_selection_sorting"""), """click selection sorting""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_brand_sorting"""), """click brand sorting""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_complementary_product"""), """click complementary product""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_product_presentation_click"""), """click product presentation""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_technical_characteristics"""), """click product characteristics""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_product_presentation_overfly"""), """overfly product presentation""", Some(1000), 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_technical_characteristics_overfly"""), """overfly product characteristics""", Some(1000), 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_sub_sub_category_accessPage"""), """acess sub sub category page""", None, 0, None, 0, 0, None), None),
    (UserAction(None, """access shipping option step page""", None, 0, None, 0, 0, None), None),
    (UserAction(None, """add discount code""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_click_cgv_page"""), """click cgv""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_overfly_cgv"""), """overfly cgv""", Some(1000), 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_click_contact_us"""), """click contact us""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_overfly_contact_us"""), """overfly contact us""", Some(1000), 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_click_offer_search_page"""), """click offer search page""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_access_pack_page"""), """access pack page""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_access_news_page"""), """access news page""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_access_brand_page"""), """access brand page""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_access_sales_page"""), """access sales page""", None, 0, None, 0, 0, None), None),
    (UserAction(Some("""user_action_see_pack"""), """see pack""", None, 0, None, 0, 0, None), None)
  )

  import DataGenerator._

  def populateTechnicalEvent(dataFile: String): Seq[TechEvtAndDepRaw] = {
    val source = getSource(readResource(dataFile))
    loopOnSource(source)(parseTechnicalEvent(_)())
  }

  type QUAId = Query[Rep[PK[UserActionTable]], PK[UserActionTable], Seq]
  type QOUAId = Query[Rep[Option[PK[UserActionTable]]], Option[PK[UserActionTable]], Seq]
  type QETId = Query[Rep[PK[EventTypeTable]], PK[EventTypeTable], Seq]
  type QPTId = Query[Rep[PK[PageTypeTable]], PK[PageTypeTable], Seq]

}
