package com.github.ojacquemart.realtime.db

import com.mongodb.BasicDBObject
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.vertx.ConsumeEvent
import io.vertx.core.eventbus.EventBus
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.untilNotNull
import org.bson.Document
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.LinkedBlockingDeque
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@QuarkusTest
@QuarkusTestResource(MongoTestLifeCycleManager::class)
internal class GenericMongoCollectionsTest {

  val databaseName = "realtime"
  val collectionName = "coll-foobarqix"

  @Inject
  lateinit var mongoClient: MongoClient

  @Inject
  lateinit var genericMongoCollections: GenericMongoCollections

  @BeforeEach
  fun setUp() {
    val collection = getCollection()
    collection.deleteMany(BasicDBObject())
    genericMongoCollections.persist(
      MongoOperation(
        db = databaseName, collection = collectionName, type = "CREATE",
        data = mapOf("_id" to "foo", "name" to "bar")
      )
    )
  }

  @Test
  fun `should persist a document`() {
    genericMongoCollections.persist(
      MongoOperation(
        type = "CREATE",
        db = databaseName, collection = collectionName,
        data = mapOf(
          "_id" to "foo-bar-qix",
          "name" to "foobarqix"
        ),
      )
    )

    val collection = getCollection()
    val document = collection.find(BasicDBObject(mapOf("_id" to "foo-bar-qix"))).first()

    Assertions.assertNotNull(document)
    Assertions.assertEquals("foo-bar-qix", document?.getString("_id"))
    Assertions.assertEquals("foobarqix", document?.getString("name"))
  }

  @Test
  fun `should receive a message on a duplicate document error`() {
    genericMongoCollections.persist(
      MongoOperation(
        type = "CREATE",
        db = databaseName, collection = collectionName,
        data = mapOf(
          "_id" to "foo",
          "name" to "foobarqix"
        ),
      )
    )

    val error = await().untilNotNull { SimpleClient.MESSAGES.poll() }
    Assertions.assertEquals("ERROR", error.type)
    Assertions.assertEquals(
      mapOf(
        "reason" to "DUPLICATE",
        "source" to mapOf(
          "_id" to "foo",
          "name" to "foobarqix"
        ),
      ), error.data
    )
  }

  @Test
  fun `should read all documents from a given collection`() {
    genericMongoCollections.persist(
      MongoOperation(
        type = "CREATE",
        db = databaseName, collection = collectionName,
        data = mapOf("name" to "foobarqix")
      )
    )
    val lastItemCreated = getCollection()
      .find(BasicDBObject(mapOf("name" to "foobarqix")))
      .first()
    val lastId = lastItemCreated?.getObjectId("_id")?.toString()

    genericMongoCollections.read(
      MongoOperation(
        type = "READ",
        db = databaseName, collection = collectionName,
        data = mapOf("_id" to "_all")
      )
    )

    val allItemsOperation = await().untilNotNull { SimpleClient.MESSAGES.poll() }
    Assertions.assertEquals("READ", allItemsOperation.type)
    Assertions.assertEquals(
      mapOf(
        "_id" to "_all",
        "items" to listOf(
          mapOf("_id" to "foo", "name" to "bar"),
          mapOf("_id" to lastId, "name" to "foobarqix"),
        )
      ), allItemsOperation.data
    )
  }

  @Test
  fun `should read a single document from a given collection`() {
    genericMongoCollections.read(
      MongoOperation(
        type = "READ",
        db = databaseName, collection = collectionName,
        data = mapOf("_id" to "foo")
      )
    )

    val foo = await().untilNotNull { SimpleClient.MESSAGES.poll() }
    Assertions.assertEquals("READ", foo.type)
    Assertions.assertEquals(
      mapOf(
        "_id" to "foo",
        "name" to "bar"
      ), foo.data
    )
  }

  @Test
  fun `should update an existing document`() {
    genericMongoCollections.persistOrUpdate(
      MongoOperation(
        type = "UPDATE",
        db = databaseName, collection = collectionName,
        data = mapOf(
          "_id" to "foo", "name" to "foobar"
        )
      )
    )

    val document = getCollection()
      .find(BasicDBObject(mapOf("_id" to "foo")))
      .first()
    Assertions.assertEquals("foobar", document?.get("name"))
  }

  @Test
  fun `should delete a document by its id`() {
    genericMongoCollections.delete(
      MongoOperation(
        type = "DELETE",
        db = databaseName, collection = collectionName, id = "foo",
      )
    )

    val collection = getCollection()
    Assertions.assertEquals(0, collection.countDocuments())
  }

  private fun getCollection(): MongoCollection<Document> {
    return mongoClient
      .getDatabase(databaseName)
      .getCollection(collectionName)
  }

  @ApplicationScoped
  class SimpleClient {

    @Inject
    lateinit var eventBus: EventBus

    @ConsumeEvent("db-data")
    fun onDataMessage(data: MongoOperation) {
      MESSAGES.add(data)
    }

    companion object {
      val MESSAGES = LinkedBlockingDeque<MongoOperation>()
    }

  }

}
