package com.affinytix.config.tracking.scapping

import com.affinytix.config.common.{PK, ProfileJdbc}

/**
  * Created by sam on 7/4/17.
  */
trait TrackingScrappingTables {
  this: ProfileJdbc =>

  import profile.api._

  final case class EventType(delay: Int,
                             delayable: Byte,
                             name: String,
                             custom: Byte,
                             id: PK[EventTypeTable] = PK(0))

  final class EventTypeTable(tag: Tag) extends
    Table[EventType](tag, "EVENT_TYPE") {

    def eventTypeId =
      column[PK[EventTypeTable]]("eventTypeId", O.AutoInc, O.PrimaryKey)

    def delay = column[Int]("delay")

    def delayable = column[Byte]("delayable")

    def name = column[String]("name")

    def custom = column[Byte]("custom")

    def * = (delay, delayable, name, custom, eventTypeId).mapTo[EventType]
  }

  lazy val eventTypes = TableQuery[EventTypeTable]
  lazy val insertEventTypes = eventTypes returning eventTypes.map(_.eventTypeId)


  final case class PageType(name: String,
                            id: PK[PageTypeTable] = PK(0))

  final class PageTypeTable(tag: Tag) extends
    Table[PageType](tag, "PAGE_TYPE") {

    def pageTypeId =
      column[PK[PageTypeTable]]("pageTypeId", O.AutoInc, O.PrimaryKey)

    def name = column[String]("name")

    def * = (name, pageTypeId).mapTo[PageType]
  }

  lazy val pageTypes = TableQuery[PageTypeTable]
  lazy val insertPageTypes = pageTypes returning pageTypes.map(_.pageTypeId)


  final case class UserAction(name: Option[String],
                              label: String,
                              minDuration: Option[Long],
                              cancelable: Byte = 0,
                              cancelActionId: Option[PK[UserActionTable]],
                              onBusinessAction: Byte,
                              onSeveralPages: Byte,
                              minRepeatableDelay: Option[Long],
                              id: PK[UserActionTable] = PK(0)
                             )

  final class UserActionTable(tag: Tag) extends
    Table[UserAction](tag, "USER_ACTION") {

    def userActionId =
      column[PK[UserActionTable]]("userActionId", O.AutoInc, O.PrimaryKey)

    def name = column[Option[String]]("name")

    def label = column[String]("label")

    def minDuration = column[Option[Long]]("minDuration")

    def cancelable = column[Byte]("cancelable")

    def cancelActionId = column[Option[PK[UserActionTable]]]("cancelActionId")

    def onBusinessAction = column[Byte]("onBusinessAction")

    def onSeveralPages = column[Byte]("onSeveralPages")

    def minRepeatableDelay = column[Option[Long]]("minRepeatableDelay")

    def cancelAction = foreignKey("cancelActionId_userActionId_fk", cancelActionId, userActions)(
      _.userActionId, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    def * = (name, label, minDuration, cancelable, cancelActionId,
      onBusinessAction, onSeveralPages, minRepeatableDelay, userActionId).mapTo[UserAction]
  }

  lazy val userActions = TableQuery[UserActionTable]
  lazy val insertUserActions = userActions returning userActions.map(_.userActionId)

  final case class TechnicalEvent(name: String,
                                  eventTypeId: PK[EventTypeTable] = PK(0),
                                  pageTypeId: PK[PageTypeTable] = PK(0),
                                  userActionId: Option[PK[UserActionTable]] = Some(PK(0)),
                                  dependentId: Option[PK[TechnicalEventTable]] = Some(PK(0)),
                                  id: PK[TechnicalEventTable] = PK(0))


  final class TechnicalEventTable(tag: Tag) extends
    Table[TechnicalEvent](tag, "TECHNICAL_EVENT") {

    def technicalEventId =
      column[PK[TechnicalEventTable]]("technicalEventId", O.AutoInc, O.PrimaryKey)

    def name = column[String]("name")

    def eventTypeId = column[PK[EventTypeTable]]("eventTypeId")

    def pageTypeId = column[PK[PageTypeTable]]("pageTypeId")

    def userActionId = column[Option[PK[UserActionTable]]]("userActionId")

    def dependentId = column[Option[PK[TechnicalEventTable]]]("dependentId")

    def eventType = foreignKey("technicalEvent_eventTypeId_eventType_eventTypeId_fk",
      eventTypeId, eventTypes)(
      _.eventTypeId, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction
    )

    def pageType = foreignKey("technicalEvent_pageTypeId_pageType_pageTypeId_fk",
      pageTypeId, pageTypes)(
      _.pageTypeId, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction
    )

    def userAction = foreignKey("technicalEvent_userActionId_userAction_userActionId_fk",
      userActionId, userActions)(
      _.userActionId, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    def dependent = foreignKey("technicalEvent_technicalEventId_technicalEvent_technicalEventId_fk",
      dependentId, technicalEvents)(_.technicalEventId,
      onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    def * = (name, eventTypeId, pageTypeId, userActionId, dependentId, technicalEventId)
      .mapTo[TechnicalEvent]
  }


  lazy val technicalEvents = TableQuery[TechnicalEventTable]
  lazy val insertTechnicalEvents =
    technicalEvents returning technicalEvents.map(_.technicalEventId)

  lazy val ddl =
    eventTypes.schema ++
      pageTypes.schema ++
      userActions.schema ++
      technicalEvents.schema

  def drop = ddl.drop.asTry

  def create = ddl.create.asTry

}
